package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import java.util.Collections;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

public class Auth extends AbstractDescribableImpl<Auth> {

    private final String value;
    private final String username;
    private final String apiToken;
    private final String creds;

    @DataBoundConstructor
    public Auth(String value, String username, String apiToken, String creds) {
        this.value = value;
        this.username = username;
        this.apiToken = apiToken;
        this.creds = creds;
    }

    public String getValue() {
        return this.value;
    }

    public Boolean isMatch(String value) {
        return this.getValue().equals(value);
    }

    public String getUsername() {
        return this.username;
    }

    public String getApiToken() {
        return this.apiToken;
    }

    public String getCreds() {
        return this.creds;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Auth> {
        @Override
        public String getDisplayName() {
            return "";
        }

        // doFillRemoteJenkinsNameItems
        public static ListBoxModel doFillCredsItems() {
            StandardUsernameListBoxModel model = new StandardUsernameListBoxModel();

            Item item = Stapler.getCurrentRequest().findAncestorObject(Item.class);
            model.withAll(CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, item, ACL.SYSTEM,
                    Collections.<DomainRequirement> emptyList()));

            return model;
        }

    }
}
