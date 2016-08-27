import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

import java.util.concurrent.TimeoutException

@Grab('com.rabbitmq:amqp-client:3.6.2')

Channel trainingChannel() {
    println "Creating training RabbitMQ channel {}" + getTrainingChannelName()
    try {
        Channel channel = getConnection().createChannel();
        channel.basicQos(1);
        channel.queueDeclare(getTrainingChannelName(), true, false, false, null);
        return channel;
    } catch (IOException | TimeoutException e) {
        println "Could not connect to RabbitMQ"
        return null;
    }
}

Connection getConnection() throws IOException, TimeoutException {
    String username = 'guest'
    String password = 'guest'
    String hostname = '192.168.99.100'

    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setHost(hostname);
    factory.setConnectionTimeout(5000);
    return factory.newConnection();
}

String getTrainingChannelName() {
    return 'nets.training.dev'
}

Channel trainingChannel = trainingChannel()
String filename = 'data/' + args[0]
String fileContents = new File(filename).text

trainingChannel.basicPublish("", getTrainingChannelName(), null, fileContents.getBytes());

println 'Submitted ' + filename

System.exit(0)