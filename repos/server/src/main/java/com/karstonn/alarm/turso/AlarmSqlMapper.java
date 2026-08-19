package com.karstonn.alarm.turso;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.protobuf.ByteString;

import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.ActionParameter;
import com.karstonn.alarmsystem.proto.ActionValue;
import com.karstonn.alarmsystem.proto.AddAlarmRequest;
import com.karstonn.alarmsystem.proto.AddAlarmResponse;
import com.karstonn.alarmsystem.proto.Alarm;
import com.karstonn.alarmsystem.proto.AlarmId;
import com.karstonn.alarmsystem.proto.AlarmListResponse;
import com.karstonn.alarmsystem.proto.AlarmListing;
import com.karstonn.alarmsystem.proto.AlarmPhase;
import com.karstonn.alarmsystem.proto.AlarmRequestResponse;
import com.karstonn.alarmsystem.proto.DayOfWeek;
import com.karstonn.alarmsystem.proto.FileData;
import com.karstonn.alarmsystem.proto.UpdateAlarmRequest;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlarmSqlMapper {

    private final TursoClient turso;


    // =========================================================
    // Construction
    // =========================================================

    public AlarmSqlMapper(
            TursoClient turso
    ) {
        if (turso == null) {
            throw new IllegalArgumentException(
                    "TursoClient cannot be null"
            );
        }

        this.turso =
                turso;
    }


    // =========================================================
    // Add Alarm
    // =========================================================

    public AddAlarmResponse addAlarm(
            AddAlarmRequest request
    ) throws IOException {

        if (!request.hasAlarm()) {
            throw new IllegalArgumentException(
                    "AddAlarmRequest does not contain an alarm"
            );
        }

        Alarm alarm =
                request.getAlarm();

        AlarmId alarmId =
                AlarmId.newBuilder()
                        .setAlarmId(
                                UUID.randomUUID()
                                        .toString()
                        )
                        .build();

        List<SqlStatement> statements =
                buildAlarmInsertStatements(
                        alarm,
                        alarmId
                );

        turso.executeTransaction(
                statements
        );

        return AddAlarmResponse.newBuilder()
                .setSuccess(true)
                .setMsg("Alarm added successfully")
                .setId(alarmId)
                .build();
    }


    // =========================================================
    // List Alarms
    // =========================================================

    public AlarmListResponse listAlarms()
            throws IOException {

        JsonObject result =
                turso.executeQuery(
                        turso.statement(
                                """
                                SELECT
                                    alarm_id,
                                    label,
                                    is_enabled
                                FROM Alarms
                                ORDER BY label, alarm_id
                                """
                        )
                );

        JsonArray rows =
                result.getAsJsonArray(
                        "rows"
                );

        AlarmListResponse.Builder response =
                AlarmListResponse.newBuilder();

        for (
                JsonElement rowElement
                : rows
        ) {

            JsonArray row =
                    rowElement.getAsJsonArray();

            response.addAlarms(
                    AlarmListing.newBuilder()
                            .setId(
                                    AlarmId.newBuilder()
                                            .setAlarmId(
                                                    turso.textCell(
                                                            row,
                                                            0
                                                    )
                                            )
                                            .build()
                            )
                            .setLabel(
                                    turso.textCell(
                                            row,
                                            1
                                    )
                            )
                            .setIsEnabled(
                                    turso.boolCell(
                                            row,
                                            2
                                    )
                            )
                            .build()
            );
        }

        return response
                .setSuccess(true)
                .setMsg(
                        "Successfully listed alarms"
                )
                .build();
    }


    // =========================================================
    // Get Alarm
    // =========================================================

    public Alarm getAlarm(
            AlarmId id
    ) throws IOException {

        validateAlarmId(
                id
        );

        JsonObject result =
                turso.executeQuery(
                        turso.statement(
                                """
                                SELECT
                                    label,
                                    is_recurring,
                                    is_enabled,
                                    monday,
                                    tuesday,
                                    wednesday,
                                    thursday,
                                    friday,
                                    saturday,
                                    sunday
                                FROM Alarms
                                WHERE alarm_id = ?
                                """,

                                turso.textArg(
                                        id.getAlarmId()
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray(
                        "rows"
                );

        if (rows.size() == 0) {
            throw new IllegalArgumentException(
                    "Alarm does not exist: "
                            + id.getAlarmId()
            );
        }

        JsonArray row =
                rows.get(0)
                        .getAsJsonArray();

        Alarm.Builder alarm =
                Alarm.newBuilder()
                        .setId(id)
                        .setLabel(
                                turso.textCell(
                                        row,
                                        0
                                )
                        )
                        .setIsRecurring(
                                turso.boolCell(
                                        row,
                                        1
                                )
                        )
                        .setIsEnabled(
                                turso.boolCell(
                                        row,
                                        2
                                )
                        );

        addDayIfPresent(
                alarm,
                row,
                3,
                DayOfWeek.MONDAY
        );

        addDayIfPresent(
                alarm,
                row,
                4,
                DayOfWeek.TUESDAY
        );

        addDayIfPresent(
                alarm,
                row,
                5,
                DayOfWeek.WEDNESDAY
        );

        addDayIfPresent(
                alarm,
                row,
                6,
                DayOfWeek.THURSDAY
        );

        addDayIfPresent(
                alarm,
                row,
                7,
                DayOfWeek.FRIDAY
        );

        addDayIfPresent(
                alarm,
                row,
                8,
                DayOfWeek.SATURDAY
        );

        addDayIfPresent(
                alarm,
                row,
                9,
                DayOfWeek.SUNDAY
        );

        loadPhases(
                alarm,
                id
        );

        return alarm.build();
    }


    private void loadPhases(
            Alarm.Builder alarm,
            AlarmId alarmId
    ) throws IOException {

        JsonObject result =
                turso.executeQuery(
                        turso.statement(
                                """
                                SELECT
                                    phase_id,
                                    label,
                                    trigger_time
                                FROM Phases
                                WHERE alarm_id = ?
                                ORDER BY trigger_time, phase_id
                                """,

                                turso.textArg(
                                        alarmId.getAlarmId()
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray(
                        "rows"
                );

        for (
                JsonElement element
                : rows
        ) {

            JsonArray row =
                    element.getAsJsonArray();

            String phaseId =
                    turso.textCell(
                            row,
                            0
                    );

            long triggerTime =
                    turso.integerCell(
                            row,
                            2
                    );

            AlarmPhase.Builder phase =
                    AlarmPhase.newBuilder()
                            .setLabel(
                                    turso.textCell(
                                            row,
                                            1
                                    )
                            );

            phase.getPhaseIdBuilder()
                    .setPhaseId(
                            phaseId
                    );

            phase.getTriggerTimeBuilder()
                    .setHour(
                            (int) (
                                    triggerTime / 60
                            )
                    )
                    .setMin(
                            (int) (
                                    triggerTime % 60
                            )
                    );

            loadActions(
                    phase,
                    phaseId
            );

            alarm.addAlarmPhases(
                    phase
            );
        }
    }


    private void loadActions(
            AlarmPhase.Builder phase,
            String phaseId
    ) throws IOException {

        JsonObject result =
                turso.executeQuery(
                        turso.statement(
                                """
                                SELECT
                                    action_id,
                                    label,
                                    device_id,
                                    device_action_key
                                FROM Actions
                                WHERE phase_id = ?
                                ORDER BY action_id
                                """,

                                turso.textArg(
                                        phaseId
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray(
                        "rows"
                );

        for (
                JsonElement element
                : rows
        ) {

            JsonArray row =
                    element.getAsJsonArray();

            String actionId =
                    turso.textCell(
                            row,
                            0
                    );

            Action.Builder action =
                    Action.newBuilder()
                            .setLabel(
                                    turso.textCell(
                                            row,
                                            1
                                    )
                            );

            action.getIdBuilder()
                    .setActionId(
                            actionId
                    );

            action.getDeviceIdBuilder()
                    .setId(
                            turso.textCell(
                                    row,
                                    2
                            )
                    );

            action.getDeviceActionKeyBuilder()
                    .setKey(
                            turso.textCell(
                                    row,
                                    3
                            )
                    );

            loadParameters(
                    action,
                    actionId
            );

            phase.addActions(
                    action
            );
        }
    }


    private void loadParameters(
            Action.Builder action,
            String actionId
    ) throws IOException {

        JsonObject result =
                turso.executeQuery(
                        turso.statement(
                                """
                                SELECT
                                    ap.parameter_id,
                                    ap.parameter_key,
                                    ap.label,
                                    ap.units,
                                    ap.value_type,
                                    ap.string_val,
                                    ap.uint32_val,
                                    ap.int32_val,
                                    ap.bool_val,
                                    ap.rgba_val,
                                    ap.percentage_val,
                                    ap.double_val,

                                    f.filename,
                                    f.file_type,
                                    f.size_bytes,
                                    f.file_content

                                FROM ActionParameters ap

                                LEFT JOIN Files f
                                    ON f.file_id = ap.file_id

                                WHERE ap.action_id = ?

                                ORDER BY ap.parameter_id
                                """,

                                turso.textArg(
                                        actionId
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray(
                        "rows"
                );

        for (
                JsonElement element
                : rows
        ) {

            JsonArray row =
                    element.getAsJsonArray();

            ActionParameter.Builder parameter =
                    ActionParameter.newBuilder()
                            .setParameterId(
                                    turso.textCell(
                                            row,
                                            0
                                    )
                            )
                            .setParameterKey(
                                    turso.textCell(
                                            row,
                                            1
                                    )
                            )
                            .setLabel(
                                    turso.textCell(
                                            row,
                                            2
                                    )
                            );

            if (!turso.nullCell(row, 3)) {
                parameter.setUnits(
                        turso.textCell(
                                row,
                                3
                        )
                );
            }

            String valueType =
                    turso.textCell(
                            row,
                            4
                    );

            ActionValue.Builder value =
                    ActionValue.newBuilder();

            switch (valueType) {

                case "STRING" ->
                        value.setStringVal(
                                turso.textCell(
                                        row,
                                        5
                                )
                        );

                case "UINT32" ->
                        value.setUint32Val(
                                (int) turso.integerCell(
                                        row,
                                        6
                                )
                        );

                case "INT32" ->
                        value.setInt32Val(
                                (int) turso.integerCell(
                                        row,
                                        7
                                )
                        );

                case "BOOL" ->
                        value.setBoolVal(
                                turso.boolCell(
                                        row,
                                        8
                                )
                        );

                case "RGBA" ->
                        value.getRgbaValBuilder()
                                .setRgba(
                                        (int) turso.integerCell(
                                                row,
                                                9
                                        )
                                );

                case "PERCENTAGE" ->
                        value.getPercentageBuilder()
                                .setValue(
                                        (int) turso.integerCell(
                                                row,
                                                10
                                        )
                                );

                case "DOUBLE" ->
                        value.setDoubleVal(
                                turso.doubleCell(
                                        row,
                                        11
                                )
                        );

                case "FILE" -> {

                    if (
                            turso.nullCell(row, 12)
                                    || turso.nullCell(row, 13)
                                    || turso.nullCell(row, 14)
                                    || turso.nullCell(row, 15)
                    ) {
                        throw new IOException(
                                "FILE parameter is missing its Files row"
                        );
                    }

                    value.getFileBuilder()
                            .setFilename(
                                    turso.textCell(
                                            row,
                                            12
                                    )
                            )
                            .setFileType(
                                    turso.textCell(
                                            row,
                                            13
                                    )
                            )
                            .setSizeBytes(
                                    turso.integerCell(
                                            row,
                                            14
                                    )
                            )
                            .setFileContent(
                                    ByteString.copyFrom(
                                            turso.blobCell(
                                                    row,
                                                    15
                                            )
                                    )
                            );
                }

                default ->
                        throw new IOException(
                                "Unknown ActionParameter type: "
                                        + valueType
                        );
            }

            parameter.setValue(
                    value
            );

            action.addParameters(
                    parameter
            );
        }
    }


    // =========================================================
    // Update Alarm
    // =========================================================

    public AlarmRequestResponse updateAlarm(
            UpdateAlarmRequest request
    ) throws IOException {

        if (!request.hasAlarm()) {
            throw new IllegalArgumentException(
                    "UpdateAlarmRequest does not contain an alarm"
            );
        }

        Alarm alarm =
                request.getAlarm();

        if (!alarm.hasId()) {
            throw new IllegalArgumentException(
                    "Alarm being updated has no ID"
            );
        }

        AlarmId alarmId =
                alarm.getId();

        validateAlarmId(
                alarmId
        );

        if (!alarmExists(alarmId)) {
            return AlarmRequestResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }

        List<SqlStatement> statements =
                new ArrayList<>();

        statements.add(
                turso.statement(
                        """
                        UPDATE Alarms
                        SET
                            label = ?,
                            is_recurring = ?,
                            is_enabled = ?,
                            monday = ?,
                            tuesday = ?,
                            wednesday = ?,
                            thursday = ?,
                            friday = ?,
                            saturday = ?,
                            sunday = ?
                        WHERE alarm_id = ?
                        """,

                        turso.textArg(
                                alarm.getLabel()
                        ),

                        turso.boolArg(
                                alarm.getIsRecurring()
                        ),
                        turso.boolArg(
                                alarm.getIsEnabled()
                        ),

                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.MONDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.TUESDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.WEDNESDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.THURSDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.FRIDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SATURDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SUNDAY
                                )
                        ),

                        turso.textArg(
                                alarmId.getAlarmId()
                        )
                )
        );

        /*
         * Phases -> Actions -> ActionParameters are deleted
         * through ON DELETE CASCADE.
         */
        statements.add(
                turso.statement(
                        """
                        DELETE FROM Phases
                        WHERE alarm_id = ?
                        """,

                        turso.textArg(
                                alarmId.getAlarmId()
                        )
                )
        );

        addAlarmChildInsertStatements(
                statements,
                alarm,
                alarmId
        );

        addDeleteOrphanFilesStatement(
                statements
        );

        turso.executeTransaction(
                statements
        );

        return AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
    }


    // =========================================================
    // Delete Alarm
    // =========================================================

    public AlarmRequestResponse removeAlarm(
            AlarmId id
    ) throws IOException {

        validateAlarmId(
                id
        );

        if (!alarmExists(id)) {
            return AlarmRequestResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }

        List<SqlStatement> statements =
                new ArrayList<>();

        /*
         * Alarms -> Phases -> Actions -> ActionParameters
         * are removed through ON DELETE CASCADE.
         */
        statements.add(
                turso.statement(
                        """
                        DELETE FROM Alarms
                        WHERE alarm_id = ?
                        """,

                        turso.textArg(
                                id.getAlarmId()
                        )
                )
        );

        addDeleteOrphanFilesStatement(
                statements
        );

        turso.executeTransaction(
                statements
        );

        return AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
    }


    private boolean alarmExists(
            AlarmId id
    ) throws IOException {

        JsonObject result =
                turso.executeQuery(
                        turso.statement(
                                """
                                SELECT 1
                                FROM Alarms
                                WHERE alarm_id = ?
                                LIMIT 1
                                """,

                                turso.textArg(
                                        id.getAlarmId()
                                )
                        )
                );

        return result
                .getAsJsonArray(
                        "rows"
                )
                .size() > 0;
    }


    // =========================================================
    // Alarm -> SQL
    // =========================================================

    private List<SqlStatement> buildAlarmInsertStatements(
            Alarm alarm,
            AlarmId alarmId
    ) {
        List<SqlStatement> statements =
                new ArrayList<>();

        statements.add(
                turso.statement(
                        """
                        INSERT INTO Alarms (
                            alarm_id,
                            label,
                            is_recurring,
                            is_enabled,
                            monday,
                            tuesday,
                            wednesday,
                            thursday,
                            friday,
                            saturday,
                            sunday
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,

                        turso.textArg(
                                alarmId.getAlarmId()
                        ),
                        turso.textArg(
                                alarm.getLabel()
                        ),

                        turso.boolArg(
                                alarm.getIsRecurring()
                        ),
                        turso.boolArg(
                                alarm.getIsEnabled()
                        ),

                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.MONDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.TUESDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.WEDNESDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.THURSDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.FRIDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SATURDAY
                                )
                        ),
                        turso.boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SUNDAY
                                )
                        )
                )
        );

        addAlarmChildInsertStatements(
                statements,
                alarm,
                alarmId
        );

        return statements;
    }


    private void addAlarmChildInsertStatements(
            List<SqlStatement> statements,
            Alarm alarm,
            AlarmId alarmId
    ) {

        for (
                AlarmPhase phase
                : alarm.getAlarmPhasesList()
        ) {

            String phaseId =
                    usableId(
                            phase.getPhaseId()
                                    .getPhaseId()
                    );

            int triggerTime =
                    phase.getTriggerTime()
                            .getHour()
                            * 60
                            + phase.getTriggerTime()
                            .getMin();

            statements.add(
                    turso.statement(
                            """
                            INSERT INTO Phases (
                                phase_id,
                                alarm_id,
                                label,
                                trigger_time
                            )
                            VALUES (?, ?, ?, ?)
                            """,

                            turso.textArg(
                                    phaseId
                            ),
                            turso.textArg(
                                    alarmId.getAlarmId()
                            ),
                            turso.textArg(
                                    phase.getLabel()
                            ),
                            turso.integerArg(
                                    triggerTime
                            )
                    )
            );


            for (
                    Action action
                    : phase.getActionsList()
            ) {

                String actionId =
                        usableId(
                                action.getId()
                                        .getActionId()
                        );

                statements.add(
                        turso.statement(
                                """
                                INSERT INTO Actions (
                                    action_id,
                                    phase_id,
                                    label,
                                    device_id,
                                    device_action_key
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """,

                                turso.textArg(
                                        actionId
                                ),
                                turso.textArg(
                                        phaseId
                                ),
                                turso.textArg(
                                        action.getLabel()
                                ),

                                turso.textArg(
                                        action.getDeviceId()
                                                .getId()
                                ),

                                turso.textArg(
                                        action.getDeviceActionKey()
                                                .getKey()
                                )
                        )
                );


                for (
                        ActionParameter parameter
                        : action.getParametersList()
                ) {
                    addParameterStatements(
                            statements,
                            actionId,
                            parameter
                    );
                }
            }
        }
    }


    // =========================================================
    // Parameters
    // =========================================================

    private void addParameterStatements(
            List<SqlStatement> statements,
            String actionId,
            ActionParameter parameter
    ) {
        String parameterId =
                usableId(
                        parameter.getParameterId()
                );

        ActionValue value =
                parameter.getValue();

        String valueType;

        JsonObject stringVal =
                turso.nullArg();

        JsonObject uint32Val =
                turso.nullArg();

        JsonObject int32Val =
                turso.nullArg();

        JsonObject boolVal =
                turso.nullArg();

        JsonObject rgbaVal =
                turso.nullArg();

        JsonObject percentageVal =
                turso.nullArg();

        JsonObject fileIdArg =
                turso.nullArg();

        JsonObject doubleVal =
                turso.nullArg();


        switch (value.getValueCase()) {

            case STRING_VAL -> {
                valueType =
                        "STRING";

                stringVal =
                        turso.textArg(
                                value.getStringVal()
                        );
            }

            case UINT32_VAL -> {
                valueType =
                        "UINT32";

                long unsigned =
                        Integer.toUnsignedLong(
                                value.getUint32Val()
                        );

                uint32Val =
                        turso.integerArg(
                                unsigned
                        );
            }

            case INT32_VAL -> {
                valueType =
                        "INT32";

                int32Val =
                        turso.integerArg(
                                value.getInt32Val()
                        );
            }

            case BOOL_VAL -> {
                valueType =
                        "BOOL";

                boolVal =
                        turso.boolArg(
                                value.getBoolVal()
                        );
            }

            case RGBA_VAL -> {
                valueType =
                        "RGBA";

                long rgba =
                        Integer.toUnsignedLong(
                                value.getRgbaVal()
                                        .getRgba()
                        );

                rgbaVal =
                        turso.integerArg(
                                rgba
                        );
            }

            case PERCENTAGE -> {
                valueType =
                        "PERCENTAGE";

                percentageVal =
                        turso.integerArg(
                                value.getPercentage()
                                        .getValue()
                        );
            }

            case FILE -> {
                valueType =
                        "FILE";

                String fileId =
                        UUID.randomUUID()
                                .toString();

                FileData file =
                        value.getFile();

                statements.add(
                        turso.statement(
                                """
                                INSERT INTO Files (
                                    file_id,
                                    filename,
                                    file_type,
                                    size_bytes,
                                    file_content
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """,

                                turso.textArg(
                                        fileId
                                ),
                                turso.textArg(
                                        file.getFilename()
                                ),
                                turso.textArg(
                                        file.getFileType()
                                ),
                                turso.integerArg(
                                        file.getSizeBytes()
                                ),

                                turso.blobArg(
                                        file.getFileContent()
                                                .toByteArray()
                                )
                        )
                );

                fileIdArg =
                        turso.textArg(
                                fileId
                        );
            }

            case DOUBLE_VAL -> {
                valueType =
                        "DOUBLE";

                doubleVal =
                        turso.floatArg(
                                value.getDoubleVal()
                        );
            }

            case VALUE_NOT_SET ->
                    throw new IllegalArgumentException(
                            "Action parameter "
                                    + parameter.getParameterKey()
                                    + " has no value"
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported ActionValue: "
                                    + value.getValueCase()
                    );
        }


        JsonObject unitsArg =
                parameter.hasUnits()
                        ? turso.textArg(
                        parameter.getUnits()
                )
                        : turso.nullArg();


        statements.add(
                turso.statement(
                        """
                        INSERT INTO ActionParameters (
                            parameter_id,
                            action_id,
                            parameter_key,
                            label,
                            units,
                            value_type,

                            string_val,
                            uint32_val,
                            int32_val,
                            bool_val,
                            rgba_val,
                            percentage_val,
                            file_id,
                            double_val
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?,
                            ?, ?, ?, ?, ?, ?, ?, ?
                        )
                        """,

                        turso.textArg(
                                parameterId
                        ),
                        turso.textArg(
                                actionId
                        ),
                        turso.textArg(
                                parameter.getParameterKey()
                        ),
                        turso.textArg(
                                parameter.getLabel()
                        ),
                        unitsArg,
                        turso.textArg(
                                valueType
                        ),

                        stringVal,
                        uint32Val,
                        int32Val,
                        boolVal,
                        rgbaVal,
                        percentageVal,
                        fileIdArg,
                        doubleVal
                )
        );
    }


    private void addDeleteOrphanFilesStatement(
            List<SqlStatement> statements
    ) {
        statements.add(
                turso.statement(
                        """
                        DELETE FROM Files
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM ActionParameters
                            WHERE ActionParameters.file_id =
                                  Files.file_id
                        )
                        """
                )
        );
    }


    // =========================================================
    // Domain Helpers
    // =========================================================

    private void validateAlarmId(
            AlarmId id
    ) {
        if (
                id == null
                        || id.getAlarmId()
                        .isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Alarm ID cannot be blank"
            );
        }
    }


    private void addDayIfPresent(
            Alarm.Builder alarm,
            JsonArray row,
            int index,
            DayOfWeek day
    ) {
        if (
                turso.boolCell(
                        row,
                        index
                )
        ) {
            alarm.addDays(
                    day
            );
        }
    }


    private boolean hasDay(
            Alarm alarm,
            DayOfWeek day
    ) {
        return alarm.getDaysList()
                .contains(
                        day
                );
    }


    private String usableId(
            String existingId
    ) {
        if (
                existingId == null
                        || existingId.isBlank()
                        || "NULL".equalsIgnoreCase(
                        existingId
                )
        ) {
            return UUID.randomUUID()
                    .toString();
        }

        return existingId;
    }
}