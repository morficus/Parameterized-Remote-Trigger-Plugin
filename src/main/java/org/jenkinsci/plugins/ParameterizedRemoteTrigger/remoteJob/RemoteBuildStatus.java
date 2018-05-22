package org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob;

/**
 * The build status of a remote build.
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
