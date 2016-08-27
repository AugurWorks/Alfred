package com.augurworks.alfred.messaging;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.config.RabbitMQConfig;
import com.augurworks.alfred.logging.LoggingUtils;
import com.augurworks.alfred.server.AlfredWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class TrainingConsumer {

    Logger log = LoggerFactory.getLogger(TrainingConsumer.class);

    private Channel trainingChannel;
    private Channel resultChannel;

    private String rabbitMQEnv;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public TrainingConsumer(Channel trainingChannel, Channel resultChannel, @Value("${rabbitmq.env}") String rabbitMQEnv) {
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

    private void processMessage(TrainingMessage message) throws UnknownHostException {
        LoggingUtils.addFluentAppender(message, InetAddress.getLocalHost().getHostName());

        RectNetFixed rectNetFixed = AlfredWrapper.trainStatic(message, 3600000);
        sendResult(rectNetFixed);
    }

    private void sendResult(RectNetFixed rectNetFixed) {
        log.debug("Sending message for net {}", rectNetFixed.getName());
        TrainingMessage message = new TrainingMessage(rectNetFixed.getName(), rectNetFixed.getAugout(), rectNetFixed.getTrainingStats());
        try {
            resultChannel.basicPublish("", RabbitMQConfig.getResultsChannelName(rabbitMQEnv), null, mapper.writeValueAsString(message).getBytes());
        } catch (IOException e) {
            log.error("An error occurred when publishing a message for net {}", rectNetFixed.getName(), e);
        }
    }
}
