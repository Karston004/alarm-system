package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
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
        return CompletableFuture.completedFuture(alarms.get(id.getAlarmId()));
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
    public CompletableFuture<AddAlarmResponse> addAlarm(AddAlarmRequest addAlarmRequest) {

        AlarmId id = newID();

        Alarm alarm = addAlarmRequest.getAlarm().toBuilder()
                .setId(id)
                .build();

        alarms.put(id.getAlarmId(), alarm);

        return CompletableFuture.completedFuture(
                AddAlarmResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        );
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> updateAlarm(
            UpdateAlarmRequest request
    ) {
        String id = request.getId().getAlarmId();

        if (!alarms.containsKey(id)) {
            return CompletableFuture.completedFuture(
                    AlarmRequestResponse.newBuilder()
                            .setSuccess(false)
                            .build()
            );
        }

        Alarm alarm = request.getAlarm()
                .toBuilder()
                .setId(request.getId())
                .build();

        alarms.put(id, alarm);

        return CompletableFuture.completedFuture(
                AlarmRequestResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        );
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> removeAlarm(AlarmId id) {
        //TODO if id doesn't exist, return false
        AlarmRequestResponse response;
        alarms.remove(id.getAlarmId());
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
                .setAlarmId(id)
                .build();
    }

}

