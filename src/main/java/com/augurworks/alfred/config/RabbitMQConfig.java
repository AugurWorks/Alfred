package com.augurworks.alfred.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfig {

    private final Connection connection;

    @Autowired
    public RabbitMQConfig(
        @Value("${rabbitmq.username:guest") String username,
        @Value("${rabbitmq.password:guest") String password,
        @Value("${rabbitmq.hostname:rabbitmq") String hostname,
        @Value("${rabbitmq.port:5672") Integer port,
        @Value("${rabbitmq.virtualhost:/") String virtualhost
    ) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setHost(hostname);
        factory.setPort(port);
        factory.setVirtualHost(virtualhost);
        this.connection = factory.newConnection();
    }

    @Bean
    public Connection rabbitMQConnection() {
        return this.connection;
    }
}
