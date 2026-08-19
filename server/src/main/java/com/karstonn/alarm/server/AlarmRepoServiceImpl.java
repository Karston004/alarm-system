package com.karstonn.alarm.server;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListRequest;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmRepoServiceGrpc;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class AlarmRepoServiceImpl
        extends AlarmRepoServiceGrpc.AlarmRepoServiceImplBase {

    private final AlarmRepo repo;

    public AlarmRepoServiceImpl(AlarmRepo repo) {
        this.repo = repo;
    }

    @Override
    public void addAlarm(
            AddAlarmRequest request,
            StreamObserver<AddAlarmResponse> responseObserver
    ) {
        try {
            AddAlarmResponse response =
                    repo.addAlarm(request).get();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to add alarm")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
    @Override
    public void listAlarms(
            AlarmListRequest request,
            io.grpc.stub.StreamObserver<AlarmListResponse> responseObserver
    ){
        try {
            AlarmListResponse response =
                    repo.listAlarms().get();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to list alarms")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void getAlarm(
            AlarmId id,
            io.grpc.stub.StreamObserver<Alarm> responseObserver
    ){
        try {
            Alarm response =
                    repo.getAlarm(id).get();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to get alarm")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void updateAlarm(
            UpdateAlarmRequest request,
            io.grpc.stub.StreamObserver<AlarmRequestResponse> responseObserver
    ){
        try {
            AlarmRequestResponse response =
                    repo.updateAlarm(request).get();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to update alarm")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void removeAlarm(
            AlarmId request,
            io.grpc.stub.StreamObserver<AlarmRequestResponse> responseObserver
    ){
        try {
            AlarmRequestResponse response =
                    repo.removeAlarm(request).get();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to remove alarm")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}