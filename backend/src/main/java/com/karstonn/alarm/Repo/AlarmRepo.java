package com.karstonn.alarm.Repo;

import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;

import java.util.List;
public interface AlarmRepo {
    Alarm getAlarm(AlarmId id);

    List<Alarm> listAlarms();

    AlarmRequestResponse addAlarm(Alarm alarm);

    AlarmRequestResponse updateAlarm(AlarmId id, Alarm alarm);

    AlarmRequestResponse removeAlarm(AlarmId id);
}