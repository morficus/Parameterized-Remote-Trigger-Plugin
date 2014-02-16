package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import hudson.AbortException;
import hudson.FilePath;
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

import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * @author Maurice W.
 * 
 */
public class RemoteBuildConfiguration extends Builder {

    private final String          token;
    private final String          remoteJenkinsName;
    private final String          job;
    private final boolean         shouldNotFailBuild;
    // "parameters" is the raw string entered by the user
    private final String          parameters;
    // "parameterList" is the cleaned-up version of "parameters" (stripped out comments, character encoding, etc)

    private final List<String>    parameterList;

    private static String         paramerizedBuildUrl = "/buildWithParameters";
    private static String         normalBuildUrl      = "/build";
    private static String         buildTokenRootUrl   = "/buildByToken";

    private final boolean         overrideAuth;
    private CopyOnWriteList<Auth> auth                = new CopyOnWriteList<Auth>();

    private final boolean         loadParamsFromFile;
    private String                parameterFile       = "";

    private String                queryString         = "";

    @DataBoundConstructor
    public RemoteBuildConfiguration(String remoteJenkinsName, boolean shouldNotFailBuild, String job, String token,
            String parameters, JSONObject overrideAuth, JSONObject loadParamsFromFile) throws MalformedURLException {

        this.token = token.trim();
        this.remoteJenkinsName = remoteJenkinsName;
        this.parameters = parameters;
        this.job = job.trim();
        this.shouldNotFailBuild = shouldNotFailBuild;
        if (overrideAuth != null && overrideAuth.has("auth")) {
            this.overrideAuth = true;
            this.auth.replaceBy(new Auth(overrideAuth.getJSONObject("auth")));
        } else {
            this.overrideAuth = false;
            this.auth.replaceBy(new Auth(new JSONObject()));
        }

        if (loadParamsFromFile != null && loadParamsFromFile.has("parameterFile")) {
            this.loadParamsFromFile = true;
            this.parameterFile = loadParamsFromFile.getString("parameterFile");
        } else {
            this.loadParamsFromFile = false;
        }

        // split the parameter-string into an array based on the new-line character
        String[] params = parameters.split("\n");

        // convert the String array into a List of Strings, and remove any empty entries
        this.parameterList = new ArrayList<String>(Arrays.asList(params));

    }

    public RemoteBuildConfiguration(String remoteJenkinsName, boolean shouldNotFailBuild, String job, String token,
            String parameters) throws MalformedURLException {

        this.token = token.trim();
        this.remoteJenkinsName = remoteJenkinsName;
        this.parameters = parameters;
        this.job = job.trim();
        this.shouldNotFailBuild = shouldNotFailBuild;
        this.overrideAuth = false;
        this.auth.replaceBy(new Auth(null));

        // split the parameter-string into an array based on the new-line character
        String[] params = parameters.split("\n");

        // convert the String array into a List of Strings, and remove any empty entries
        this.parameterList = new ArrayList<String>(Arrays.asList(params));

    }

