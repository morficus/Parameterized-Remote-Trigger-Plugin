package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

public class Auth extends AbstractDescribableImpl<Auth> {

    private final String authType;
    private final String username;
    private final String apiToken;
    private final String creds;

    public final String  NONE               = "none";
    public final String  API_TOKEN          = "apiToken";
    public final String  CREDENTIALS_PLUGIN = "credentialsPlugin";

    @DataBoundConstructor
    public Auth(String value, String username, String apiToken, String creds) {
        this.authType = value;
        this.username = username;
        this.apiToken = apiToken;
        this.creds = creds;
    }

    public String getAuthType() {
        return this.authType;
    }

    public Boolean isMatch(String value) {
        return this.getAuthType().equals(value);
    }

    public String getUsername() {
        String authType = this.getAuthType();
        String username = null;

        if (authType.equals(NONE)) {
            username = "";
        } else if (authType.equals(API_TOKEN)) {
            username = this.username;
        } else if (authType.equals(CREDENTIALS_PLUGIN)) {
            username = this.getCredentials().getUsername();
        }

        return username;
    }

    public String getPassword() {
        String authType = this.getAuthType();
        String password = null;

        if (authType.equals(NONE)) {
            password = "";
        } else if (authType.equals(API_TOKEN)) {
            password = this.getApiToken();
        } else if (authType.equals(CREDENTIALS_PLUGIN)) {
            password = Secret.toString(this.getCredentials().getPassword());
        }

        return password;
    }

    public String getApiToken() {
        return this.apiToken;
    }

    public String getCreds() {
        return this.creds;
    }

    private UsernamePasswordCredentials getCredentials() {
        String credetialId = this.getCreds();
        StandardUsernameCredentials matchedCredentials = null;
        Item item = null;

        List<StandardUsernameCredentials> listOfCredentials = CredentialsProvider.lookupCredentials(
                StandardUsernameCredentials.class, item, ACL.SYSTEM, Collections.<DomainRequirement> emptyList());
        for (StandardUsernameCredentials cred : listOfCredentials) {
            if (credetialId.equals(cred.getId())) {
                matchedCredentials = cred;
                break;
            }
        }

        // now we have matchedCredentials.getPassword() and matchedCredentials.getUsername();
        return (UsernamePasswordCredentials) matchedCredentials;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Auth> {
        @Override
        public String getDisplayName() {
            return "";
        }

        public static ListBoxModel doFillCredsItems() {
            StandardUsernameListBoxModel model = new StandardUsernameListBoxModel();

            Item item = Stapler.getCurrentRequest().findAncestorObject(Item.class);
            @SuppressWarnings("unchecked")
            Iterable<? extends StandardUsernameCredentials> listOfSandardUsernameCredentials = (Iterable<? extends StandardUsernameCredentials>) CredentialsProvider
                    .lookupCredentials(UsernamePasswordCredentials.class, item, ACL.SYSTEM,
                            Collections.<DomainRequirement> emptyList());

            model.withAll(listOfSandardUsernameCredentials);

            return model;
        }

    }
}
