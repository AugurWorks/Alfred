package com.augurworks.alfred.services

import com.augurworks.alfred.RectNetFixed
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType
import com.augurworks.alfred.server.AlfredPrefs
import com.augurworks.alfred.server.AlfredPrefsImpl
import com.augurworks.alfred.server.AlfredWrapper
import com.augurworks.alfred.server.TrainStatus
import com.augurworks.alfred.stats.StatsTracker.Snapshot
import com.google.common.base.Optional
import grails.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.PostConstruct
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@Transactional
class AlfredService {

    private static final Logger log = LoggerFactory.getLogger(AlfredService.class);
    private AlfredPrefs prefs;
    private AlfredWrapper alfred;

    public void setPrefs(AlfredPrefs prefs) {
        this.prefs = prefs;
    }

    @PostConstruct
    public void init() {
        prefs = new AlfredPrefsImpl();
        int threads = prefs.getNumThreads();
        int timeout = prefs.getTimeout();
        ScaleFunctionType scaleFunction = prefs.getScaleFunction();

        alfred = new AlfredWrapper(threads, timeout, scaleFunction);
    }

    public void destroy() {
        alfred.shutdownNow();
    }

    public TrainStatus getStatus(String name) {
        TrainStatus trainStatus = alfred.getCurrentJobStatuses().get(name);
        if (trainStatus == null) {
            return TrainStatus.NOT_FOUND;
        }
        return trainStatus;
    }

    public void cancelJob(String jobName) {
        alfred.cancelJob(jobName);
    }

    public boolean isShutdown() {
        return alfred.isShutdown();
    }

    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) {
        alfred.shutdownAndAwaitTermination(timeout, unit);
    }

    public void shutdownImmediate() {
        alfred.shutdownNow();
    }

    public void train(String name, String augtrain) {
        alfred.train(name, augtrain);
    }

    public String trainSynchronous(String netId, String augtrain) {
        return alfred.trainSynchronous(netId, augtrain).getAugout();
    }

    public String getResult(String name) {
        Optional<RectNetFixed> result = null;
        try {
            result = alfred.getResultIfComplete(name);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting result for net {}", name, e);
        }
        if (result == null || !result.isPresent()) {
            return "";
        } else {
            return result.get().getAugout();
        }
    }

    public String getStats() {
        List<Snapshot> snapshots = alfred.getStats();
        StringBuilder sb = new StringBuilder();
        sb.append("roundsTrained,millisElapsed,maxRounds,name,");
        sb.append("trainingConstant,isDone,completionReason,");
        sb.append("residual,cutoff\n");
        for (Snapshot snapshot : snapshots) {
            sb.append(snapshot.getRoundsTrained()).append(",");
            sb.append(snapshot.getMillisElapsed()).append(",");
            sb.append(snapshot.getMaxRounds()).append(",");
            sb.append(snapshot.getName()).append(",");
            sb.append(snapshot.getTrainingConstant()).append(",");
            sb.append(snapshot.isDone()).append(",");
            sb.append(snapshot.getCompletionReason()).append(",");
            sb.append(snapshot.getResidual()).append(",");
            sb.append(snapshot.getCutoff()).append("\n");
        }
        return sb.toString();
    }
}
