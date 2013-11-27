package org.jenkinsci.plugins.ParameterizedRemoteTrigger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.RemoteBuilder.DescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Hudson.MasterComputer;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

public class RemoteSitez extends AbstractDescribableImpl<RemoteSitez> {

	private  final String hostname;
	private final String displayName;
	
	@DataBoundConstructor
	public RemoteSitez(String hostname, String displayName) {
		this.hostname = hostname;
		this.displayName = displayName;
	}
	
	//XXX: need to add a bunch of stuff around URL validation (take it all from RemoteBuilder.java)
	
	public String getDisplayName() {
		return displayName;
	}

	public String getHostname() {
		return hostname;
	}
	
	public String getName() {
		return "some name";
	}

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	
    
	
	@Extension
	public static class DescriptorImpl extends Descriptor<RemoteSitez> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }
        
		public String getDisplayName() {
			return "";
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
	}
	
}
