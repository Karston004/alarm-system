package com.karstonn.alarm;

import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListRequest;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmRepoServiceGrpc;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

public class GrpcAlarmRepo implements AlarmRepo {

    private final ManagedChannel channel;

    private final AlarmRepoServiceGrpc
            .AlarmRepoServiceBlockingStub stub;


    public GrpcAlarmRepo(
            String host,
            int port
    ) {
        channel =
                OkHttpChannelBuilder
                        .forAddress(host, port)
                        .useTransportSecurity()
                        .build();

        stub =
                AlarmRepoServiceGrpc
                        .newBlockingStub(channel);
    }


    @Override
    public CompletableFuture<AlarmListResponse> listAlarms() {

        return CompletableFuture.supplyAsync(() -> {
            try {

                AlarmListRequest request =
                        AlarmListRequest
                                .newBuilder()
                                .build();

                return stub.listAlarms(request);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<AddAlarmResponse> addAlarm(
            AddAlarmRequest request
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return stub.addAlarm(request);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<Alarm> getAlarm(
            AlarmId id
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return stub.getAlarm(id);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<AlarmRequestResponse> updateAlarm(
            UpdateAlarmRequest request
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return stub.updateAlarm(request);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<AlarmRequestResponse> removeAlarm(
            AlarmId id
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return stub.removeAlarm(id);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    private static <T> CompletableFuture<T> failedFuture(
            Throwable throwable
    ) {
        CompletableFuture<T> future =
                new CompletableFuture<>();

        future.completeExceptionally(throwable);

        return future;
    }


    public void shutdown() {
        channel.shutdown();
    }
}