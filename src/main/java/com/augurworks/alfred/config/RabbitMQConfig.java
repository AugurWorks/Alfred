package com.augurworks.alfred.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfig {

    public static final String TRAINING_CHANNEL = "nets.training";
    public static final String RESULTS_CHANNEL = "nets.results";

    private final Connection connection;

    @Autowired
    public RabbitMQConfig() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        this.connection = factory.newConnection();
    }

    @Bean
    public Channel trainingChannel() throws IOException {
        Channel channel = this.connection.createChannel();
        channel.queueDeclare(TRAINING_CHANNEL, false, false, false, null);
        return channel;
    }

    @Bean
    public Channel resultsChannel() throws IOException {
        Channel channel = this.connection.createChannel();
        channel.queueDeclare(RESULTS_CHANNEL, false, false, false, null);
        return channel;
    }
}
