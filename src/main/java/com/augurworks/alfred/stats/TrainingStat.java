package com.augurworks.alfred.stats;

import com.augurworks.alfred.TrainingStopReason;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TrainingStat implements Serializable {

    private final String netId;
    private final Integer dataSets;
    private final Integer rowCount;
    private final Double learningConstant;
    private final Integer secondsElapsed;
    private final Integer roundsTrained;
    private final Double rmsError;
    private final TrainingStopReason trainingStopReason;
    private final TrainingStage trainingStage;

    private Date dateCreated = new Date();
}