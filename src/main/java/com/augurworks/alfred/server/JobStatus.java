package com.augurworks.alfred.server;

public class JobStatus {

    private final String jobName;
    private final long elapsedMillis;
    private final TrainStatus status;

    public JobStatus(String jobName, long elapsedMillis, TrainStatus status) {
        this.jobName = jobName;
        this.elapsedMillis = elapsedMillis;
        this.status = status;
    }
}
