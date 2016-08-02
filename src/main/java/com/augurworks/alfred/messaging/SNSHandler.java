package com.augurworks.alfred.messaging;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.logging.LoggingUtils;
import com.augurworks.alfred.server.AlfredWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class SNSHandler implements RequestHandler<SNSEvent, TrainingMessage> {

    ObjectMapper mapper = new ObjectMapper();
    AmazonSQSClient sqsClient = new AmazonSQSClient();

    private static final String SQS_NAME_KEY = "sqsName";
    private final Integer TRAINING_BUFFER_SEC = 5;

    public TrainingMessage handleRequest(SNSEvent snsEvent, Context context) {
        try {
            TrainingMessage trainingMessage = mapper.readValue(snsEvent.getRecords().get(0).getSNS().getMessage(), TrainingMessage.class);

            LoggingUtils.addFluentAppender(trainingMessage, "AWS Lambda");

            RectNetFixed rectNetFixed = AlfredWrapper.trainStatic(trainingMessage, context.getRemainingTimeInMillis() - 1000 * TRAINING_BUFFER_SEC);
            TrainingMessage outputMessage = new TrainingMessage(rectNetFixed.getName(), rectNetFixed.getAugout(), rectNetFixed.getTrainingStats());
            sqsClient.sendMessage(trainingMessage.getMetadata().get(SQS_NAME_KEY), mapper.writeValueAsString(outputMessage));
            return outputMessage;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}