package com.augurworks.alfred.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.stats.StatsTracker;
import com.augurworks.alfred.stats.StatsTracker.Snapshot;
import com.augurworks.alfred.stats.UsageTracker;
import com.augurworks.alfred.util.TimeUtils;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class AlfredWrapper {

    Logger log = LoggerFactory.getLogger(AlfredWrapper.class);

    private final ExecutorService exec;
    private final Map<String, TrainStatus> jobStatusByFileName;
    private final Map<String, Future<RectNetFixed>> futuresByFileName;
    private final int timeoutSeconds;
    private final Semaphore semaphore;
    private final ScaleFunctionType sfType;
    private final UsageTracker usage = new UsageTracker();
    private final StatsTracker stats;
    private AlfredPrefs prefs;

    public AlfredWrapper(int numThreads, int timeoutSeconds, ScaleFunctionType sfType) {
        this.exec = Executors.newCachedThreadPool();
        this.semaphore = new Semaphore(numThreads);
        this.timeoutSeconds = timeoutSeconds;
        this.sfType = sfType;
        this.jobStatusByFileName = Maps.newConcurrentMap();
        this.futuresByFileName = Maps.newConcurrentMap();
        this.prefs = new AlfredPrefsImpl();
        this.stats = new StatsTracker(prefs.getStatsHistory());
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
            log.error("Interrupted while terminating. Will shutdown now.");
            throw new IllegalStateException("Interrupted while terminating. Will shutdown now.");
        }
    }

    public boolean isShutdown() {
        return exec.isTerminated();
    }

    public void train(String name, String augtrain) {
        Callable<RectNetFixed> trainCallable = getTrainCallable(name, augtrain);
        Future<RectNetFixed> future = exec.submit(trainCallable);
        futuresByFileName.put(name, future);
    }

    public Optional<RectNetFixed> getResultIfComplete(String name) throws InterruptedException, ExecutionException {
        Future<RectNetFixed> future = futuresByFileName.get(name);
        if (future == null) {
            return null;
        }
        if (future.isDone()) {
            return Optional.of(future.get());
        } else {
            return Optional.absent();
        }
    }

    public void cancelJob(String fileName) {
        Future<?> future = futuresByFileName.get(fileName);
        if (future != null) {
            log.info("Attempting to cancel job for file {}", fileName);
            // job status will be updated in finally block of train callable
            future.cancel(true);
            futuresByFileName.remove(fileName);
        } else {
            log.error("Unable to find job with name {}", fileName);
            log.error("Valid names are {}", futuresByFileName.keySet());
        }
    }

    public String printStatus() {
        String sb = "Server Status:\n" + "  Jobs in progress : " + usage.getJobsInProgress() + "\n" +
                "  Jobs submitted   : " + usage.getJobsSubmitted() + "\n" +
                "  Jobs completed   : " + usage.getJobsCompleted() + "\n" +
                getCurrentJobStatusesPretty();
        return sb;
    }

    private Callable<RectNetFixed> getTrainCallable(final String name, final String augtrain) {
        return () -> trainSynchronous(name, augtrain);
    }

    public RectNetFixed trainSynchronous(String netId, String augtrain) {
        MDC.put("netId", netId);
        MDC.put("trainingTimeLimitSec", timeoutSeconds);
        MDC.put("scaleFunctionType", sfType.name());

        usage.incrementJobsSubmitted();
        jobStatusByFileName.put(netId, TrainStatus.SUBMITTED);
        try {
            semaphore.acquire();
            usage.incrementJobsInProgress();
            jobStatusByFileName.put(netId, TrainStatus.IN_PROGRESS);

            log.info("Starting training for file {} with time limit of {} seconds.", netId, timeoutSeconds);
            List<String> lines = Splitter.on("\n").splitToList(augtrain);
            RectNetFixed net = new RectNetFixed().train(netId, lines, prefs.getVerbose(), timeoutSeconds * 1000, sfType, 5);
            jobStatusByFileName.put(netId, TrainStatus.COMPLETE);
            return net;
        } catch (Exception t) {
            log.error("Exception caught during evaluation of " + netId, t);
        } finally {
            semaphore.release();
            usage.incrementJobsInProgress();
            usage.incrementJobsCompleted();
        }
        return null;
    }

    public List<Snapshot> getStats() {
        return stats.getStats();
    }
}