    /**
     * Reads a file from the jobs workspace, and loads the list of parameters from with in it.
     * It will also call ```getCleanedParameters``` before returning.
     * 
     * @param build
     * @return List<String> of build parameters
     */
    private List<String> loadExternalParameterFile(AbstractBuild<?, ?> build) {

        FilePath workspace = build.getWorkspace();
        BufferedReader br = null;
        List<String> ParameterList = new ArrayList<String>();
        try {

            String filePath = workspace + this.getParameterFile();
            String sCurrentLine;
            String fileContent = "";

            br = new BufferedReader(new FileReader(filePath));

            while ((sCurrentLine = br.readLine()) != null) {
                // fileContent += sCurrentLine;
                ParameterList.add(sCurrentLine);
            }

            // ParameterList = new ArrayList<String>(Arrays.asList(fileContent));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        // FilePath.
        return getCleanedParameters(ParameterList);
    }

    /**
     * Strip out any empty strings from the parameterList
     */
    private void removeEmptyElements(Collection<String> collection) {
        collection.removeAll(Arrays.asList(null, ""));
        collection.removeAll(Arrays.asList(null, " "));
    }

    /**
     * Convenience method
     * 
     * @return List<String> of build parameters
     */
    private List<String> getCleanedParameters() {

        return getCleanedParameters(this.getParameterList());
    }

    /**
     * Same as "getParameterList", but removes comments and empty strings Notice that no type of character encoding is
     * happening at this step. All encoding happens in the "buildUrlQueryString" method.
     * 
     * @param List
     *            <String> parameters
     * @return List<String> of build parameters
     */
    private List<String> getCleanedParameters(List<String> parameters) {
        List<String> params = new ArrayList<String>(parameters);
        removeEmptyElements(params);
        removeCommentsFromParameters(params);
        return params;
    }

    /**
     * Similar to "replaceToken", but acts on a list in place of just a single string
     * 
     * @param build
     * @param listener
     * @param params
     *            List<String> of params to be tokenized/replaced
     * @return List<String> of resolved variables/tokens
     */
    private List<String> replaceTokens(AbstractBuild<?, ?> build, BuildListener listener, List<String> params) {
        List<String> tokenizedParams = new ArrayList<String>();

        for (int i = 0; i < params.size(); i++) {
            tokenizedParams.add(replaceToken(build, listener, params.get(i)));
            // params.set(i, replaceToken(build, listener, params.get(i)));
        }

        return tokenizedParams;
    }

    /**
     * Resolves any environment variables in the string
     * 
     * @param build
     * @param listener
     * @param input
     *            String to be tokenized/replaced
     * @return String with resolved Environment variables
     */
    private String replaceToken(AbstractBuild<?, ?> build, BuildListener listener, String input) {
        try {
            return TokenMacro.expandAll(build, listener, input);
        } catch (Exception e) {
            listener.getLogger().println(
                    String.format("Failed to resolve parameters in string %s due to following error:\n%s", input,
                            e.getMessage()));
        }
        return input;
    }

    /**
     * Strip out any comments (lines that start with a #) from the collection that is passed in.
     */
    private void removeCommentsFromParameters(Collection<String> collection) {
        List<String> itemsToRemove = new ArrayList<String>();

        for (String parameter : collection) {
            if (parameter.indexOf("#") == 0) {
                itemsToRemove.add(parameter);
            }
        }
        collection.removeAll(itemsToRemove);
    }

    /**
     * Return the Collection<String> in an encoded query-string
     * 
     * @return query-parameter-formated URL-encoded string
     * @throws InterruptedException
     * @throws IOException
     * @throws MacroEvaluationException
     */
    private String buildUrlQueryString(Collection<String> parameters) {

        // List to hold the encoded parameters
        List<String> encodedParameters = new ArrayList<String>();

        for (String parameter : parameters) {

            // Step #1 - break apart the parameter-pairs (because we don't want to encode the "=" character)
            String[] splitParameters = parameter.split("=");

            // List to hold each individually encoded parameter item
            List<String> encodedItems = new ArrayList<String>();
            for (String item : splitParameters) {
                try {
                    // Step #2 - encode each individual parameter item add the encoded item to its corresponding list

                    encodedItems.add(encodeValue(item));

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

    /**
     * Helper function to allow values to be added to the query string from any method.
     * 
     * @param item
     */
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

    /**
     * Build the proper URL to trigger the remote build
     * 
     * All passed in string have already had their tokens replaced with real values. All 'params' also have the proper
     * character encoding
     * 
     * @param job
     *            Name of the remote job
     * @param securityToken
     *            Security token used to trigger remote job
     * @param params
     *            Parameters for the remote job
     * @return fully formed, fully qualified remote trigger URL
     */
    private String buildTriggerUrl(String job, String securityToken, Collection<String> params) {
        RemoteJenkinsServer remoteServer = this.findRemoteHost(this.getRemoteJenkinsName());
        String triggerUrlString = remoteServer.getAddress().toString();

        // start building the proper URL based on known capabiltiies of the remote server
        if (remoteServer.getHasBuildTokenRootSupport()) {
            triggerUrlString += buildTokenRootUrl;
            triggerUrlString += getBuildTypeUrl();

            this.addToQueryString("job=" + this.encodeValue(job));

        } else {
            triggerUrlString += "/job/";
            triggerUrlString += this.encodeValue(job);
            triggerUrlString += getBuildTypeUrl();
        }

        // don't try to include a security token in the URL if none is provided
        if (!securityToken.equals("")) {
            this.addToQueryString("token=" + encodeValue(securityToken));
        }

        // turn our Collection into a query string
        String buildParams = buildUrlQueryString(params);

        if (!buildParams.isEmpty()) {
            this.addToQueryString(buildParams);
        }

        // by adding "delay=0", this will (theoretically) force this job to the top of the remote queue
        this.addToQueryString("delay=0");

        triggerUrlString += "?" + this.getQueryString();

        return triggerUrlString;
    }

    /**
     * Convenience function to mark the build as failed. It's intended to only be called from this.perform();
     * 
     * @param e
     *            Exception that caused the build to fail
     * @param listener
     *            Build Listener
     * @throws IOException
     */
    private void failBuild(Exception e, BuildListener listener) throws IOException {
        e.getStackTrace();
        if (this.getShouldNotFailBuild()) {
            listener.error("Remote build failed for the following reason, but the build will continue:");
            listener.error(e.getMessage());
        } else {
            listener.error("Remote build failed for the following reason:");
            throw new AbortException(e.getMessage());
        }
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {

        RemoteJenkinsServer remoteServer = this.findRemoteHost(this.getRemoteJenkinsName());

        if (remoteServer == null) {
            this.failBuild(new Exception("No remote host is defined for this job."), listener);
            return true;
        }

        List<String> parameters = null;

        if (this.loadParamsFromFile) {
            parameters = loadExternalParameterFile(build);
        } else {
            // tokenize all variables and encode all variables, then build the fully-qualified trigger URL
            parameters = getCleanedParameters();
            parameters = replaceTokens(build, listener, parameters);
        }

        String jobName = replaceToken(build, listener, this.getJob());
        String securityToken = replaceToken(build, listener, this.getToken());
        String triggerUrlString = this.buildTriggerUrl(jobName, securityToken, parameters);

        // print out some debugging information to the console
        listener.getLogger().println("URL: " + triggerUrlString);
        listener.getLogger().println("Triggering this job: " + jobName);
        listener.getLogger().println("Using this remote Jenkins config: " + this.getRemoteJenkinsName());
        if (this.getOverrideAuth()) {
            listener.getLogger().println(
                    "Using job-level defined credentails in place of those from remote Jenkins config ["
                            + this.getRemoteJenkinsName() + "]");
        }
        listener.getLogger().println("With these parameters: " + parameters.toString());

        // uncomment the 2 lines below for debugging purposes only
        // listener.getLogger().println("Fully Built URL: " + triggerUrlString);
        // listener.getLogger().println("Token: " + securityToken);

        HttpURLConnection connection = null;

        try {
            URL triggerUrl = new URL(triggerUrlString);
            connection = (HttpURLConnection) triggerUrl.openConnection();

            // if there is a username + apiToken defined for this remote host, then use it
            String usernameTokenConcat = "";

            if (this.getOverrideAuth()) {
                usernameTokenConcat = this.getAuth()[0].getUsername() + ":" + this.getAuth()[0].getPassword();
            } else {
                usernameTokenConcat = remoteServer.getAuth()[0].getUsername() + ":"
                        + remoteServer.getAuth()[0].getPassword();
            }

            if (!usernameTokenConcat.equals(":")) {
                // token-macro replacment
                usernameTokenConcat = TokenMacro.expandAll(build, listener, usernameTokenConcat);

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
            this.failBuild(e, listener);
        } catch (MacroEvaluationException e) {
            this.failBuild(e, listener);
        } catch (InterruptedException e) {
            this.failBuild(e, listener);
        } finally {
            // always make sure we close the connection
            if (connection != null) {
                connection.disconnect();
            }

            // and always clear the query string and remove some "global" values
            this.clearQueryString();
            // this.build = null;
            // this.listener = null;

        }

        return true;
    }

    /**
     * Helper function for character encoding
     * 
     * @param dirtyValue
     * @return encoded value
     */
    private String encodeValue(String dirtyValue) {
        String cleanValue = "";

        try {
            cleanValue = URLEncoder.encode(dirtyValue, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return cleanValue;
    }

    // Getters
    public String getRemoteJenkinsName() {
        return this.remoteJenkinsName;
    }

    public String getJob() {
        return this.job;
    }

    public boolean getShouldNotFailBuild() {
        return this.shouldNotFailBuild;
    }

    public String getToken() {
        return this.token;
    }

    private String getParameterFile() {
        return this.parameterFile;
    }

    private String getBuildTypeUrl() {
        boolean isParameterized = (this.getParameters().length() > 0);

        if (isParameterized) {
            return RemoteBuildConfiguration.paramerizedBuildUrl;
        } else {
            return RemoteBuildConfiguration.normalBuildUrl;
        }
    }

    public boolean getOverrideAuth() {
        return this.overrideAuth;
    }

    public Auth[] getAuth() {
        return auth.toArray(new Auth[this.auth.size()]);

    }

    public String getParameters() {
        return this.parameters;
    }

    private List<String> getParameterList() {
        return this.parameterList;
    }

    public String getQueryString() {
        return this.queryString;
    }

    private void setQueryString(String string) {
        this.queryString = string.trim();
    }

    /**
     * Convenience function for setting the query string to empty
     */
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
