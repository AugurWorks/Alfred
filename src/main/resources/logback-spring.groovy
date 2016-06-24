import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.more.appenders.DataFluentAppender

import static ch.qos.logback.classic.Level.DEBUG

appender("FLUENTD", DataFluentAppender) {
    label = "logback"
    remoteHost = System.getenv('FLUENTD_HOST')
    port = 24224
    maxQueueSize = 999
    additionalFields = [
        function: "ALF",
        env: System.getProperty('ENV') ?: (System.getenv('ENV') ?: 'DEV'),
        hostname: System.getenv('HOSTNAME') ?: InetAddress.getLocalHost().getHostName()
    ]
}

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date [%thread] %-5level %logger{15}#%line %msg %n"
    }
}
logger('com.augurworks.alfred', DEBUG, System.getenv('FLUENTD_HOST') ? ["STDOUT", "FLUENTD"] : ["STDOUT"])
