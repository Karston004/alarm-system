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
import java.util.concurrent.CompletableFuture;

public class InMemoryAlarmRepo implements AlarmRepo {
    private final Map<String, Alarm> alarms = new HashMap<>();
    @Override
    public CompletableFuture<Alarm> getAlarm(AlarmId id) {
        return CompletableFuture.completedFuture(alarms.get(id.getId()));
    }

    @Override
    public CompletableFuture<AlarmListResponse> listAlarms() {
        AlarmListResponse.Builder responseBuilder = AlarmListResponse.newBuilder();
        AlarmListing.Builder listingBuilder = AlarmListing.newBuilder();
        for (Alarm alarm: alarms.values()) {
            listingBuilder
                    .setId(alarm.getId())
                    .setLabel(alarm.getLabel())
                    .setIsEnabled(alarm.getIsEnabled());
            responseBuilder.addAlarms(listingBuilder.build());
        }
        return CompletableFuture.completedFuture(responseBuilder.build());
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> addAlarm(Alarm alarm) {
        AlarmRequestResponse response;
        if (!alarm.hasId() || alarm.getId().getId().isEmpty() && !alarms.containsKey(alarm.getId().getId())) {
            alarm = alarm.toBuilder()
                    .setId(newID())
                    .build();

            alarms.put(alarm.getId().getId(), alarm);

            response = AlarmRequestResponse.newBuilder()
                    .setSuccess(true)
                    .build();
        } else {
            response = AlarmRequestResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> updateAlarm(UpdateAlarmRequest updateRequest) {
        AlarmRequestResponse response;
        Alarm alarm = updateRequest.getAlarm();

        if (!alarm.hasId() || alarm.getId().getId().isEmpty()) {
            alarm = alarm.toBuilder()
                    .setId(newID())
                    .build();
        }

        alarms.put(alarm.getId().getId(), alarm);

        response = AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> removeAlarm(AlarmId id) {
        //TODO if id doesn't exist, return false
        AlarmRequestResponse response;
        alarms.remove(id.getId());
        response = AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
        return CompletableFuture.completedFuture(response);
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

