package com.augurworks.alfred.messaging;

import com.augurworks.alfred.stats.TrainingStat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TrainingMessage implements Serializable {

    TrainingMessage(String netId, String data) {
        this.netId = netId;
        this.data = data;
    }

    private String netId;
    private String data;

    private List<TrainingStat> trainingStats = new ArrayList<>();
}
