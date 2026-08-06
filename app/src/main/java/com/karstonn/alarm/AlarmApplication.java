package com.karstonn.alarm;

import android.app.Application;

public class AlarmApplication extends Application {
    private AlarmRepo alarmRepo;
    private DeviceRepo deviceRepo;
    @Override
    public void onCreate() {
        super.onCreate();

        this.alarmRepo = new InMemoryAlarmRepo();
        this.deviceRepo = new DebugDeviceRepo();
    }
    public AlarmRepo getAlarmRepo(){
        return alarmRepo;
    }
    public DeviceRepo getDeviceRepo(){return deviceRepo;}
}