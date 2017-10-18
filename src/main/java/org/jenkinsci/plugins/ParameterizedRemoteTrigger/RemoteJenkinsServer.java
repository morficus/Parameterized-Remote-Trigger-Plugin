package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import static org.apache.commons.lang.StringUtils.trimToEmpty;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.Auth2;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.Auth2.Auth2Descriptor;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.NoneAuth;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Holds everything regarding the remote server we wish to connect to, including validations and what not.
 * 
 * @author Maurice W.
 * 
 */
public class RemoteJenkinsServer extends AbstractDescribableImpl<RemoteJenkinsServer> {

    /**
     * We need to keep this for compatibility - old config deserialization!
     * @deprecated since 2.3.0-SNAPSHOT - use {@link Auth2} instead.
     */
    private List<Auth> auth;

    private String     displayName;
    private boolean    hasBuildTokenRootSupport;
    private Auth2      auth2;
    private URL        address;

    @DataBoundConstructor
    public RemoteJenkinsServer() {
        this.auth2 = new NoneAuth();
    }

    @DataBoundSetter
    public void setDisplayName(String displayName)
    {
        this.displayName = trimToEmpty(displayName);
    }

    @DataBoundSetter
    public void setHasBuildTokenRootSupport(boolean hasBuildTokenRootSupport)
    {
        this.hasBuildTokenRootSupport = hasBuildTokenRootSupport;
    }

    @DataBoundSetter
    public void setAuth2(Auth2 auth2)
    {
        this.auth2 = (auth2 != null) ? auth2 : new NoneAuth();
    }

    @DataBoundSetter
    public void setAddress(String address) throws MalformedURLException
    {
        this.address = new URL(address);
    }
    
    // Getters

    public String getDisplayName() {
        String displayName = null;

        if (this.displayName == null || this.displayName.trim().equals("")) {
            displayName = this.getAddress().toString();
        } else {
            displayName = this.displayName;
        }
        return displayName;
    }

    public boolean getHasBuildTokenRootSupport() {
        return hasBuildTokenRootSupport;
    }
    
    public Auth2 getAuth2() {
        migrateAuthToAuth2();
        return auth2;
    }

    /**
     * Migrates old <code>Auth</code> to <code>Auth2</code> if necessary. 
     * @deprecated since 2.3.0-SNAPSHOT - get rid once all users migrated
     */
    private void migrateAuthToAuth2() {
        if(auth2 == null) {
            if(auth == null || auth.size() <= 0) {
                auth2 = new NoneAuth(); 
            } else {
                auth2 = Auth.authToAuth2(auth);
            }
        }
        auth = null;
    }

    public URL getAddress() {
        return address;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJenkinsServer> {

        public String getDisplayName() {
            return "";
        }

        /**
         * Validates the given address to see that it's well-formed, and is reachable.
         * 
         * @param address
         *            Remote address to be validated
         * @return FormValidation object
         */
        public FormValidation doValidateAddress(@QueryParameter String address) {

            URL host = null;

            // no empty addresses allowed
            if (address == null || address.trim().equals("")) {
                return FormValidation.error("The remote address can not be left empty.");
            }

            // check if we have a valid, well-formed URL
            try {
                host = new URL(address);
                host.toURI();
            } catch (Exception e) {
                return FormValidation.error("Malformed address (" + address + "), please double-check it.");
            }

            // check that the host is reachable
            try {
                HttpURLConnection connection = (HttpURLConnection) host.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();
            } catch (Exception e) {
                return FormValidation.warning("Address looks good, but we were not able to connect to it");
            }

            return FormValidation.okWithMarkup("Address looks good");
        }

        public static List<Auth2Descriptor> getAuth2Descriptors() {
            return Auth2.all();
        }

        public static Auth2Descriptor getDefaultAuth2Descriptor() {
            return NoneAuth.DESCRIPTOR;
        }
    }
}
