package com.augurworks.alfred.server;

import java.io.PrintWriter;

public class LoggingHelper {

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
            e.printStackTrace();
        }
    }

    public static void out(String message, PrintWriter logLocation) {
        System.out.println(message);
        if (logLocation != null) {
            synchronized (logLocation) {
                logLocation.println(message);
            }
        }
    }
}
