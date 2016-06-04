package com.augurworks.alfred.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

public class LoggingHelper {

    static Logger log = LoggerFactory.getLogger(LoggingHelper.class);

    private LoggingHelper() {
        // utility class
    }

    public static void flushAndCloseQuietly(PrintWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
        } catch (Exception e) {
            log.error("Error closing log file", e);
        }
    }

    public static void out(String message, PrintWriter logLocation) {
        log.info(message);
        log(message, logLocation);
    }

    public static void error(String message, PrintWriter logLocation) {
        log.error(message);
        log(message, logLocation);
    }

    public static void error(String message, PrintWriter logLocation, Exception e) {
        log.error(message, e);
        log(message, logLocation);
    }

    private static void log(String message, PrintWriter logLocation) {
        if (logLocation != null) {
            synchronized (logLocation) {
                logLocation.println(message);
            }
        }
    }
}
