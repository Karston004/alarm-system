package com.karstonn.alarm;

import android.app.Application;

public class AlarmApplication extends Application {
    private AlarmRepo alarmRepo;
    private DeviceRepo deviceRepo;
    @Override
    public void onCreate() {
        super.onCreate();

        this.alarmRepo = new GrpcAlarmRepo(
                        "alarm-system-server-244710268941.europe-west2.run.app",
                        443
                );
        this.deviceRepo = new DebugDeviceRepo();
    }
    public AlarmRepo getAlarmRepo(){
        return alarmRepo;
    }
    public DeviceRepo getDeviceRepo(){return deviceRepo;}
}