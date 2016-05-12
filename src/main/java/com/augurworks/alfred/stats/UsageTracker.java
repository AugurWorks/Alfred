package com.augurworks.alfred.stats;

import java.util.concurrent.atomic.AtomicInteger;

public class UsageTracker {
    private final AtomicInteger jobsSubmitted = new AtomicInteger();
    private final AtomicInteger jobsCompleted = new AtomicInteger();
    private final AtomicInteger jobsInProgress = new AtomicInteger();

    public int incrementJobsSubmitted() {
        return jobsSubmitted.incrementAndGet();
    }

    public int incrementJobsCompleted() {
        return jobsCompleted.incrementAndGet();
    }

    public int incrementJobsInProgress() {
        return jobsInProgress.incrementAndGet();
    }

    public int getJobsSubmitted() {
        return jobsSubmitted.get();
    }

    public int getJobsCompleted() {
        return jobsCompleted.get();
    }

    public int getJobsInProgress() {
        return jobsInProgress.get();
    }
}
