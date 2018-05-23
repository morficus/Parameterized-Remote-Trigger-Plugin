package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

/**
 * This class implements the build status of a remote build.
 *
 *<pre>{@code
 * NOT_TRIGGERED --+--> QUEUED --+--> RUNNING --+--> FINISHED
                          |                              |
                          |                              |
                          +-------> Cancelled <----------+
 *}</pre>
 *
 * <p>
 * By default, the remote build status is NOT_TRIGGERED.
 * <p>
 * When the remote build is triggered, the remote job enters the queue (waiting list)
 * and the status of the remote build changes to QUEUED.
 * <p>
 * When the remote job leaves the queue, the status changes to RUNNING.
 * <p>
 * When the remote job is finished, the status changes to FINISHED. This is the final status.
 *
 * In addition, at the status QUEUED and FINISHED, a remote build can be cancelled,
 * in this case an AbortException is thrown.
 */
public enum RemoteBuildStatus
{
    /**
     * The remote job was not triggered and it did not enter the queue.
     */
    NOT_TRIGGERED("NOT_TRIGGERED"),

    /**
     * The remote job was triggered and it did enter the queue.
     */
    QUEUED("QUEUED"),

    /**
     * The remote job left the queue and it is running currently.
     */
    RUNNING("RUNNING"),

    /**
     * The remote build is finished.
     */
    FINISHED("FINISHED");


    private final String id;


    private RemoteBuildStatus(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

}
