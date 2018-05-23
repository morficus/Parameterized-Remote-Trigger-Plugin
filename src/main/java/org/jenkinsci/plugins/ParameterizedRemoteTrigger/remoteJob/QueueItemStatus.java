package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

/**
 * This class implements the status of an item while it is on the queue.
 * <p>
 * See {@link hudson.model.Queue}.
 *
 *<pre>{@code
 * (enter Queue) --> WAITING --+--> BLOCKED
                          |          ^
                          |          |
                          |          v
                          +-------> BUILDABLE ---> PENDING ---> LEFT --> EXECUTED
                                       ^              |
                                       |              |
                                       +---(rarely)---+
 *}</pre>
 *
 * <p>
 * When the remote build is triggered, the remote job enters the queue (waiting list)
 * and the queue item status is WAITING.
 * <p>
 * After that, if there is another build already in progress, the queue item status changes to BLOCKED.
 * <p>
 * On the contrary, if there is not another build in progress, the queue item status changes to BUILDABLE.
 * <p>
 * Once the queue item is buildable, it needs to wait for an available executor, and the status changes
 * to PENDING.
 * <p>
 * If the node disappears before the execution starts, the status moves back to BUILDABLE,
 * but this is not the normal case.
 * <p>
 * When there is an available executor and the execution starts, the queue item leaves the queue,
 * and the status changes to LEFT.
 * <p>
 * When the remote job leaves the queue, the build number and the build URL are available.
 * The build URL can be used to request information about the remote job while it is being executed.
 * <p>
 * Sometimes, we did face some issues, because the item left the queue but this properties where not available,
 * therefore the status EXECUTED was added.
 * <p>
 * When this properties are available, the queue item status changes to EXECUTED. This is the final status.
 * <p>
 * In addition, at any status, an item can be removed from the queue, in this case an AbortException is thrown.
 *
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