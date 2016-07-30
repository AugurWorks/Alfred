package com.augurworks.alfred.messaging;

import com.augurworks.alfred.stats.TrainingStat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingMessage implements Serializable {

    private String netId;
    private String data;

    private List<TrainingStat> trainingStats = new ArrayList<>();
}
