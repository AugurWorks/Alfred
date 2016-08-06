import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.DEBUG

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date [%thread] %-5level %logger{15}#%line %msg %n"
    }
}
logger('com.augurworks.alfred', DEBUG, ["STDOUT"])
