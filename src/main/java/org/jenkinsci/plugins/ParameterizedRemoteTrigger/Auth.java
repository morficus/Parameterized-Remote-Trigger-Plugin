package org.jenkinsci.plugins.ParameterizedRemoteTrigger;


import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class Auth extends AbstractDescribableImpl<Auth> {
    
    private final String value;
    private final String username;
    private final String apiToken;
    
    @DataBoundConstructor
    public Auth(String value, String username, String apiToken) {
        this.value = value;
        this.username = username;
        this.apiToken = apiToken;
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
    
    @Extension
    public static class DescriptorImpl extends Descriptor<Auth> {
        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
