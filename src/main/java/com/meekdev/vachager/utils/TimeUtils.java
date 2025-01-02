package com.meekdev.vachager.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static long getMillisFromMinutes(int minutes) {
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    public static long getSecondsFromMillis(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    public static long getMinutesFromMillis(long millis) {
        return TimeUnit.MILLISECONDS.toMinutes(millis);
    }

    public static String formatTime(long timeInSeconds) {
        if (timeInSeconds > 3600) {
            return String.format("%.1fh", timeInSeconds / 3600.0);
        } else if (timeInSeconds > 60) {
            return String.format("%dm", timeInSeconds / 60);
        } else {
            return String.format("%ds", timeInSeconds);
        }
    }
}