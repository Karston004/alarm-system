package com.karstonn.alarm.server;

import com.google.protobuf.ByteString;

import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.ActionId;
import com.karstonn.alarmsystem.proto.ActionParameter;
import com.karstonn.alarmsystem.proto.ActionValue;
import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmPhase;
import com.karstonn.alarmsystem.proto.AlarmPhaseId;
import com.karstonn.alarmsystem.proto.AlarmRepoServiceGrpc;
import com.karstonn.alarmsystem.proto.DayOfWeek;
import com.karstonn.alarmsystem.proto.DeviceCapabilityKey;
import com.karstonn.alarmsystem.proto.DeviceId;
import com.karstonn.alarmsystem.proto.FileData;
import com.karstonn.alarmsystem.proto.Percentage;
import com.karstonn.alarmsystem.proto.RGBA;
import com.karstonn.alarmsystem.proto.TimeOfDay;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ServerTestClient {

    private static final String TEST_DEVICE_ID =
            "test-device";

    private static final String TEST_CAPABILITY_KEY =
            "test-capability";


    public static void main(String[] args)
            throws InterruptedException {

        ManagedChannel channel =
                ManagedChannelBuilder
                        .forAddress("localhost", 8080)
                        .usePlaintext()
                        .build();

        try {
            AlarmRepoServiceGrpc.AlarmRepoServiceBlockingStub stub =
                    AlarmRepoServiceGrpc.newBlockingStub(channel);

            String runId =
                    UUID.randomUUID()
                            .toString()
                            .substring(0, 8);


            System.out.println();
            System.out.println(
                    "========================================"
            );
            System.out.println(
                    " Alarm System AddAlarm Tests"
            );
            System.out.println(
                    " Run ID: " + runId
            );
            System.out.println(
                    "========================================"
            );


            // =================================================
            // TEST 1
            // Alarm only
            // =================================================

            Alarm alarmOnly =
                    baseAlarm(
                            "TEST 1 - Alarm Only - " + runId
                    )
                            .build();

            runTest(
                    stub,
                    "1. Alarm only",
                    alarmOnly
            );


            // =================================================
            // TEST 2
            // Alarm + Phase
            // =================================================

            AlarmPhase phaseOnly =
                    createPhase(
                            "Morning Phase",
                            7,
                            30
                    );

            Alarm alarmWithPhase =
                    baseAlarm(
                            "TEST 2 - Alarm + Phase - " + runId
                    )
                            .addAlarmPhases(phaseOnly)
                            .build();

            runTest(
                    stub,
                    "2. Alarm + Phase",
                    alarmWithPhase
            );


            // =================================================
            // TEST 3
            // Alarm + Phase + Action
            // =================================================

            Action actionWithoutParameters =
                    createBaseAction(
                            "Test Action"
                    )
                            .build();

            AlarmPhase phaseWithAction =
                    createPhaseBuilder(
                            "Action Phase",
                            8,
                            15
                    )
                            .addActions(
                                    actionWithoutParameters
                            )
                            .build();

            Alarm alarmWithAction =
                    baseAlarm(
                            "TEST 3 - Alarm + Phase + Action - "
                                    + runId
                    )
                            .addAlarmPhases(
                                    phaseWithAction
                            )
                            .build();

            runTest(
                    stub,
                    "3. Alarm + Phase + Action",
                    alarmWithAction
            );


            // =================================================
            // TEST 4
            // Full hierarchy + every parameter value
            // =================================================

            Action fullAction =
                    createBaseAction(
                            "Full Parameter Test Action"
                    )

                            .addParameters(
                                    parameter(
                                            "string-param",
                                            "String Parameter",
                                            ActionValue.newBuilder()
                                                    .setStringVal(
                                                            "Hello Turso"
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    parameter(
                                            "uint32-param",
                                            "UInt32 Parameter",
                                            ActionValue.newBuilder()
                                                    .setUint32Val(
                                                            123456789
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    parameter(
                                            "int32-param",
                                            "Int32 Parameter",
                                            ActionValue.newBuilder()
                                                    .setInt32Val(
                                                            -12345
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    parameter(
                                            "bool-param",
                                            "Boolean Parameter",
                                            ActionValue.newBuilder()
                                                    .setBoolVal(
                                                            true
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    parameter(
                                            "rgba-param",
                                            "RGBA Parameter",
                                            ActionValue.newBuilder()
                                                    .setRgbaVal(
                                                            RGBA.newBuilder()
                                                                    .setRgba(
                                                                            0x3366CCFF
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    parameter(
                                            "percentage-param",
                                            "Percentage Parameter",
                                            ActionValue.newBuilder()
                                                    .setPercentage(
                                                            Percentage.newBuilder()
                                                                    .setValue(
                                                                            75
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    parameter(
                                            "double-param",
                                            "Double Parameter",
                                            ActionValue.newBuilder()
                                                    .setDoubleVal(
                                                            123.456
                                                    )
                                                    .build()
                                    )
                            )

                            .addParameters(
                                    fileParameter()
                            )

                            .build();


            AlarmPhase fullPhase =
                    createPhaseBuilder(
                            "Full Test Phase",
                            9,
                            45
                    )
                            .addActions(fullAction)
                            .build();


            Alarm fullAlarm =
                    baseAlarm(
                            "TEST 4 - Full Alarm - " + runId
                    )
                            .addAlarmPhases(fullPhase)
                            .build();


            runTest(
                    stub,
                    "4. Full alarm + all parameter types",
                    fullAlarm
            );


            System.out.println();
            System.out.println(
                    "========================================"
            );
            System.out.println(
                    " Test client finished"
            );
            System.out.println(
                    "========================================"
            );

        } finally {
            channel.shutdown()
                    .awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    );
        }
    }


    // =========================================================
    // Test Runner
    // =========================================================

    private static void runTest(
            AlarmRepoServiceGrpc.AlarmRepoServiceBlockingStub stub,
            String testName,
            Alarm alarm
    ) {

        System.out.println();
        System.out.println(
                "----------------------------------------"
        );
        System.out.println(
                "Running: " + testName
        );

        try {

            AddAlarmRequest request =
                    AddAlarmRequest.newBuilder()
                            .setAlarm(alarm)
                            .build();

            AddAlarmResponse response =
                    stub.addAlarm(request);

            if (!response.getSuccess()) {

                System.out.println(
                        "FAILED"
                );

                System.out.println(
                        "Server message: "
                                + response.getMsg()
                );

                return;
            }


            System.out.println(
                    "PASSED"
            );

            System.out.println(
                    "Alarm ID: "
                            + response
                            .getId()
                            .getAlarmId()
            );

            System.out.println(
                    "Server message: "
                            + response.getMsg()
            );

        } catch (StatusRuntimeException e) {

            System.out.println(
                    "FAILED"
            );

            System.out.println(
                    "gRPC status: "
                            + e.getStatus()
            );

            if (
                    e.getStatus()
                            .getDescription()
                            != null
            ) {
                System.out.println(
                        "Description: "
                                + e.getStatus()
                                .getDescription()
                );
            }
        }
    }


    // =========================================================
    // Alarm Builders
    // =========================================================

    private static Alarm.Builder baseAlarm(
            String label
    ) {

        return Alarm.newBuilder()
                .setLabel(label)
                .setIsRecurring(true)
                .setIsEnabled(true)

                .addDays(
                        DayOfWeek.MONDAY
                )

                .addDays(
                        DayOfWeek.WEDNESDAY
                )

                .addDays(
                        DayOfWeek.FRIDAY
                );
    }


    // =========================================================
    // Phase Builders
    // =========================================================

    private static AlarmPhase createPhase(
            String label,
            int hour,
            int minute
    ) {
        return createPhaseBuilder(
                label,
                hour,
                minute
        ).build();
    }


    private static AlarmPhase.Builder createPhaseBuilder(
            String label,
            int hour,
            int minute
    ) {

        AlarmPhaseId phaseId =
                AlarmPhaseId.newBuilder()
                        .setPhaseId(
                                UUID.randomUUID()
                                        .toString()
                        )
                        .build();

        TimeOfDay time =
                TimeOfDay.newBuilder()
                        .setHour(hour)
                        .setMin(minute)
                        .build();

        return AlarmPhase.newBuilder()
                .setPhaseId(phaseId)
                .setLabel(label)
                .setTriggerTime(time);
    }


    // =========================================================
    // Action Builders
    // =========================================================

    private static Action.Builder createBaseAction(
            String label
    ) {

        ActionId actionId =
                ActionId.newBuilder()
                        .setActionId(
                                UUID.randomUUID()
                                        .toString()
                        )
                        .build();

        DeviceId deviceId =
                DeviceId.newBuilder()
                        .setId(
                                TEST_DEVICE_ID
                        )
                        .build();

        DeviceCapabilityKey capabilityKey =
                DeviceCapabilityKey.newBuilder()
                        .setKey(
                                TEST_CAPABILITY_KEY
                        )
                        .build();

        return Action.newBuilder()
                .setId(actionId)
                .setLabel(label)
                .setDeviceId(deviceId)
                .setDeviceActionKey(
                        capabilityKey
                );
    }


    // =========================================================
    // Parameter Builders
    // =========================================================

    private static ActionParameter parameter(
            String key,
            String label,
            ActionValue value
    ) {

        return ActionParameter.newBuilder()
                .setParameterId(
                        UUID.randomUUID()
                                .toString()
                )
                .setParameterKey(key)
                .setLabel(label)
                .setValue(value)
                .build();
    }


    private static ActionParameter fileParameter() {

        byte[] content =
                "This is a test file stored by the Alarm System."
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        FileData file =
                FileData.newBuilder()
                        .setFilename(
                                "test.txt"
                        )
                        .setFileType(
                                "text/plain"
                        )
                        .setSizeBytes(
                                content.length
                        )
                        .setFileContent(
                                ByteString.copyFrom(
                                        content
                                )
                        )
                        .build();


        return parameter(
                "file-param",
                "File Parameter",
                ActionValue.newBuilder()
                        .setFile(file)
                        .build()
        );
    }
}