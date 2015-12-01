package com.augurworks.alfred;

public enum TrainingStopReason {
    OUT_OF_TIME("Ran out of time"),
    HIT_PERFORMANCE_CUTOFF("Hit performance cutoff"),
    HIT_TRAINING_LIMIT("Round limit reached"),
    ;

    private String explanation;

    private TrainingStopReason(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }
}
