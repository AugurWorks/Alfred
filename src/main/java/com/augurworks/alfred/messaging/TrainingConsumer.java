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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TrainingConsumer {

    Logger log = LoggerFactory.getLogger(TrainingConsumer.class);

    private final AlfredService alfredService;

    private Channel trainingChannel;
    private Channel resultChannel;

    private String rabbitMQEnv;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public TrainingConsumer(AlfredService alfredService, Channel trainingChannel, Channel resultChannel, @Value("${rabbitmq.env}") String rabbitMQEnv) {
        this.alfredService = alfredService;
        this.trainingChannel = trainingChannel;
        this.resultChannel = resultChannel;
        this.rabbitMQEnv = rabbitMQEnv;
    }

    @PostConstruct
    public void init() {
        if (trainingChannel == null) {
            log.warn("Training channel not open, skipping training consumer init");
        } else {
            startConsumer();
        }
    }

    public void startConsumer() {
        log.info("Starting training consumer");
        Consumer consumer = new DefaultConsumer(trainingChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                TrainingMessage message = mapper.readValue(body, TrainingMessage.class);
                processMessage(message);
                trainingChannel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        try {
            trainingChannel.basicConsume(RabbitMQConfig.getTrainingChannelName(rabbitMQEnv), false, consumer);
        } catch (IOException e) {
            log.error("Training consumer failed to initialize", e);
        }
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
            resultChannel.basicPublish("", RabbitMQConfig.getResultsChannelName(rabbitMQEnv), null, mapper.writeValueAsString(message).getBytes());
        } catch (IOException e) {
            log.error("An error occurred when publishing a message for net {}", netId, e);
        }
    }
}