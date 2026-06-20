package com.karstonn.alarm.ui.scheduleList;

public class DebugScheduleItem {
    private final String name;
    private final boolean enabled;

    public DebugScheduleItem(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}