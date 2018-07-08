package org.jenkinsci.plugins.ParameterizedRemoteTrigger.exceptions;

import java.io.IOException;
import java.net.URL;

public class UrlNotFoundException extends IOException {
    
	private static final long serialVersionUID = -8787613112499246042L;
	private URL url;

    public UrlNotFoundException(URL url)
    {
        this.url = url;
    }

    @Override
    public String getMessage()
    {
        return "Server returned 404 - URL not found for this request: " + url;
    }

}
