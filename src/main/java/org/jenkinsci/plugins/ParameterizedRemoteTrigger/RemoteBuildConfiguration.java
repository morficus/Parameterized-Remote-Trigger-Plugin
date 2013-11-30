package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import hudson.AbortException;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.CopyOnWriteList;
import hudson.util.ListBoxModel;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * @author Maurice W.
 * 
 */
public class RemoteBuildConfiguration extends Builder {

    private final String       token;
    private final String       remoteJenkinsName;
    private final String       job;
    // "parameters" is the raw string entered by the user
    private final String       parameters;
    // "parameterList" is the cleaned-up version of "parameters" (stripped out comments, character encoding, etc)
    private final List<String> parameterList;

    private static String      paramerizedBuildUrl = "/buildWithParameters";
    private static String      normalBuildUrl      = "/build";
    private static String      buildTokenRootUrl   = "/buildByToken";

    private String             queryString         = "";

    @DataBoundConstructor
    public RemoteBuildConfiguration(String remoteJenkinsName, String job, String token, String parameters)
            throws MalformedURLException {

        this.token = token.trim();
        this.remoteJenkinsName = remoteJenkinsName;
        this.parameters = parameters;
        this.job = job.trim();

        // split the parameter-string into an array based on the new-line character
        String[] params = parameters.split("\n");

        // convert the String array into a List of Strings, and remove any empty entries
        this.parameterList = new ArrayList<String>(Arrays.asList(params));
        this.cleanUpParameters();
    }

    /**
     * A convenience function to clean up any type of unwanted items from the parameterList
     */
    private void cleanUpParameters() {
        this.removeEmptyElements();
        this.removeCommentsFromParameters();
    }

    /**
     * Strip out any empty strings from the parameterList
     */
    private void removeEmptyElements() {
        this.parameterList.removeAll(Arrays.asList(null, ""));
        this.parameterList.removeAll(Arrays.asList(null, " "));
    }

    /**
     * Strip out any comments (lines that start with a #) from the parameterList
     */
    private void removeCommentsFromParameters() {
        List<String> itemsToRemove = new ArrayList<String>();

        for (String parameter : this.parameterList) {
            if (parameter.indexOf("#") == 0) {
                itemsToRemove.add(parameter);
            }
        }

        this.parameterList.removeAll(itemsToRemove);
    }

    /**
     * Return the parameterList in an encoded query-string
     * 
     * @return query-parameter-formated URL-encoded string
     */
    private String buildUrlQueryString() {

        // List to hold the encoded parameters
        List<String> encodedParameters = new ArrayList<String>();

        for (String parameter : this.parameterList) {
            // Step #1 - break apart the parameter-pairs (because we don't want to encode the "=" character)
            String[] splitParameters = parameter.split("=");

            // List to hold each individually encoded parameter item
            List<String> encodedItems = new ArrayList<String>();
            for (String item : splitParameters) {
                try {
                    // Step #2 - encode each individual parameter item add the encoded item to its corresponding list
                    encodedItems.add(URLEncoder.encode(item, "UTF-8"));
                } catch (Exception e) {
                    // do nothing
                    // because we are "hard-coding" the encoding type, there is a 0% chance that this will fail.
                }

            }

            // Step #3 - reunite the previously separated parameter items and add them to the corresponding list
            encodedParameters.add(StringUtils.join(encodedItems, "="));
        }

        return StringUtils.join(encodedParameters, "&");
    }

    /**
     * Lookup up a Remote Jenkins Server based on display name
     * 
     * @param displayName
     *            Name of the configuration you are looking for
     * @return A RemoteSitez object
     */
    public RemoteJenkinsServer findRemoteHost(String displayName) {
        RemoteJenkinsServer match = null;

        for (RemoteJenkinsServer host : this.getDescriptor().remoteSites) {
            // if we find a match, then stop looping
            if (displayName.equals(host.getDisplayName())) {
                match = host;
                break;
            }
        }

        return match;
    }

    private void addToQueryString(String item) {
        String currentQueryString = this.getQueryString();
        String newQueryString = "";

        if (currentQueryString == null || currentQueryString.equals("")) {
            newQueryString = item;
        } else {
            newQueryString = currentQueryString + "&" + item;
        }
        this.setQueryString(newQueryString);
    }

