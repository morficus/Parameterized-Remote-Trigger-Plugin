package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

import java.io.Serializable;
import java.net.URL;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import hudson.AbortException;
import hudson.model.Result;

/**
 * This class contains information about the remote build.
 *
 *<pre>{@code
 * NOT_TRIGGERED ---+--->    QUEUED    --+-->    RUNNING    -----+----->         FINISHED
                             queueId           buildNumber                        result
                                                & buildURL              (ABORTED | UNSTABLE | FAILURE | SUCCESS)
 *}</pre>
 *
 * <p>
 * By default, the remote build status is NOT_TRIGGERED and the remote build result is NOT_BUILT.
 * <p>
 * When the remote build is triggered, the remote job enters the queue (waiting list)
 * and the status of the remote build changes to QUEUED. In this moment the queueId is available.
 * The queueId can be used to request information about the remote job while it is waiting to be executed.
 * <p>
 * When the remote job leaves the queue, the status changes to RUNNING. Then, the build number and the build URL
 * are available. The build URL can be used to request information about the remote job while it is being executed.
 * <p>
 * When the remote job is finished, the status changes to FINISHED. Then, the remote build result is available.
 *
 */
public class RemoteBuildInfo implements Serializable
{
    private static final long serialVersionUID = -5177308623227407314L;

    @CheckForNull
    private String queueId;

    @Nonnull
    private int buildNumber;

    @CheckForNull
    private URL buildURL;

    @Nonnull
    private RemoteBuildStatus status;

    @Nonnull
    private Result result;


    public RemoteBuildInfo()
    {
        status = RemoteBuildStatus.NOT_TRIGGERED;
        result = Result.NOT_BUILT;
    }

    @CheckForNull
    public String getQueueId() {
        return queueId;
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

    @Nonnull
    public RemoteBuildStatus getStatus()
    {
        return status;
    }

    @Nonnull
    public Result getResult()
    {
        return result;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
        this.status = RemoteBuildStatus.QUEUED;
    }

    public void setBuildData(@Nonnull int buildNumber, @Nullable URL buildURL) throws AbortException
    {
        if (buildURL == null) {
            throw new AbortException(String.format("Unexpected remote build status: %s", toString()));
        }
        this.buildNumber = buildNumber;
        this.buildURL = buildURL;
        this.status = RemoteBuildStatus.RUNNING;
    }

    public void setBuildStatus(RemoteBuildStatus status)
    {
        if (status == RemoteBuildStatus.FINISHED) {
            throw new IllegalArgumentException("It is not possible to set the status to finished without setting the build result. "
                    + "Please use BuildInfo(Result result) or BuildInfo(String result) in order to set the status to finished.");
        } else {
            this.status = status;
            this.result = Result.NOT_BUILT;
        }
    }

    public void setBuildResult(Result result)
    {
        this.status = RemoteBuildStatus.FINISHED;
        this.result = result;
    }

    public void setBuildResult(String result)
    {
        this.status = RemoteBuildStatus.FINISHED;
        this.result = Result.fromString(result);
    }

    @Nonnull
    @Override
    public String toString()
    {
        if (status == RemoteBuildStatus.FINISHED) return String.format("status=%s, result=%s", status.toString(), result.toString());
        else return String.format("status=%s", status.toString());
    }

    public boolean isNotTriggered() {
        return status == RemoteBuildStatus.NOT_TRIGGERED;
    }

    public boolean isQueued() {
        return status == RemoteBuildStatus.QUEUED;
    }

    public boolean isRunning() {
        return status == RemoteBuildStatus.RUNNING;
    }

    public boolean isFinished() {
        return status == RemoteBuildStatus.FINISHED;
    }
}
