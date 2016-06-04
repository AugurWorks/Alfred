import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.more.appenders.DataFluentAppender

import static ch.qos.logback.classic.Level.INFO

appender("FLUENTD", DataFluentAppender) {
    tag = "alfred"
    label = "logback"
    remoteHost = "localhost"
    port = 24224
    maxQueueSize = 999
    additionalFields = [
        function: "alfred"
    ]
}

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date [%thread] %-5level %logger{15}#%line %msg %n"
    }
}
root(INFO, ["STDOUT", "FLUENTD"])