    private String buildTriggerUrl() {
        RemoteJenkinsServer remoteServer = this.findRemoteHost(this.getRemoteJenkinsName());
        String triggerUrlString = remoteServer.getAddress().toString();

        if (remoteServer.getHasBuildTokenRootSupport()) {
            triggerUrlString += buildTokenRootUrl;
            triggerUrlString += getBuildTypeUrl();
            this.addToQueryString("job=" + this.getJob(true));

        } else {
            triggerUrlString += "/job/";
            triggerUrlString += this.getJob(true);
            triggerUrlString += getBuildTypeUrl();
        }

        // don't include a token in the URL if none is provided
        if (!this.getToken().equals("")) {
            this.addToQueryString("token=" + this.getToken(true));
        }

        String buildParams = this.getParameters(true);
        if (!buildParams.isEmpty()) {
            this.addToQueryString(buildParams);
        }

        this.addToQueryString("delay=0");

        triggerUrlString += "?" + this.getQueryString();

        return triggerUrlString;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {

        String triggerUrlString = this.buildTriggerUrl();
        RemoteJenkinsServer remoteServer = this.findRemoteHost(this.getRemoteJenkinsName());

        listener.getLogger().println("Triggering this job: " + this.getJob());
        listener.getLogger().println("Using this remote Jenkins config: " + this.getRemoteJenkinsName());
        listener.getLogger().println("With these parameters: " + this.parameterList.toString());

        // uncomment the 2 lines below for debugging purposes only
        // listener.getLogger().println("Fully Built URL: " + triggerUrlString);
        // listener.getLogger().println("Token: " + this.getToken());

        HttpURLConnection connection = null;

        try {
            URL triggerUrl = new URL(triggerUrlString);
            connection = (HttpURLConnection) triggerUrl.openConnection();

            // if there is a username + apiToken defined for this remote host, then use it
            String usernameTokenConcat = remoteServer.getUsername() + ":" + remoteServer.getApiToken();
            if (!usernameTokenConcat.equals(":")) {
                byte[] encodedAuthKey = Base64.encodeBase64(usernameTokenConcat.getBytes());
                connection.setRequestProperty("Authorization", "Basic " + new String(encodedAuthKey));
            }

            connection.setDoInput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            // wait up to 5 seconds for the connection to be open
            connection.setConnectTimeout(5000);
            connection.connect();

            // TODO: right now this is just doing a "fire and forget", but would be nice to get some feedback from the
            // remote server. To accomplish this we would need to poll some URL
            // - http://jenkins.local/job/test/lastBuild/api/json

            InputStream is = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            // String line;
            // StringBuffer response = new StringBuffer();

            // while ((line = rd.readLine()) != null) {
            // System.out.println(line);
            // }
            // rd.close();

        } catch (IOException e) {
            // something failed with the connection, so throw an exception to mark the build as failed.
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        } finally {
            // always make sure we close the connection
            if (connection != null) {
                connection.disconnect();
            }

            // and always clear the query string
            this.clearQueryString();

        }

        return true;
    }

    // Getters
    public String getRemoteJenkinsName() {
        return this.remoteJenkinsName;
    }

    public String getJob(boolean isEncoded) {
        String jobName = this.getJob();
        if (isEncoded) {
            try {
                jobName = URLEncoder.encode(jobName, "UTF-8").replace("+", "%20");
            } catch (Exception e) {
                // do nothing
                // because we are "hard-coding" the encoding type, there is a 0% chance that this will fail.
            }

        }
        return jobName;
    }

    public String getJob() {
        return this.job;
    }

    public String getToken(boolean isEncoded) {
        String token = this.getToken();
        if (isEncoded) {
            try {
                token = URLEncoder.encode(token, "UTF-8").replace("+", "%20");
            } catch (Exception e) {
                // do nothing
                // because we are "hard-coding" the encoding type, there is a 0% chance that this will fail.
            }

        }
        return token;
    }

    public String getToken() {
        return this.token;
    }

    public String getParameters(Boolean asString) {
        if (asString) {
            return this.buildUrlQueryString();
        } else {
            return this.parameters;
        }
    }

    private String getBuildTypeUrl() {
        boolean isParameterized = (this.getParameters().length() > 0);

        if (isParameterized) {
            return RemoteBuildConfiguration.paramerizedBuildUrl;
        } else {
            return RemoteBuildConfiguration.normalBuildUrl;
        }
    }

    public String getParameters() {
        return this.parameters;
    }

    public String getQueryString() {
        return this.queryString;
    }

    private void setQueryString(String string) {
        this.queryString = string.trim();
    }

    private void clearQueryString() {
        this.setQueryString("");
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information, simply store it in a field and call save().
         * 
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private CopyOnWriteList<RemoteJenkinsServer> remoteSites = new CopyOnWriteList<RemoteJenkinsServer>();

        /**
         * In order to load the persisted global configuration, you have to call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        /*
         * public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException { if
         * (value.length() == 0) return FormValidation.error("Please set a name"); if (value.length() < 4) return
         * FormValidation.warning("Isn't the name too short?"); return FormValidation.ok(); }
         */

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Trigger a remote parameterized job";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

            remoteSites.replaceBy(req.bindJSONToList(RemoteJenkinsServer.class, formData.get("remoteSites")));
            save();

            return super.configure(req, formData);
        }

        public ListBoxModel doFillRemoteJenkinsNameItems() {
            ListBoxModel model = new ListBoxModel();

            for (RemoteJenkinsServer site : getRemoteSites()) {
                model.add(site.getDisplayName());
            }

            return model;
        }

        public RemoteJenkinsServer[] getRemoteSites() {

            return remoteSites.toArray(new RemoteJenkinsServer[this.remoteSites.size()]);
        }
    }
}
