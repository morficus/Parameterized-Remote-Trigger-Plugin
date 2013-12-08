package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
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

    private final URL     address;
    private final String  displayName;
    private final boolean hasBuildTokenRootSupport;
    private final Auth    authenticationMode;
    // private final JSONObject authenticationMode;
    // private final String authenticationMode;
    private final String  username;
    private final String  apiToken;

    @DataBoundConstructor
    public RemoteJenkinsServer(String address, String displayName, boolean hasBuildTokenRootSupport,
            Auth authenticationMode) throws MalformedURLException {

        this.address = new URL(address);
        this.displayName = displayName.trim();
        this.hasBuildTokenRootSupport = hasBuildTokenRootSupport;
        this.authenticationMode = authenticationMode;
        // this.authenticationMode = authenticationMode;

        // Holding on to both of these variables for legacy purposes. The seemingly 'dirty' getters for these properties
        // are for the same reason.
        this.username = "";
        this.apiToken = "";

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

    public URL getAddress() {
        return address;
    }

    public Auth getAuthenticationMode() {
        return this.authenticationMode;
    }

    public boolean getHasBuildTokenRootSupport() {
        return this.hasBuildTokenRootSupport;
    }

    public String getUsername() {
        String username = "";
        if (this.username == null || this.username.equals("")) {
            username = this.authenticationMode.getUsername();
        } else {
            username = this.username;
        }
        return username;
    }

    public String getApiToken() {
        String apiToken = "";
        if (this.apiToken == null || this.apiToken.equals("")) {
            apiToken = this.authenticationMode.getApiToken();
        } else {
            apiToken = this.apiToken;
        }
        return apiToken;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteJenkinsServer> {

        private JSONObject authenticationMode;

        /**
         * In order to load the persisted global configuration, you have to call load() in the constructor.
         */
        /*
         * public DescriptorImpl() { load(); }
         */

        public String getDisplayName() {
            return "";
        }

        public JSONObject doFillAuthenticationMode() {
            return this.authenticationMode.getJSONObject("value");
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
                URI uri = host.toURI();
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
    }

}
