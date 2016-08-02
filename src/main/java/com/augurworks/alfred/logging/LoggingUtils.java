package com.augurworks.alfred.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.more.appenders.DataFluentAppender;
import com.augurworks.alfred.messaging.TrainingMessage;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LoggingUtils {

    public static final String FLUENT_HOST_KEY = "fluentHost";
    public static final String LOGGING_ENV_KEY = "loggingEnv";

    private static final Integer FLUENT_PORT = 24224;
    private static final String FUNCTION_KEY = "function";
    private static final String FUNCTION_VALUE = "ALF";
    private static final String HOSTNAME_KEY = "hostname";
    private static final String ENV_KEY = "env";

    public static void addFluentAppender(TrainingMessage trainingMessage, String hostname) {
        Map<String, String> metadata = trainingMessage.getMetadata();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        DataFluentAppender fluentAppender = new DataFluentAppender();
        fluentAppender.setName("FluentD");
        fluentAppender.setRemoteHost(metadata.get(FLUENT_HOST_KEY));
        fluentAppender.setPort(FLUENT_PORT);
        fluentAppender.setLabel("logback");
        fluentAppender.setMaxQueueSize(999);
        fluentAppender.addAdditionalField(createField(FUNCTION_KEY, FUNCTION_VALUE));
        fluentAppender.addAdditionalField(createField(HOSTNAME_KEY, hostname));
        fluentAppender.addAdditionalField(createField(ENV_KEY, metadata.get(LOGGING_ENV_KEY)));
        fluentAppender.setContext(loggerContext);
        fluentAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger("com.augurworks.alfred");
        logger.addAppender(fluentAppender);
    }

    private static DataFluentAppender.Field createField(String key, String value) {
        DataFluentAppender.Field field = new DataFluentAppender.Field();
        field.setKey(key);
        field.setValue(value);
        return field;
    }
}
