package com.augurworks.alfred.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class TrainingMessage implements Serializable {

    TrainingMessage() { }

    private String netId;
    private String data;
}
