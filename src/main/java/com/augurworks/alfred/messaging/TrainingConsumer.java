package com.augurworks.alfred.messaging;

import com.augurworks.alfred.config.RabbitMQConfig;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TrainingConsumer {

    private Channel trainingChannel;

    @Autowired
    public TrainingConsumer(Channel trainingChannel) {
        this.trainingChannel = trainingChannel;
    }

    @PostConstruct
    public void startConsumer() throws IOException {
        Consumer consumer = new DefaultConsumer(trainingChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                processMessage(message);
            }
        };
        trainingChannel.basicConsume(RabbitMQConfig.TRAINING_CHANNEL, true, consumer);
    }

    private void processMessage(String message) {
        System.out.println(message);
    }
}
