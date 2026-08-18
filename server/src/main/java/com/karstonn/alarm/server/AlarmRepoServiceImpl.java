package com.karstonn.alarm.server;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.AlarmListRequest;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmRepoServiceGrpc;

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
}