package com.augurworks.alfred.stats;

import com.augurworks.alfred.TrainingStopReason;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TrainingStat implements Serializable {

    private final String netId;
    private final Integer dataSets;
    private final Double learningConstant;
    private final Integer rowCount;

    private Integer secondsElapsed;
    private Integer roundsTrained;
    private Double rmsError;
    private TrainingStopReason trainingStopReason;
    private TrainingStage trainingStage;

    private Date dateCreated = new Date();
}