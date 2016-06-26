package com.augurworks.alfred.domains

import com.augurworks.alfred.TrainingStopReason
import com.augurworks.alfred.TrainingSummary

class TrainingRun {

    String netId
    Integer dataSets
    Integer rowCount
    Double learningConstant
    TrainingStopReason stopReason
    Integer secondsElapsed
    Integer roundsTrained
    Double rmsError

    static constraints = {

    }

    static TrainingRun fromTrainingSummary(TrainingSummary trainingSummary) {
        return new TrainingRun(
            netId: trainingSummary.getNetId(),
            dataSets: trainingSummary.getDataSets(),
            rowCount: trainingSummary.getRowCount(),
            learningConstant: trainingSummary.getLearningConstant(),
            stopReason: trainingSummary.getStopReason(),
            secondsElapsed: trainingSummary.getSecondsElapsed(),
            roundsTrained: trainingSummary.getRoundsTrained(),
            rmsError: trainingSummary.getRmsError()
        )
    }
}
