package org.jenkinsci.plugins.ParameterizedRemoteTrigger;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class RemoteBuilder extends Builder {

    private final String token;
    private final URL hostname;
    private final String job;
    private final String parameters;
    private static String paramerizedBuildUrl = "/buildWithParameters";
    private static String normalBuildUrl = "/build";
    
    private List<String> parameterList;
    
    
    
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RemoteBuilder(String token, String hostname, String parameters, String job) throws MalformedURLException {
    	
    	this.token = token;
        this.hostname = new URL(hostname);
    	this.parameters = parameters;
    	this.job = job;
    	
    	//split the parameter-string into an array based on the new-line character
    	String[] params = parameters.split("\n");
    	
    	//convert the String array into a List of Strings, and remove any empty entries
    	this.parameterList = new ArrayList<String>(Arrays.asList(params));
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
    	this.parameterList.removeAll(Arrays.asList(null,""));
    }
    
    /**
     * Strip out any comments (lines that start with a #) from the parameterList
     */
    private void removeCommentsFromParameters()
    {
    	List<String> itemsToRemove = new ArrayList<String>();
    	
    	Integer numberOfParams = this.parameterList.size();
    	
    	//identify all the items that are marked as a comment
    	for(int i = 0; i < numberOfParams; i++) {
    		String currentItem = this.parameterList.get(i);
    		
    		if( currentItem.indexOf("#") == 0 )
    		{
    			itemsToRemove.add(currentItem);
    		}
    	}
    	
    	this.parameterList.removeAll(itemsToRemove);
    	
    }

    /**
     * Return the parameterList in an encoded query-string
     * @return URL-encoded string
     */
    public String buildUrlQueryString() {
    	String queryParams = StringUtils.join(this.parameterList, "&");
    	
    	//TODO: some URL encoding would be nice
    	
    	/*
    	try {
    		queryParams = URLEncoder.encode(queryParams, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		*/
    	
    	return queryParams;
    }

    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	//TODO: this should probably be done in a helper function
    	String myTriggerURLString = this.getHostname() + "/job/" + this.getJob() + this.paramerizedBuildUrl + "?" + "token=" + this.getToken() + "&" + this.buildUrlQueryString();
        
        listener.getLogger().println("Token: "+ this.getToken() );
    	listener.getLogger().println("URL: "+ this.getHostname() );
    	listener.getLogger().println("Remote Job: "+ this.getJob() );
    	listener.getLogger().println("Parameters: "+ this.parameterList.toString() );
    	listener.getLogger().println("Fully Built URL: " + myTriggerURLString);
    	
    	HttpURLConnection connection = null;
    	
    	try {
    		URL triggerUrl = new URL(myTriggerURLString);
        	connection = (HttpURLConnection)triggerUrl.openConnection();
        	
    		connection.setDoInput(true);
    		//connection.setRequestProperty("Accept", "application/json");
    		connection.setRequestMethod("POST");
    		connection.connect();
    		
    		
    		//connection.setDoOutput(true);

    		
    		//TODO: right now this just "fires and forgets". will need to poll the remote server to check for success or failure
    		//http://jenkins.local/job/test/lastBuild/api/json
    		
    		/*
    		InputStream is = connection.getInputStream();
    	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	      	String line;
	      	StringBuffer response = new StringBuffer();
	      	 
	      	while((line = rd.readLine()) != null) {
	        	System.out.println(line);
    	        
	      	}
	      	
	      	rd.close();
    	      */
    		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
        return true;
    }

    //Getters
    public URL getHostname() {
		return this.hostname;
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
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
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
            // Indicates that this builder can be used with all kinds of project types 
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
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * Validates the given hostname to see that it's well-formed, and is reachable.
         * 
         * @param hostname Remote hostname to be validated
         * @return FormValidation object
         */
        public FormValidation doValidateHostname(@QueryParameter String hostname) {
        	
        	URL host = null;
        	
        	//check if we have a valid, well-formed URL
    		try {
    			host = new URL(hostname);
    			URI uri = host.toURI();
    		}catch(Exception e) {
    			return FormValidation.error("Malformed URL (" + hostname + "), please double-check your hostname");
    		}
    		
    		//check that the host is reachable
    		try {
    			HttpURLConnection connection = (HttpURLConnection)host.openConnection();
    			connection.connect();
    		}catch(Exception e) {
    			return FormValidation.error("Unable to connect to remote Jenkins: " + hostname);
    		}
    		
    		return FormValidation.okWithMarkup("Hostname looks good");
        }
        
        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseFrench() {
            return useFrench;
        }
    }
}

