package com.augurworks.alfred.messaging;

import com.augurworks.alfred.config.RabbitMQConfig;
import com.augurworks.alfred.server.AlfredService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TrainingConsumer {

    Logger log = LoggerFactory.getLogger(TrainingConsumer.class);

    private final AlfredService alfredService;

    private Channel trainingChannel;
    private Channel resultChannel;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public TrainingConsumer(AlfredService alfredService, Channel trainingChannel, Channel resultChannel) {
        this.alfredService = alfredService;
        this.trainingChannel = trainingChannel;
        this.resultChannel = resultChannel;
    }

    @PostConstruct
    public void startConsumer() throws IOException {
        log.info("Starting training consumer");
        Consumer consumer = new DefaultConsumer(trainingChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                TrainingMessage message = mapper.readValue(body, TrainingMessage.class);
                processMessage(message);
            }
        };
        trainingChannel.basicConsume(RabbitMQConfig.TRAINING_CHANNEL, true, consumer);
    }

    private void processMessage(TrainingMessage message) {
        MDC.put("netId", message.getNetId());
        log.debug("Received an incoming training message");
        String result = alfredService.trainSynchronous(message.getNetId(), message.getData());
        sendResult(message.getNetId(), result);
    }

    private void sendResult(String netId, String result) {
        log.debug("Sending message for net {}", netId);
        TrainingMessage message = new TrainingMessage(netId, result);
        try {
            resultChannel.basicPublish("", RabbitMQConfig.RESULTS_CHANNEL, null, mapper.writeValueAsString(message).getBytes());
        } catch (IOException e) {
            log.error("An error occurred when publishing a message for net {}", netId, e);
        }
    }
}
