package com.augurworks.alfred.server;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.stats.StatsTracker.Snapshot;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AlfredServiceImpl implements AlfredService {

    private static final Logger log = LoggerFactory.getLogger(AlfredServiceImpl.class);
    private AlfredPrefs prefs;
    private AlfredWrapper alfred;

    public void setPrefs(AlfredPrefs prefs) {
        this.prefs = prefs;
    }

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

    @Override
    public TrainStatus getStatus(String name) {
        TrainStatus trainStatus = alfred.getCurrentJobStatuses().get(name);
        if (trainStatus == null) {
            return TrainStatus.NOT_FOUND;
        }
        return trainStatus;
    }

    @Override
    public void cancelJob(String jobName) {
        alfred.cancelJob(jobName);
    }

    @Override
    public boolean isShutdown() {
        return alfred.isShutdown();
    }

    @Override
    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) {
        alfred.shutdownAndAwaitTermination(timeout, unit);
    }

    @Override
    public void shutdownImmediate() {
        alfred.shutdownNow();
    }

    @Override
    public void train(String name, String augtrain) {
        alfred.train(name, augtrain);
    }

    @Override
    public String getResult(String name) {
        Optional<RectNetFixed> result = null;
        try {
            result = alfred.getResultIfComplete(name);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting result for net " + name, e);
        }
        if (result == null || !result.isPresent()) {
            return "";
        } else {
            return RectNetFixed.getAugout(result.get());
        }
    }

    @Override
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
