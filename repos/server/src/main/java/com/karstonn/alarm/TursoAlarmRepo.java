package com.karstonn.alarm;

import com.karstonn.alarm.turso.AlarmSqlMapper;
import com.karstonn.alarm.turso.TursoClient;

import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TursoAlarmRepo implements AlarmRepo {

    private final AlarmSqlMapper alarmSql;


    // =========================================================
    // Construction
    // =========================================================

    public TursoAlarmRepo(
            String databaseUrl,
            String authToken
    ) {
        TursoClient turso =
                new TursoClient(
                        databaseUrl,
                        authToken
                );

        this.alarmSql =
                new AlarmSqlMapper(
                        turso
                );
    }


    // =========================================================
    // AlarmRepo
    // =========================================================

    @Override
    public CompletableFuture<Alarm> getAlarm(
            AlarmId id
    ) {
        return async(
                () -> alarmSql.getAlarm(
                        id
                )
        );
    }


    @Override
    public CompletableFuture<AlarmListResponse> listAlarms() {
        return async(
                alarmSql::listAlarms
        );
    }


    @Override
    public CompletableFuture<AddAlarmResponse> addAlarm(
            AddAlarmRequest request
    ) {
        return async(
                () -> alarmSql.addAlarm(
                        request
                )
        );
    }


    @Override
    public CompletableFuture<AlarmRequestResponse> updateAlarm(
            UpdateAlarmRequest request
    ) {
        return async(
                () -> alarmSql.updateAlarm(
                        request
                )
        );
    }


    @Override
    public CompletableFuture<AlarmRequestResponse> removeAlarm(
            AlarmId id
    ) {
        return async(
                () -> alarmSql.removeAlarm(
                        id
                )
        );
    }


    // =========================================================
    // Async Boundary
    // =========================================================

    private <T> CompletableFuture<T> async(
            ThrowingSupplier<T> operation
    ) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return operation.get();

                    } catch (Exception e) {
                        throw new CompletionException(
                                e
                        );
                    }
                }
        );
    }


    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}