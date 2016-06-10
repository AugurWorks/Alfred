package com.augurworks.alfred.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfig {

    private final Connection connection;

    @Autowired
    public RabbitMQConfig() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        this.connection = factory.newConnection();
    }

    @Bean
    public Connection rabbitMQConnection() {
        return this.connection;
    }
}
