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

/**
 * Holds everything regarding the remote server we wish to connect to,
 * including validations and what not.
 * 
 * @author Maurice W.
 *
 */
public class RemoteJenkinsServer extends AbstractDescribableImpl<RemoteJenkinsServer> {

    private final URL address;
    private final String displayName;

    @DataBoundConstructor
    public RemoteJenkinsServer(String address, String displayName)
            throws MalformedURLException {

        this.address = new URL(address);

        if (displayName == null || displayName.trim().equals("")) {
            this.displayName = this.getAddress().toString();
        } else {
            this.displayName = displayName;
        }

    }

    // Getters
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
    public static class DescriptorImpl extends Descriptor<RemoteJenkinsServer> {

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        /*public DescriptorImpl() {
            load();
        }*/

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

            if(address == null || address.trim().equals("")) {
                return FormValidation.error("The remote address can not be left empty");
            }
            
            // check if we have a valid, well-formed URL
            try {
                host = new URL(address);
                URI uri = host.toURI();
            } catch (Exception e) {
                return FormValidation.error("Malformed URL (" + address + "), please double-check the address");
            }

            // check that the host is reachable
            try {
                HttpURLConnection connection = (HttpURLConnection) host.openConnection();
                connection.connect();
            } catch (Exception e) {
                return FormValidation
                        .error("Address looks good, but unable to connect: " + address);
            }

            return FormValidation.okWithMarkup("Address looks good");
        }
    }

}
