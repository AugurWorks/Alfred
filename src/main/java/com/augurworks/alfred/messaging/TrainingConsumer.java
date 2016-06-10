package com.augurworks.alfred.messaging;

import com.augurworks.alfred.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TrainingConsumer {

    Logger log = LoggerFactory.getLogger(TrainingConsumer.class);

    private Channel trainingChannel;
    private Channel resultChannel;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public TrainingConsumer(Channel trainingChannel, Channel resultChannel) {
        this.trainingChannel = trainingChannel;
        this.resultChannel = resultChannel;
    }

    @PostConstruct
    public void startConsumer() throws IOException {
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
        System.out.println(message);
    }
}
