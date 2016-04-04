package com.augurworks.alfred.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.util.TimeUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class AlfredDirectoryListener {

    private final ExecutorService exec;
    private final Map<String, TrainStatus> jobStatusByFileName;
    private final Map<String, Future<?>> futuresByFileName;
    private final int timeoutSeconds;
    private final Semaphore semaphore;
    private final ScaleFunctionType sfType;
    private final UsageTracker usage = new UsageTracker();
    private AlfredPrefs prefs;

    public AlfredDirectoryListener(int numThreads, int timeoutSeconds, ScaleFunctionType sfType) {
        this.exec = Executors.newCachedThreadPool();
        this.semaphore = new Semaphore(numThreads);
        this.timeoutSeconds = timeoutSeconds;
        this.sfType = sfType;
        this.jobStatusByFileName = Maps.newConcurrentMap();
        this.futuresByFileName = Maps.newConcurrentMap();
        this.prefs = new AlfredPrefsImpl();
    }

    public void shutdownNow() {
        exec.shutdownNow();
    }

    public Map<String, TrainStatus> getCurrentJobStatuses() {
        return ImmutableMap.copyOf(jobStatusByFileName);
    }

    public String getCurrentJobStatusesPretty() {
        StringBuilder sb = new StringBuilder("Current job statuses: \n");
        for (Map.Entry<String, TrainStatus> entry : getCurrentJobStatuses().entrySet()) {
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

    public void train(String name, String augtrain) {
        Callable<Void> trainCallable = getTrainCallable(name, augtrain);
        Future<Void> future = exec.submit(trainCallable);
        futuresByFileName.put(name, future);
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

    public String printStatus() {
        StringBuilder sb = new StringBuilder("Server Status:\n");
        sb.append("  Jobs in progress : " + usage.getJobsInProgress()).append("\n");
        sb.append("  Jobs submitted   : " + usage.getJobsSubmitted()).append("\n");
        sb.append("  Jobs completed   : " + usage.getJobsCompleted()).append("\n");
        sb.append(getCurrentJobStatusesPretty());
        return sb.toString();
    }

    private Callable<Void> getTrainCallable(final String name, final String augtrain) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                usage.incrementJobsSubmitted();
                jobStatusByFileName.put(name, TrainStatus.SUBMITTED);
                PrintWriter logLocation = null;
                try {
                    semaphore.acquire();
                    usage.incrementJobsInProgress();
                    jobStatusByFileName.put(name, TrainStatus.IN_PROGRESS);
                    logLocation = getLogLocation(name);

                    LoggingHelper.out("Starting training for file " + name + " with time limit of " + timeoutSeconds + " seconds.", logLocation);
                    long startTime = System.currentTimeMillis();
                    List<String> lines = Splitter.on("\n").splitToList(augtrain);
                    RectNetFixed net = RectNetFixed.trainFile(name,
                                                              lines,
                                                              prefs.getVerbose(),
                                                              false,
                                                              timeoutSeconds * 1000,
                                                              sfType,
                                                              5,
                                                              logLocation);
                    LoggingHelper.out("Training complete for file " + net + " after " + TimeUtils.formatTimeSince(startTime), logLocation);
                } catch (Exception t) {
                    System.err.println("Exception caught during evaluation of " + name);
                    t.printStackTrace();
                } finally {
                    LoggingHelper.flushAndCloseQuietly(logLocation);
                    jobStatusByFileName.remove(name);
                    semaphore.release();
                    usage.incrementJobsInProgress();
                    usage.incrementJobsCompleted();
                }
                return null;
            }
        };
    }

    private PrintWriter getLogLocation(String name) {
        try {
            new File("logs").mkdir();
            return new PrintWriter(new BufferedWriter(new FileWriter(new File("logs/" + name + ".log"))));
        } catch (IOException e) {
            System.err.println("Unable to create log file for " + name);
            return null;
        }
    }

}
