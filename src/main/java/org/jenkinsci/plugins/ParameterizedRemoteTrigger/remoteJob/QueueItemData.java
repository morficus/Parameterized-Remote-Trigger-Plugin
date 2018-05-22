package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Contains information about the job <b>while is waiting on the queue</b>.
 *
 */
public class QueueItemData
{
    @Nonnull
    private final JSONObject queueResponse;

    @Nonnull
    private RemoteBuildQueueStatus status;

    @Nonnull
    private int buildNumber;

    @CheckForNull
    private URL buildURL;


    public QueueItemData(@Nonnull BuildContext context, @Nonnull JSONObject queueResponse) throws MalformedURLException
    {
        this.queueResponse = queueResponse;
        this.status = RemoteBuildQueueStatus.QUEUED;
        setQueueItemData(context);
    }

    public boolean isBlocked()
    {
        return queueResponse.getBoolean("blocked");
    }

    public boolean isBuildable()
    {
        return queueResponse.getBoolean("buildable");
    }

    public boolean isPending()
    {
        return getOptionalBoolean("pending");
    }

    public boolean isCancelled()
    {
        return getOptionalBoolean("cancelled");
    }

    public String getWhy()
    {
        return queueResponse.getString("why");
    }

    public boolean isExecutable()
    {
        return (!isBlocked() && !isBuildable() && !isPending() && !isCancelled());
    }

    public boolean isExecuted()
    {
        return status == RemoteBuildQueueStatus.EXECUTED;
    }

    public RemoteBuildQueueStatus getQueueStatus() {
        return status;
    }

    @Nonnull
    public int getBuildNumber()
    {
        return buildNumber;
    }

    @CheckForNull
    public URL getBuildURL()
    {
        return buildURL;
    }

    /**
     * When a queue item is <b>executable</b>, the build number and the build URL
     * of the remote job are available in the queue item data.
     *
     * @param context
     *            the context of this Builder/BuildStep.
     * @throws MalformedURLException
     *            if there is an error creating the build URL.
     */
    private void setQueueItemData(@Nonnull BuildContext context) throws MalformedURLException
    {
        if (isExecutable()) {
            try {
                JSONObject remoteJobInfo = queueResponse.getJSONObject("executable");
                if (remoteJobInfo != null) {
                    try {
                        buildNumber = remoteJobInfo.getInt("number");
                    } catch (JSONException e) {
                        context.logger.println("The attribute \"number\" was not found. Unexpected response: " + queueResponse.toString());
                    }
                    try {
                        buildURL = new URL(remoteJobInfo.getString("url"));
                    } catch (JSONException e) {
                        context.logger.println("The attribute \"url\" was not found. Unexpected response: " + queueResponse.toString());
                    }
                }
            } catch (JSONException e) {
                context.logger.println("The attribute \"executable\" was not found. Unexpected response: " + queueResponse.toString());
            }
            if (buildNumber != -1 && buildURL != null) status = RemoteBuildQueueStatus.EXECUTED;
        }
    }

    private boolean getOptionalBoolean(String attribute)
    {
        if (queueResponse.containsKey(attribute))
            return queueResponse.getBoolean(attribute);
        else return false;
    }
}
