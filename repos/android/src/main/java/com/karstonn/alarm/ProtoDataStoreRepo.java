package com.karstonn.alarm;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.datastore.guava.GuavaDataStore;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmListing;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.AlarmStorage;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Uses Proto DataStore as local Android repo.
 * -
 * Does not scale well - suitable for single user only.
 * Does not require a network connection.
 */
public class ProtoDataStoreRepo implements AlarmRepo{
    private final GuavaDataStore<AlarmStorage> dataStore;
    public ProtoDataStoreRepo(Context context){
        this.dataStore = new GuavaDataStore.Builder<>(
                context,
                "alarms.pb",
                new AlarmStorageSerializer()
        ).build();
    }

    @Override
    public CompletableFuture<Alarm> getAlarm(AlarmId id) {
        return toCompletableFuture(dataStore.getDataAsync())
                .thenApply(alarmStorage -> {

                    for (Alarm alarm : alarmStorage.getAlarmsList()) {
                        if (alarm.getId().equals(id)) {
                            return alarm;
                        }
                    }

                    return null;
                });
    }

    @Override
    public CompletableFuture<AlarmListResponse> listAlarms() {
        return toCompletableFuture(dataStore.getDataAsync())
                .thenApply(alarmStorage -> {
                    AlarmListResponse.Builder responseBuilder = AlarmListResponse.newBuilder();
                    AlarmListing.Builder listingBuilder = AlarmListing.newBuilder();

                    for (Alarm alarm: alarmStorage.getAlarmsList()) {
                        listingBuilder
                                .setId(alarm.getId())
                                .setLabel(alarm.getLabel())
                                .setIsEnabled(alarm.getIsEnabled());
                        responseBuilder.addAlarms(listingBuilder.build());
                    }
                    return responseBuilder.build();
                });
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> addAlarm(Alarm alarm) {

        // Give new alarms an ID if they do not already have one.
        Alarm alarmToAdd = alarm;

        if (!alarm.hasId() || alarm.getId().getId().isEmpty()) {

            //TODO - we sure this is unique!?!?!?!?
            AlarmId id = AlarmId.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .build();

            alarmToAdd = alarm.toBuilder()
                    .setId(id)
                    .build();
        }

        Alarm finalAlarm = alarmToAdd;
        AtomicBoolean added = new AtomicBoolean(false);

        ListenableFuture<AlarmStorage> update =
                dataStore.updateDataAsync(storage -> {

                    // Don't allow duplicate IDs.
                    for (Alarm existing : storage.getAlarmsList()) {
                        if (existing.getId().equals(finalAlarm.getId())) {
                            return storage;
                        }
                    }

                    added.set(true);

                    return storage.toBuilder()
                            .addAlarms(finalAlarm)
                            .build();
                });

        return toCompletableFuture(update)
                .thenApply(storage ->
                        AlarmRequestResponse.newBuilder()
                                .setSuccess(added.get())
                                .build()
                );
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> updateAlarm(
            UpdateAlarmRequest updateRequest
    ) {
        ListenableFuture<AlarmStorage> update =
                dataStore.updateDataAsync(storage -> {

                    AlarmStorage.Builder builder = storage.toBuilder();

                    Alarm replacement =
                            updateRequest.getAlarm()
                                    .toBuilder()
                                    .setId(updateRequest.getId())
                                    .build();

                    for (int i = 0; i < builder.getAlarmsCount(); i++) {

                        if (builder.getAlarms(i)
                                .getId()
                                .equals(updateRequest.getId())) {

                            builder.setAlarms(i, replacement);
                            return builder.build();
                        }
                    }

                    // No existing alarm with this ID — add it.
                    builder.addAlarms(replacement);

                    return builder.build();
                });

        return toCompletableFuture(update)
                .thenApply(storage ->
                        AlarmRequestResponse.newBuilder()
                                .setSuccess(true)
                                .build()
                );
    }

    @Override
    public CompletableFuture<AlarmRequestResponse> removeAlarm(AlarmId id) {

        AtomicBoolean removed = new AtomicBoolean(false);

        ListenableFuture<AlarmStorage> update =
                dataStore.updateDataAsync(storage -> {

                    AlarmStorage.Builder builder = storage.toBuilder();

                    for (int i = 0; i < builder.getAlarmsCount(); i++) {

                        if (builder.getAlarms(i).getId().equals(id)) {

                            builder.removeAlarms(i);
                            removed.set(true);
                            break;
                        }
                    }

                    return builder.build();
                });

        return toCompletableFuture(update)
                .thenApply(storage ->
                        AlarmRequestResponse.newBuilder()
                                .setSuccess(removed.get())
                                .build()
                );
    }

    /**
     * Convert Google's ListenableFuture into the standard Java
     * CompletableFuture used by AlarmRepo.
     */
    private static <T> CompletableFuture<T> toCompletableFuture(
            ListenableFuture<T> future
    ) {

        CompletableFuture<T> result = new CompletableFuture<>();

        Futures.addCallback(
                future,
                new FutureCallback<T>() {
                    @Override
                    public void onSuccess(@Nullable T value) {
                        result.complete(value);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        result.completeExceptionally(throwable);
                    }
                },
                MoreExecutors.directExecutor()
        );

        return result;
    }
}
