package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

/**
 * If the remote Jenkins server uses the "Prevent Cross Site Request Forgery exploits" security option,
 * a CSRF protection token must be sent in the header of the request to trigger the remote job.
 * This token is called crumb.
 *
 */
public class JenkinsCrumb
{
    String headerId;
    String crumbValue;

    public JenkinsCrumb(String headerId, String crumbValue)
    {
        this.headerId = headerId;
        this.crumbValue = crumbValue;
    }

    public String getHeaderId()
    {
        return headerId;
    }

    public String getCrumbValue()
    {
        return crumbValue;
    }
}
