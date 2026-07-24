package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmListing;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryAlarmRepo implements AlarmRepo {
    private final Map<String, Alarm> alarms = new HashMap<>();
    @Override
    public Alarm getAlarm(AlarmId id) {
        return alarms.get(id.getId());
    }

    @Override
    public AlarmListResponse listAlarms() {
        AlarmListResponse.Builder responseBuilder = AlarmListResponse.newBuilder();
        AlarmListing.Builder listingBuilder = AlarmListing.newBuilder();
        for (Alarm alarm: alarms.values()) {
            listingBuilder
                    .setId(alarm.getId())
                    .setLabel(alarm.getLabel())
                    .setIsEnabled(alarm.getIsEnabled());
            responseBuilder.addAlarms(listingBuilder.build());
        }
        return responseBuilder.build();
    }

    @Override
    public AlarmRequestResponse addAlarm(Alarm alarm) {
        if (!alarm.hasId() || alarm.getId().getId().isEmpty()) {
            alarm = alarm.toBuilder()
                    .setId(newID())
                    .build();
        } else if (alarms.containsKey(alarm.getId().getId())) {
            return AlarmRequestResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }

        alarms.put(alarm.getId().getId(), alarm);

        return AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
    }

    @Override
    public AlarmRequestResponse updateAlarm(UpdateAlarmRequest updateRequest) {
        Alarm alarm = updateRequest.getAlarm();

        if (!alarm.hasId() || alarm.getId().getId().isEmpty()) {
            alarm = alarm.toBuilder()
                    .setId(newID())
                    .build();
        }

        alarms.put(alarm.getId().getId(), alarm);

        return AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
    }

    @Override
    public AlarmRequestResponse removeAlarm(AlarmId id) {
        alarms.remove(id.getId());
        return AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
    }

    private AlarmId newID() {
        String id;

        do {
            id = UUID.randomUUID().toString();
        } while (alarms.containsKey(id));

        return AlarmId.newBuilder()
                .setId(id)
                .build();
    }

}

