package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

/**
 * The status of one queue item while it is on the queue.
 * See {@link hudson.model.Queue}.
 */
public enum QueueItemStatus
{
    /**
     * If a queue item enters the queue (waiting list).
     */
    WAITING("WAITING"),

    /**
     * If another build is already in progress.
     */
    BLOCKED("BLOCKED"),

    /**
     * If there is not another build in progress.
     */
    BUILDABLE("BUILDABLE"),

    /**
     * If the queue item is waiting for an available executor.
     */
    PENDING("PENDING"),

    /**
     * If there is an available executor and no build is already in progress.
     */
    LEFT("LEFT"),

    /**
     * The queue item left the queue and the build information (build number and build URL) is available.
     */
    EXECUTED("EXECUTED"),

    /**
     * If the queue item was cancelled and therefore it will not be executed.
     */
    CANCELLED("CANCELLED");


    private final String id;


    private QueueItemStatus(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

}