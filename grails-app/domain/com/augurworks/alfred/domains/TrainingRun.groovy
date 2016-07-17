package com.augurworks.alfred.domains

import com.augurworks.alfred.TrainingStage
import com.augurworks.alfred.TrainingStopReason

class TrainingRun {

    String netId
    Integer dataSets
    Integer rowCount
    Double learningConstant
    Integer secondsElapsed
    Integer roundsTrained
    Double rmsError
    TrainingStopReason trainingStopReason
    TrainingStage trainingStage

    Date dateCreated

    static constraints = {
        trainingStopReason nullable: true
    }
}
