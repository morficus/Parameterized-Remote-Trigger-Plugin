package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

import java.io.Serializable;
import java.net.URL;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import hudson.AbortException;
import hudson.model.Result;

/**
 * The remote build info contains the queue id and the queue status of the remote build,
 * while it enters the queue, and the remote job build number, build url, build status and build result,
 * when it leaves the queue.
 *
 */
public class RemoteBuildInfo implements Serializable
{
    private static final long serialVersionUID = -5177308623227407314L;

    @CheckForNull
    private String queueId;

    @Nonnull
    private RemoteBuildQueueStatus queueStatus;

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
        queueId = null;
        queueStatus = RemoteBuildQueueStatus.NOT_QUEUED;
        status = RemoteBuildStatus.NOT_STARTED;
        result = Result.NOT_BUILT;
    }

    @CheckForNull
    public String getQueueId() {
        return queueId;
    }

    @Nonnull
    public RemoteBuildQueueStatus getQueueStatus()
    {
        return queueStatus;
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
        this.queueStatus = RemoteBuildQueueStatus.QUEUED;
    }

    public void setBuildData(@Nonnull int buildNumber, @Nullable URL buildURL) throws AbortException
    {
        if (buildURL == null) {
            throw new AbortException(String.format("Unexpected remote build status: %s", toString()));
        }
        this.buildNumber = buildNumber;
        this.buildURL = buildURL;
        this.queueStatus = RemoteBuildQueueStatus.EXECUTED;
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
        else return String.format("queueStatus=%s, status=%s", queueStatus.toString(), status.toString());
    }

    public boolean isNotQueued() {
        return queueStatus == RemoteBuildQueueStatus.NOT_QUEUED;
    }

    public boolean isQueued() {
        return queueStatus == RemoteBuildQueueStatus.QUEUED;
    }

    public boolean isNotStarted() {
        return status == RemoteBuildStatus.NOT_STARTED;
    }

    public boolean isRunning() {
        return status == RemoteBuildStatus.RUNNING;
    }

    public boolean isFinished() {
        return status == RemoteBuildStatus.FINISHED;
    }
}
