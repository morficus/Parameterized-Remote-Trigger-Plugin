package org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2;

import java.io.IOException;
import java.net.URLConnection;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Item;

public class NoneAuth extends Auth2 {

    private static final long serialVersionUID = -3128995428538415113L;

    @Extension
    public static final Auth2Descriptor DESCRIPTOR = new NoneAuthDescriptor();

    @DataBoundConstructor
    public NoneAuth() {
    }

    @Override
    public void setAuthorizationHeader(URLConnection connection, BuildContext context) throws IOException {
        connection.setRequestProperty("Authorization", null);
    }

    @Override
    public String toString() {
        return "'" + getDescriptor().getDisplayName() + "'";
    }

    @Override
    public String toString(Item item) {
        return toString();
    }

    @Override
    public Auth2Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Symbol("NoneAuth")
    public static class NoneAuthDescriptor extends Auth2Descriptor {
        @Override
        public String getDisplayName() {
            return "No Authentication";
        }
    }

}
