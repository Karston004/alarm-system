package com.karstonn.alarm;

import android.app.Application;

public class AlarmApplication extends Application {
    private AlarmRepo repo;
    @Override
    public void onCreate() {
        super.onCreate();

        this.repo = new InMemoryAlarmRepo();
    }
    public AlarmRepo getAlarmRepo(){
        return repo;
    }
}