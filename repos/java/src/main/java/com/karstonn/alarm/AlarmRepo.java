package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.List;
public interface AlarmRepo {
    Alarm getAlarm(AlarmId id);

    AlarmListResponse listAlarms();

    AlarmRequestResponse addAlarm(Alarm alarm);

    AlarmRequestResponse updateAlarm(UpdateAlarmRequest updateRequest);

    AlarmRequestResponse removeAlarm(AlarmId id);
}