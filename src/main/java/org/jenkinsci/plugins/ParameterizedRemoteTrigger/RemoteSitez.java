package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;

import hudson.model.Descriptor;

import hudson.util.FormValidation;

public class RemoteSitez extends AbstractDescribableImpl<RemoteSitez> {

    private final URL address;
    private final String displayName;

    @DataBoundConstructor
    public RemoteSitez(String address, String displayName)
            throws MalformedURLException {
        this.address = new URL(address);

        if (displayName == null || displayName.trim().equals("")) {
            this.displayName = this.getAddress().toString();
        } else {
            this.displayName = displayName;
        }

    }

    // XXX: need to add a bunch of stuff around URL validation (take it all from
    // RemoteBuilder.java)

    public String getDisplayName() {
        return displayName;
    }

    public URL getAddress() {
        return address;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RemoteSitez> {

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "";
        }

        /**
         * Validates the given address to see that it's well-formed, and is
         * reachable.
         * 
         * @param address
         *            Remote address to be validated
         * @return FormValidation object
         */
        public FormValidation doValidateAddress(@QueryParameter String address) {

            URL host = null;

            // check if we have a valid, well-formed URL
            try {
                host = new URL(address);
                URI uri = host.toURI();
            } catch (Exception e) {
                return FormValidation.error("Malformed URL (" + address
                        + "), please double-check your address");
            }

            // check that the host is reachable
            try {
                HttpURLConnection connection = (HttpURLConnection) host
                        .openConnection();
                connection.connect();
            } catch (Exception e) {
                return FormValidation
                        .error("Unable to connect to remote Jenkins: "
                                + address);
            }

            return FormValidation.okWithMarkup("Address looks good");
        }
    }

}
