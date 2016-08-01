package com.augurworks.alfred.util;

public final class TimeUtils {

    public static String formatSeconds(int seconds) {
        return String.format("%02dhr %02dmin %02dsec",
                             seconds / 3600,
                             (seconds % 3600) / 60,
                             (seconds % 60));
    }

}
