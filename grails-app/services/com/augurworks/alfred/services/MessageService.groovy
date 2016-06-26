package com.augurworks.alfred.services

import com.augurworks.alfred.messaging.TrainingMessage
import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.*
import grails.core.GrailsApplication
import grails.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

import javax.annotation.PostConstruct

@Transactional
class MessagingService {

    boolean lazyInit = false

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class)

    public static final String ROOT_TRAINING_CHANNEL = "nets.training"
    public static final String ROOT_RESULTS_CHANNEL = "nets.results"

    public String trainingChannelName
    public String resultsChannelName

    private final ObjectMapper mapper = new ObjectMapper()

    GrailsApplication grailsApplication
    AlfredService alfredService

    private Channel trainingChannel
    private Channel resultChannel

    @PostConstruct
    private void init() {
        log.info('Initializing RabbitMQ connections')
        try {
            ConnectionFactory factory = new ConnectionFactory()
            factory.setUsername((String) grailsApplication.config.rabbitmq.username)
            factory.setPassword((String) grailsApplication.config.rabbitmq.password)
            factory.setHost((String) grailsApplication.config.rabbitmq.hostname)
            factory.setPort(Integer.valueOf(grailsApplication.config.rabbitmq.portnum))
            factory.setRequestedHeartbeat(1)
            factory.setConnectionTimeout(5000)
            factory.setAutomaticRecoveryEnabled(true)
            factory.setTopologyRecoveryEnabled(true)
            Connection connection = factory.newConnection()

            connection.addShutdownListener(new ShutdownListener() {
                public void shutdownCompleted(ShutdownSignalException e) {
                    log.error('RabbitMQ connection lost', e)
                }
            });

            String channelPostfix = grailsApplication.config.rabbitmq.env ? '.' + grailsApplication.config.rabbitmq.env.toLowerCase() : ''

            trainingChannelName = ROOT_TRAINING_CHANNEL + channelPostfix
            resultsChannelName = ROOT_RESULTS_CHANNEL + channelPostfix

            log.info('Connecting to RabbitMQ channel ' + trainingChannelName)
            trainingChannel = connection.createChannel()
            trainingChannel.queueDeclare(trainingChannelName, false, false, false, null)

            log.info('Connecting to RabbitMQ channel ' + resultsChannelName)
            resultChannel = connection.createChannel()
            resultChannel.queueDeclare(resultsChannelName, false, false, false, null)

            initTrainingConsumer(trainingChannel)
        } catch (Exception e) {
            trainingChannel = null
            resultChannel = null
            log.error("Could not connect to RabbitMQ", e)
        }
    }

    private void initTrainingConsumer(Channel trainingChannel) {
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
            trainingChannel.basicConsume(trainingChannelName, false, consumer);
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
            resultChannel.basicPublish("", resultsChannelName, null, mapper.writeValueAsString(message).getBytes());
        } catch (IOException e) {
            log.error("An error occurred when publishing a message for net {}", netId, e);
        }
    }
}
