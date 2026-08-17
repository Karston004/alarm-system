package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AlarmRepo {
    CompletableFuture<Alarm> getAlarm(AlarmId id);

    CompletableFuture<AlarmListResponse> listAlarms();

    CompletableFuture<AddAlarmResponse> addAlarm(AddAlarmRequest addAlarmRequest);

    CompletableFuture<AlarmRequestResponse> updateAlarm(UpdateAlarmRequest updateRequest);

    CompletableFuture<AlarmRequestResponse> removeAlarm(AlarmId id);
}