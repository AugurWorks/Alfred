package com.augurworks.alfred.util;

public final class TimeUtils {

    private TimeUtils() {
        // utility class
    }

    public static String formatTimeSince(long startTimeMillis) {
        long millisPassed = System.currentTimeMillis() - startTimeMillis;
        int secondsPassed = (int) millisPassed / 1000;
        return formatSeconds(secondsPassed);
    }

    public static String formatSeconds(int seconds) {
        return String.format("%02dhr %02dmin %02dsec",
                             seconds / 3600,
                             (seconds % 3600) / 60,
                             (seconds % 60));
    }

}
