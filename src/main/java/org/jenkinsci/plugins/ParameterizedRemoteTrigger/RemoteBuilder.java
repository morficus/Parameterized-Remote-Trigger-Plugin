package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
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
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RemoteBuilder extends Builder {

    private final String token;
    private final String remoteJenkinsName;
    private final String job;
    // "parameters" is the raw string entered by the user
    private final String parameters;
    // "parameterList" is the cleaned-up version of "parameters" (stripped out
    // comments, character encoding, etc)
    private final List<String> parameterList;

    private static String paramerizedBuildUrl = "/buildWithParameters";
    private static String normalBuildUrl = "/build";

    @DataBoundConstructor
    public RemoteBuilder(String remoteSites, String job, String token,
            String parameters) throws MalformedURLException {

        this.token = token;
        this.remoteJenkinsName = remoteSites;
        this.parameters = parameters;
        this.job = job;

        // split the parameter-string into an array based on the new-line
        // character
        String[] params = parameters.split("\n");

        // convert the String array into a List of Strings, and remove any empty
        // entries
        this.parameterList = new ArrayList<String>(Arrays.asList(params));
        this.cleanUpParameters();
    }

    /**
     * A convenience function to clean up any type of unwanted items from the
     * parameterList
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
    }

    /**
     * Strip out any comments (lines that start with a #) from the parameterList
     */
    private void removeCommentsFromParameters() {
        List<String> itemsToRemove = new ArrayList<String>();

        for(String parameter : this.parameterList) {
            if(parameter.indexOf("#") == 0)
            {
                itemsToRemove.add(parameter);
            }
        }

        this.parameterList.removeAll(itemsToRemove);
    }

    /**
     * Return the parameterList in an encoded query-string
     * 
     * @return URL-encoded string 
     */
    public String buildUrlQueryString() {

        //List to hold the encoded parameters
        List<String> encodedParameters = new ArrayList<String>();
        
        
        for(String parameter : this.parameterList) {
            //Step #1 - break apart the parameter-pairs (because we don't want to encode the "=" character)
            String[] splitParameters = parameter.split("=");
            
            //List to hold each individually encoded parameter item
            List<String> encodedItems = new ArrayList<String>();
            for(String item : splitParameters) {
                try {
                    //Step #2 - encode each individual parameter item add the encoded item to its corresponding list
                    encodedItems.add(URLEncoder.encode(item, "UTF-8"));
                }catch(Exception e) {
                    //do nothing
                    //because we are "hard-coding" the encoding type, there is a 0% chance that this will fail.
                }
                
            }
            
            //Step #3 - reunite the previously separated parameter items and add them to the corresponding list
            encodedParameters.add(StringUtils.join(encodedItems, "="));
        }
        

        return StringUtils.join(encodedParameters, "&");
    }

    /**
     * Lookup up a Remote Jenkins Configuration based on display name
     * 
     * @param displayName
     *            Name of the configuration you are looking for
     * @return A RemoteSitez object
     */
    public RemoteSitez findRemoteHost(String displayName) {
        RemoteSitez match = null;

        for (RemoteSitez host : this.getDescriptor().remoteSites) {
            // if we find a match, then stop looping
            if (displayName.equals(host.getDisplayName())) {
                match = host;
                break;
            }
        }

        return match;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
            BuildListener listener) {

        // String myTriggerURLString = this.getHostname() + "/job/" +
        // this.getJob() + this.paramerizedBuildUrl + "?" + "token=" +
        // this.getToken() + "&" + this.buildUrlQueryString();
        String myTriggerURLString = this
                .findRemoteHost(this.getRemoteJenkinsName()).getAddress()
                .toString();
        myTriggerURLString += "/jobs/" + this.getJob() + paramerizedBuildUrl
                + "?" + "token=" + this.getToken() + "&"
                + this.buildUrlQueryString();

        listener.getLogger().println("Token: " + this.getToken());
        listener.getLogger().println(
                "Jenkins config: " + this.getRemoteJenkinsName());
        listener.getLogger().println("Remote Job: " + this.getJob());
        listener.getLogger().println(
                "Parameters: " + this.parameterList.toString());
        listener.getLogger().println("Fully Built URL: " + myTriggerURLString);

        HttpURLConnection connection = null;

        try {
            URL triggerUrl = new URL(myTriggerURLString);
            connection = (HttpURLConnection) triggerUrl.openConnection();

            connection.setDoInput(true);
            // connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            connection.connect();

            // connection.setDoOutput(true);

            // TODO: right now this just "fires and forgets". will need to poll
            // the remote server to check for success or failure
            // http://jenkins.local/job/test/lastBuild/api/json

            /*
             * InputStream is = connection.getInputStream(); BufferedReader rd =
             * new BufferedReader(new InputStreamReader(is)); String line;
             * StringBuffer response = new StringBuffer();
             * 
             * while((line = rd.readLine()) != null) { System.out.println(line);
             * 
             * }
             * 
             * rd.close();
             */

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    // Getters
    public String getRemoteJenkinsName() {
        return this.remoteJenkinsName;
    }

    public String getJob() {
        return this.job;
    }

    public String getToken() {
        return this.token;
    }

    public String getParameters() {
        return this.parameters;
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
         * To persist global configuration information, simply store it in a
         * field and call save().
         * 
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;
        private CopyOnWriteList<RemoteSitez> remoteSites = new CopyOnWriteList<RemoteSitez>();

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the
         *         browser.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

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
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            // useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            // (easier when there are many fields; need set* methods for this,
            // like setUseFrench)
            // save();
            // return super.configure(req,formData);

            remoteSites.replaceBy(req.bindJSONToList(RemoteSitez.class,
                    formData.get("remoteSites")));
            save();

            return super.configure(req, formData);
        }

        public ListBoxModel doFillRemoteSitesItems() {
            ListBoxModel model = new ListBoxModel();

            for (RemoteSitez site : getRemoteSites()) {
                model.add(site.getDisplayName());
            }

            return model;
        }

        /**
         * This method returns true if the global configuration says we should
         * speak French.
         * 
         * The method name is bit awkward because global.jelly calls this method
         * to determine the initial state of the checkbox by the naming
         * convention.
         */
        public boolean getUseFrench() {
            return useFrench;
        }

        public RemoteSitez[] getRemoteSites() {

            Iterator<RemoteSitez> it = remoteSites.iterator();
            int size = 0;
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return remoteSites.toArray(new RemoteSitez[size]);
        }
    }
}
