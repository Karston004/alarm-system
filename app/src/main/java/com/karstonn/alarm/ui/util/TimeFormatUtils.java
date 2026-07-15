package com.karstonn.alarm.ui.util;

import com.karstonn.alarmsystem.proto.TimeOfDay;

import java.util.Locale;

public final class TimeFormatUtils {
    private TimeFormatUtils() {}

    public static String formatTimeOfDay(TimeOfDay time) {
        return String.format(
                Locale.UK,
                "%02d:%02d",
                time.getHour(),
                time.getMin()
        );
    }
}