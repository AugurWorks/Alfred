package com.augurworks.alfred;

public class TrainingSummary {

    private final TrainingStopReason stopReason;
    private final int secondsElapsed;
    private final int roundsTrained;
    private final double rmsError;

    public TrainingSummary(TrainingStopReason stopReason,
                           int secondsElapsed,
                           int roundsTrained,
                           double rmsError) {
        this.stopReason = stopReason;
        this.secondsElapsed = secondsElapsed;
        this.roundsTrained = roundsTrained;
        this.rmsError = rmsError;
    }

    public TrainingStopReason getStopReason() {
        return stopReason;
    }

    public int getSecondsElapsed() {
        return secondsElapsed;
    }

    public int getRoundsTrained() {
        return roundsTrained;
    }

    public double getRmsError() {
        return rmsError;
    }

}
