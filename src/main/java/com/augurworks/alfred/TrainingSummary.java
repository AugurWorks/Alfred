package com.augurworks.alfred;

import lombok.Data;

@Data
public class TrainingSummary {

    private final String netId;
    private final Integer dataSets;
    private final Integer rowCount;
    private final Double learningConstant;
    private final TrainingStopReason stopReason;
    private final int secondsElapsed;
    private final int roundsTrained;
    private final double rmsError;
}
