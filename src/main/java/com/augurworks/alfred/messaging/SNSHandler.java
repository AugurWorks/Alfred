package com.augurworks.alfred.messaging;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.server.AlfredWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class SNSHandler implements RequestHandler<SNSEvent, TrainingMessage> {

    public TrainingMessage handleRequest(SNSEvent snsEvent, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            TrainingMessage trainingMessage = mapper.readValue(snsEvent.getRecords().get(0).getSNS().getMessage(), TrainingMessage.class);
            RectNetFixed rectNetFixed = AlfredWrapper.trainStatic(trainingMessage.getNetId(), trainingMessage.getData());
            return new TrainingMessage(rectNetFixed.getName(), rectNetFixed.getAugout(), rectNetFixed.getTrainingStats());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}