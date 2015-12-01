package com.augurworks.alfred.server;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import com.augurworks.alfred.Net;
import com.augurworks.alfred.Net.NetType;
import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.util.TimeUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AlfredDirectoryListener extends FileAlterationListenerAdaptor {

    public enum JobStatus {
        SUBMITTED_NOT_STARTED,
        IN_PROGRESS
    }

    private final ExecutorService exec;
    private AtomicInteger jobsSubmitted = new AtomicInteger();
    private AtomicInteger jobsCompleted = new AtomicInteger();
    private AtomicInteger jobsInProgress = new AtomicInteger();
    private final Map<String, JobStatus> jobStatusByFileName;
    private final Map<String, Future<?>> futuresByFileName;
    private final int timeoutSeconds;
    private final Semaphore semaphore;
    private final ScaleFunctionType sfType;

    public AlfredDirectoryListener(int numThreads, int timeoutSeconds, ScaleFunctionType sfType) {
        this.exec = Executors.newCachedThreadPool();
        this.semaphore = new Semaphore(numThreads);
        this.timeoutSeconds = timeoutSeconds;
        this.sfType = sfType;
        this.jobStatusByFileName = Maps.newConcurrentMap();
        this.futuresByFileName = Maps.newConcurrentMap();
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

    public void shutdownNow() {
        exec.shutdownNow();
    }

    public Map<String, JobStatus> getCurrentJobStatuses() {
        return ImmutableMap.copyOf(jobStatusByFileName);
    }

    public String getCurrentJobStatusesPretty() {
        StringBuilder sb = new StringBuilder("Current job statuses: \n");
        for (Map.Entry<String, JobStatus> entry : getCurrentJobStatuses().entrySet()) {
            sb.append("\t").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) {
        exec.shutdown();
        try {
            exec.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while terminating. Will shutdown now.");
            throw new IllegalStateException("Interrupted while terminating. Will shutdown now.");
        }
    }

    public boolean isShutdown() {
        return exec.isTerminated();
    }

    @Override
    public void onFileCreate(File changedFile) {
        try {
            onFileCreateUnsafe(changedFile);
        } catch (Throwable t) {
            System.err.println("Error thrown on file create");
            t.printStackTrace();
        }
    }

    private void onFileCreateUnsafe(File changedFile) {
        System.out.println("File created " + changedFile);
        NetType netType = Net.NetType.fromFile(changedFile.getName());
        if (netType == NetType.TRAIN) {
            Callable<Void> trainCallable = getTrainCallable(changedFile);
            Future<Void> future = exec.submit(trainCallable);
            futuresByFileName.put(changedFile.getName(), future);
        }
        updateFuturesMap();
    }

    private void updateFuturesMap() {
        // this doesn't strictly have to be kept up to date
        // all the time, but we should keep it from growing too huge
        List<String> toRemove = Lists.newArrayList();
        for (Map.Entry<String, Future<?>> entry : futuresByFileName.entrySet()) {
            if (entry.getValue().isDone()) {
                toRemove.add(entry.getKey());
            }
        }
        for (String done : toRemove) {
            futuresByFileName.remove(done);
        }
    }

    public void cancelJob(String fileName) {
        Future<?> future = futuresByFileName.get(fileName);
        if (future != null) {
            System.out.println("Attemping to cancel job for file " + fileName);
            // job status will be updated in finally block of train callable
            future.cancel(true);
            futuresByFileName.remove(fileName);
        } else {
            System.err.println("Unable to find job with name " + fileName);
            System.err.println("Valid names are " + futuresByFileName.keySet());
        }
    }

    private Callable<Void> getTrainCallable(final File file) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String fullPath = file.getAbsolutePath();
                String name = file.getName();
                jobsSubmitted.incrementAndGet();
                jobStatusByFileName.put(name, JobStatus.SUBMITTED_NOT_STARTED);
                try {
                    semaphore.acquire();
                    jobsInProgress.incrementAndGet();
                    jobStatusByFileName.put(name, JobStatus.IN_PROGRESS);

                    System.out.println("Starting training for file " + name + " with time limit of " + timeoutSeconds + " seconds.");
                    long startTime = System.currentTimeMillis();
                    RectNetFixed net = RectNetFixed.trainFile(fullPath,
                                                              true,
                                                              fullPath + "." + NetType.SAVE.getSuffix().toLowerCase(),
                                                              false,
                                                              timeoutSeconds * 1000,
                                                              sfType);
                    System.out.println("Training complete for file " + net + " after " + TimeUtils.formatTimeSince(startTime));

                    System.out.println("Saving net for file " + name);
                    RectNetFixed.saveNet(fullPath + "." + NetType.SAVE.getSuffix().toLowerCase(), net);
                    System.out.println("Net saved for file " + name);

                    System.out.println("Writing augout file for " + name);
                    RectNetFixed.writeAugoutFile(fullPath + "." + NetType.AUGOUT.getSuffix().toLowerCase(), net);
                    System.out.println("Augout written for " + name);
                } catch (Throwable t) {
                    System.err.println("Exception caught during evaluation of " + name);
                    t.printStackTrace();
                } finally {
                    jobStatusByFileName.remove(name);
                    semaphore.release();
                    jobsInProgress.decrementAndGet();
                    jobsCompleted.incrementAndGet();
                }
                return null;
            }
        };
    }

}
