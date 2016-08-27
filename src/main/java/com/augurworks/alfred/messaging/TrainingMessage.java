package com.augurworks.alfred.messaging;

import com.augurworks.alfred.stats.TrainingStat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class TrainingMessage implements Serializable {

    TrainingMessage(String netId, String data, List<TrainingStat> trainingStats) {
        this.netId = netId;
        this.data = data;
        this.trainingStats = trainingStats;
    }

    private String netId;
    private String data;

    private Map<String, String> metadata;
    private List<TrainingStat> trainingStats;
}
