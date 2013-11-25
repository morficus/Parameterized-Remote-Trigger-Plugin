package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

public class RemoteSite {

	private  final String url;
	private final String displayName;
	
	public RemoteSite(String url, String displayName) {
		this.url = url;
		this.displayName = displayName;
	}
	
}
