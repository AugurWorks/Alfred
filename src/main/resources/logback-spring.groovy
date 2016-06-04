import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.more.appenders.DataFluentAppender

import static ch.qos.logback.classic.Level.INFO

appender("FLUENTD", DataFluentAppender) {
    label = "logback"
    remoteHost = System.getenv('FLUENTD_HOST')
    port = 24224
    maxQueueSize = 999
    additionalFields = [
        function: "ALF",
        hostname: System.getenv('HOSTNAME') ?: InetAddress.getLocalHost().getHostName()
    ]
}

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date [%thread] %-5level %logger{15}#%line %msg %n"
    }
}
root(INFO, System.getenv('FLUENTD_HOST') ? ["STDOUT", "FLUENTD"] : ["STDOUT"])
