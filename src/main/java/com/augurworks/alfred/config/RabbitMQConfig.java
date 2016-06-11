package com.augurworks.alfred.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfig {

    Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    @Value("${rabbitmq.hostname}")
    private String hostname;

    @Value("${rabbitmq.port}")
    private Integer port;

    public static final String TRAINING_CHANNEL = "nets.training";
    public static final String RESULTS_CHANNEL = "nets.results";

    @Bean
    public Channel trainingChannel() {
        log.info("Creating training RabbitMQ channel");
        try {
            Channel channel = getConnection().createChannel();
            channel.queueDeclare(TRAINING_CHANNEL, false, false, false, null);
            return channel;
        } catch (IOException | TimeoutException e) {
            log.error("Could not connect to RabbitMQ", e);
            return null;
        }
    }

    @Bean
    public Channel resultChannel(){
        log.info("Creating results RabbitMQ channel");
        try {
            Channel channel = getConnection().createChannel();
            channel.queueDeclare(RESULTS_CHANNEL, false, false, false, null);
            return channel;
        } catch (IOException | TimeoutException e) {
            log.error("Could not connect to RabbitMQ", e);
            return null;
        }
    }

    private Connection getConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setHost(hostname);
        factory.setPort(port);
        return factory.newConnection();
    }
}
