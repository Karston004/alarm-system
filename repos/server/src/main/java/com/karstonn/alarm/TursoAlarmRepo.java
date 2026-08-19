package com.karstonn.alarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TursoAlarmRepo implements AlarmRepo {

    private final String pipelineUrl;
    private final String authToken;

    private final Gson gson =
            new GsonBuilder()
                    .disableHtmlEscaping()
                    .create();


    // =========================================================
    // Construction
    // =========================================================

    public TursoAlarmRepo(
            String databaseUrl,
            String authToken
    ) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Turso database URL cannot be blank"
            );
        }

        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Turso auth token cannot be blank"
            );
        }

        this.pipelineUrl =
                normaliseDatabaseUrl(databaseUrl)
                        + "/v2/pipeline";

        this.authToken = authToken;
    }


    // =========================================================
    // AlarmRepo
    // =========================================================

    @Override
    public CompletableFuture<Alarm> getAlarm(
            AlarmId id
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAlarmBlocking(id);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<AlarmListResponse> listAlarms() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return listAlarmsBlocking();

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
                return addAlarmBlocking(request);

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
                return updateAlarmBlocking(request);

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
                return removeAlarmBlocking(id);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    // =========================================================
    // Add Alarm
    // =========================================================

    private AddAlarmResponse addAlarmBlocking(
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
                                UUID.randomUUID().toString()
                        )
                        .build();

        List<SqlStatement> statements =
                buildAlarmInsertStatements(
                        alarm,
                        alarmId
                );

        executeTransaction(statements);

        return AddAlarmResponse.newBuilder()
                .setSuccess(true)
                .setMsg("Alarm added successfully")
                .setId(alarmId)
                .build();
    }


    // =========================================================
    // List Alarms
    // =========================================================

    private AlarmListResponse listAlarmsBlocking()
            throws IOException {

        JsonObject result =
                executeQuery(
                        statement(
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
                result.getAsJsonArray("rows");

        AlarmListResponse.Builder response =
                AlarmListResponse.newBuilder();

        for (JsonElement rowElement : rows) {

            JsonArray row =
                    rowElement.getAsJsonArray();

            response.addAlarms(
                    AlarmListing.newBuilder()
                            .setId(
                                    AlarmId.newBuilder()
                                            .setAlarmId(
                                                    textCell(
                                                            row,
                                                            0
                                                    )
                                            )
                                            .build()
                            )
                            .setLabel(
                                    textCell(
                                            row,
                                            1
                                    )
                            )
                            .setIsEnabled(
                                    boolCell(
                                            row,
                                            2
                                    )
                            )
                            .build()
            );
        }

        return response
                .setSuccess(true)
                .setMsg("Successfully listed alarms")
                .build();
    }


    // =========================================================
    // Get Alarm
    // =========================================================

    private Alarm getAlarmBlocking(
            AlarmId id
    ) throws IOException {

        validateAlarmId(id);

        JsonObject result =
                executeQuery(
                        statement(
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

                                textArg(
                                        id.getAlarmId()
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray("rows");

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
                                textCell(
                                        row,
                                        0
                                )
                        )
                        .setIsRecurring(
                                boolCell(
                                        row,
                                        1
                                )
                        )
                        .setIsEnabled(
                                boolCell(
                                        row,
                                        2
                                )
                        );

        if (boolCell(row, 3)) {
            alarm.addDays(
                    DayOfWeek.MONDAY
            );
        }

        if (boolCell(row, 4)) {
            alarm.addDays(
                    DayOfWeek.TUESDAY
            );
        }

        if (boolCell(row, 5)) {
            alarm.addDays(
                    DayOfWeek.WEDNESDAY
            );
        }

        if (boolCell(row, 6)) {
            alarm.addDays(
                    DayOfWeek.THURSDAY
            );
        }

        if (boolCell(row, 7)) {
            alarm.addDays(
                    DayOfWeek.FRIDAY
            );
        }

        if (boolCell(row, 8)) {
            alarm.addDays(
                    DayOfWeek.SATURDAY
            );
        }

        if (boolCell(row, 9)) {
            alarm.addDays(
                    DayOfWeek.SUNDAY
            );
        }

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
                executeQuery(
                        statement(
                                """
                                SELECT
                                    phase_id,
                                    label,
                                    trigger_time
                                FROM Phases
                                WHERE alarm_id = ?
                                ORDER BY trigger_time, phase_id
                                """,

                                textArg(
                                        alarmId.getAlarmId()
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray("rows");

        for (JsonElement element : rows) {

            JsonArray row =
                    element.getAsJsonArray();

            String phaseId =
                    textCell(
                            row,
                            0
                    );

            long triggerTime =
                    integerCell(
                            row,
                            2
                    );

            AlarmPhase.Builder phase =
                    AlarmPhase.newBuilder()
                            .setLabel(
                                    textCell(
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
                executeQuery(
                        statement(
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

                                textArg(
                                        phaseId
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray("rows");

        for (JsonElement element : rows) {

            JsonArray row =
                    element.getAsJsonArray();

            String actionId =
                    textCell(
                            row,
                            0
                    );

            Action.Builder action =
                    Action.newBuilder()
                            .setLabel(
                                    textCell(
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
                            textCell(
                                    row,
                                    2
                            )
                    );

            action.getDeviceActionKeyBuilder()
                    .setKey(
                            textCell(
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
                executeQuery(
                        statement(
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

                                textArg(
                                        actionId
                                )
                        )
                );

        JsonArray rows =
                result.getAsJsonArray("rows");

        for (JsonElement element : rows) {

            JsonArray row =
                    element.getAsJsonArray();

            ActionParameter.Builder parameter =
                    ActionParameter.newBuilder()
                            .setParameterId(
                                    textCell(
                                            row,
                                            0
                                    )
                            )
                            .setParameterKey(
                                    textCell(
                                            row,
                                            1
                                    )
                            )
                            .setLabel(
                                    textCell(
                                            row,
                                            2
                                    )
                            );

            if (!nullCell(row, 3)) {
                parameter.setUnits(
                        textCell(
                                row,
                                3
                        )
                );
            }

            String valueType =
                    textCell(
                            row,
                            4
                    );

            ActionValue.Builder value =
                    ActionValue.newBuilder();

            switch (valueType) {

                case "STRING" ->
                        value.setStringVal(
                                textCell(
                                        row,
                                        5
                                )
                        );

                case "UINT32" ->
                        value.setUint32Val(
                                (int) integerCell(
                                        row,
                                        6
                                )
                        );

                case "INT32" ->
                        value.setInt32Val(
                                (int) integerCell(
                                        row,
                                        7
                                )
                        );

                case "BOOL" ->
                        value.setBoolVal(
                                boolCell(
                                        row,
                                        8
                                )
                        );

                case "RGBA" ->
                        value.getRgbaValBuilder()
                                .setRgba(
                                        (int) integerCell(
                                                row,
                                                9
                                        )
                                );

                case "PERCENTAGE" ->
                        value.getPercentageBuilder()
                                .setValue(
                                        (int) integerCell(
                                                row,
                                                10
                                        )
                                );

                case "DOUBLE" ->
                        value.setDoubleVal(
                                doubleCell(
                                        row,
                                        11
                                )
                        );

                case "FILE" -> {

                    if (
                            nullCell(row, 12)
                                    || nullCell(row, 13)
                                    || nullCell(row, 14)
                                    || nullCell(row, 15)
                    ) {
                        throw new IOException(
                                "FILE parameter is missing its Files row"
                        );
                    }

                    value.getFileBuilder()
                            .setFilename(
                                    textCell(
                                            row,
                                            12
                                    )
                            )
                            .setFileType(
                                    textCell(
                                            row,
                                            13
                                    )
                            )
                            .setSizeBytes(
                                    integerCell(
                                            row,
                                            14
                                    )
                            )
                            .setFileContent(
                                    ByteString.copyFrom(
                                            blobCell(
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

    private AlarmRequestResponse updateAlarmBlocking(
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

        validateAlarmId(alarmId);

        if (!alarmExists(alarmId)) {
            return AlarmRequestResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }

        List<SqlStatement> statements =
                new ArrayList<>();

        statements.add(
                statement(
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

                        textArg(
                                alarm.getLabel()
                        ),

                        boolArg(
                                alarm.getIsRecurring()
                        ),
                        boolArg(
                                alarm.getIsEnabled()
                        ),

                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.MONDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.TUESDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.WEDNESDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.THURSDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.FRIDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SATURDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SUNDAY
                                )
                        ),

                        textArg(
                                alarmId.getAlarmId()
                        )
                )
        );

        /*
         * Phases -> Actions -> ActionParameters are deleted
         * through the database's ON DELETE CASCADE rules.
         *
         * The updated child tree is then inserted from the
         * protobuf currently supplied by the client.
         */
        statements.add(
                statement(
                        """
                        DELETE FROM Phases
                        WHERE alarm_id = ?
                        """,

                        textArg(
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

        executeTransaction(
                statements
        );

        return AlarmRequestResponse.newBuilder()
                .setSuccess(true)
                .build();
    }


    // =========================================================
    // Delete Alarm
    // =========================================================

    private AlarmRequestResponse removeAlarmBlocking(
            AlarmId id
    ) throws IOException {

        validateAlarmId(id);

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
                statement(
                        """
                        DELETE FROM Alarms
                        WHERE alarm_id = ?
                        """,

                        textArg(
                                id.getAlarmId()
                        )
                )
        );

        /*
         * Files are referenced by ActionParameters rather than
         * being children of them, so remove any file rows left
         * without a parameter reference after the cascade.
         */
        addDeleteOrphanFilesStatement(
                statements
        );

        executeTransaction(
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
                executeQuery(
                        statement(
                                """
                                SELECT 1
                                FROM Alarms
                                WHERE alarm_id = ?
                                LIMIT 1
                                """,

                                textArg(
                                        id.getAlarmId()
                                )
                        )
                );

        return result
                .getAsJsonArray("rows")
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

        // -----------------------------------------------------
        // Alarm
        // -----------------------------------------------------

        statements.add(
                statement(
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

                        textArg(
                                alarmId.getAlarmId()
                        ),
                        textArg(
                                alarm.getLabel()
                        ),

                        boolArg(
                                alarm.getIsRecurring()
                        ),
                        boolArg(
                                alarm.getIsEnabled()
                        ),

                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.MONDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.TUESDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.WEDNESDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.THURSDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.FRIDAY
                                )
                        ),
                        boolArg(
                                hasDay(
                                        alarm,
                                        DayOfWeek.SATURDAY
                                )
                        ),
                        boolArg(
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

        // -----------------------------------------------------
        // Phases
        // -----------------------------------------------------

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
                    statement(
                            """
                            INSERT INTO Phases (
                                phase_id,
                                alarm_id,
                                label,
                                trigger_time
                            )
                            VALUES (?, ?, ?, ?)
                            """,

                            textArg(
                                    phaseId
                            ),
                            textArg(
                                    alarmId.getAlarmId()
                            ),
                            textArg(
                                    phase.getLabel()
                            ),
                            integerArg(
                                    triggerTime
                            )
                    )
            );


            // -------------------------------------------------
            // Actions
            // -------------------------------------------------

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
                        statement(
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

                                textArg(
                                        actionId
                                ),
                                textArg(
                                        phaseId
                                ),
                                textArg(
                                        action.getLabel()
                                ),

                                textArg(
                                        action.getDeviceId()
                                                .getId()
                                ),

                                textArg(
                                        action.getDeviceActionKey()
                                                .getKey()
                                )
                        )
                );


                // ---------------------------------------------
                // Parameters
                // ---------------------------------------------

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
                nullArg();

        JsonObject uint32Val =
                nullArg();

        JsonObject int32Val =
                nullArg();

        JsonObject boolVal =
                nullArg();

        JsonObject rgbaVal =
                nullArg();

        JsonObject percentageVal =
                nullArg();

        JsonObject fileIdArg =
                nullArg();

        JsonObject doubleVal =
                nullArg();


        switch (value.getValueCase()) {

            case STRING_VAL -> {
                valueType =
                        "STRING";

                stringVal =
                        textArg(
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
                        integerArg(
                                unsigned
                        );
            }

            case INT32_VAL -> {
                valueType =
                        "INT32";

                int32Val =
                        integerArg(
                                value.getInt32Val()
                        );
            }

            case BOOL_VAL -> {
                valueType =
                        "BOOL";

                boolVal =
                        boolArg(
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
                        integerArg(
                                rgba
                        );
            }

            case PERCENTAGE -> {
                valueType =
                        "PERCENTAGE";

                percentageVal =
                        integerArg(
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
                        statement(
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

                                textArg(
                                        fileId
                                ),
                                textArg(
                                        file.getFilename()
                                ),
                                textArg(
                                        file.getFileType()
                                ),
                                integerArg(
                                        file.getSizeBytes()
                                ),

                                blobArg(
                                        file.getFileContent()
                                                .toByteArray()
                                )
                        )
                );

                fileIdArg =
                        textArg(
                                fileId
                        );
            }

            case DOUBLE_VAL -> {
                valueType =
                        "DOUBLE";

                doubleVal =
                        floatArg(
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
                        ? textArg(
                        parameter.getUnits()
                )
                        : nullArg();


        statements.add(
                statement(
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

                        textArg(
                                parameterId
                        ),
                        textArg(
                                actionId
                        ),
                        textArg(
                                parameter.getParameterKey()
                        ),
                        textArg(
                                parameter.getLabel()
                        ),
                        unitsArg,
                        textArg(
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
                statement(
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
    // Transaction
    // =========================================================

    private void executeTransaction(
            List<SqlStatement> statements
    ) throws IOException {

        /*
         * Foreign keys are enabled per connection.
         *
         * Do this before BEGIN because SQLite does not allow
         * changing foreign_keys while already in a transaction.
         */

        JsonObject beginResponse =
                sendPipeline(
                        null,
                        List.of(
                                executeRequest(
                                        new SqlStatement(
                                                "PRAGMA foreign_keys = ON",
                                                List.of()
                                        )
                                ),
                                executeRequest(
                                        new SqlStatement(
                                                "BEGIN",
                                                List.of()
                                        )
                                )
                        )
                );

        ensureSuccessful(
                beginResponse
        );

        String baton =
                requireBaton(
                        beginResponse
                );


        try {

            // ---------------------------------------------
            // Execute all writes
            // ---------------------------------------------

            List<JsonObject> requests =
                    new ArrayList<>();

            for (
                    SqlStatement statement
                    : statements
            ) {
                requests.add(
                        executeRequest(
                                statement
                        )
                );
            }

            JsonObject writeResponse =
                    sendPipeline(
                            baton,
                            requests
                    );

            baton =
                    responseBaton(
                            writeResponse,
                            baton
                    );

            ensureSuccessful(
                    writeResponse
            );


            // ---------------------------------------------
            // Commit
            // ---------------------------------------------

            JsonObject commitResponse =
                    sendPipeline(
                            baton,
                            List.of(
                                    executeRequest(
                                            new SqlStatement(
                                                    "COMMIT",
                                                    List.of()
                                            )
                                    ),
                                    closeRequest()
                            )
                    );

            ensureSuccessful(
                    commitResponse
            );

        } catch (Exception e) {

            try {
                rollback(
                        baton
                );

            } catch (Exception rollbackException) {
                e.addSuppressed(
                        rollbackException
                );
            }

            if (e instanceof IOException ioException) {
                throw ioException;
            }

            if (
                    e instanceof RuntimeException
                            runtimeException
            ) {
                throw runtimeException;
            }

            throw new IOException(
                    "Turso transaction failed",
                    e
            );
        }
    }


    private JsonObject executeQuery(
            SqlStatement statement
    ) throws IOException {

        JsonObject response =
                sendPipeline(
                        null,
                        List.of(
                                executeRequest(
                                        statement
                                ),
                                closeRequest()
                        )
                );

        ensureSuccessful(
                response
        );

        JsonArray results =
                response.getAsJsonArray(
                        "results"
                );

        return results
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject(
                        "response"
                )
                .getAsJsonObject(
                        "result"
                );
    }


    private void rollback(
            String baton
    ) throws IOException {

        if (baton == null) {
            return;
        }

        JsonObject response =
                sendPipeline(
                        baton,
                        List.of(
                                executeRequest(
                                        new SqlStatement(
                                                "ROLLBACK",
                                                List.of()
                                        )
                                ),
                                closeRequest()
                        )
                );

        ensureSuccessful(
                response
        );
    }


    // =========================================================
    // Turso HTTP
    // =========================================================

    private JsonObject sendPipeline(
            String baton,
            List<JsonObject> requests
    ) throws IOException {

        JsonObject body =
                new JsonObject();

        if (baton != null) {
            body.addProperty(
                    "baton",
                    baton
            );
        }

        JsonArray requestArray =
                new JsonArray();

        for (
                JsonObject request
                : requests
        ) {
            requestArray.add(
                    request
            );
        }

        body.add(
                "requests",
                requestArray
        );


        URL url =
                URI.create(
                                pipelineUrl
                        )
                        .toURL();

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setRequestMethod(
                "POST"
        );

        connection.setRequestProperty(
                "Authorization",
                "Bearer " + authToken
        );

        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        connection.setRequestProperty(
                "Accept",
                "application/json"
        );

        connection.setConnectTimeout(
                10_000
        );

        connection.setReadTimeout(
                20_000
        );

        connection.setDoOutput(
                true
        );


        byte[] requestBody =
                gson.toJson(
                                body
                        )
                        .getBytes(
                                StandardCharsets.UTF_8
                        );


        try (
                OutputStream output =
                        connection.getOutputStream()
        ) {
            output.write(
                    requestBody
            );
        }


        int statusCode =
                connection.getResponseCode();

        InputStream stream =
                statusCode >= 200
                        && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();


        String responseBody =
                readAll(
                        stream
                );


        connection.disconnect();


        if (
                statusCode < 200
                        || statusCode >= 300
        ) {
            throw new IOException(
                    "Turso returned HTTP "
                            + statusCode
                            + ": "
                            + responseBody
            );
        }


        JsonElement json =
                JsonParser.parseString(
                        responseBody
                );

        if (!json.isJsonObject()) {
            throw new IOException(
                    "Invalid Turso response: "
                            + responseBody
            );
        }

        return json.getAsJsonObject();
    }


    // =========================================================
    // Turso request builders
    // =========================================================

    private JsonObject executeRequest(
            SqlStatement statement
    ) {
        JsonObject request =
                new JsonObject();

        request.addProperty(
                "type",
                "execute"
        );


        JsonObject stmt =
                new JsonObject();

        stmt.addProperty(
                "sql",
                statement.sql()
        );


        if (!statement.args().isEmpty()) {

            JsonArray args =
                    new JsonArray();

            for (
                    JsonObject arg
                    : statement.args()
            ) {
                args.add(
                        arg
                );
            }

            stmt.add(
                    "args",
                    args
            );
        }


        request.add(
                "stmt",
                stmt
        );

        return request;
    }


    private JsonObject closeRequest() {
        JsonObject request =
                new JsonObject();

        request.addProperty(
                "type",
                "close"
        );

        return request;
    }


    // =========================================================
    // Turso response handling
    // =========================================================

    private void ensureSuccessful(
            JsonObject response
    ) throws IOException {

        JsonArray results =
                response.getAsJsonArray(
                        "results"
                );

        if (results == null) {
            throw new IOException(
                    "Turso response contains no results: "
                            + response
            );
        }


        for (JsonElement element : results) {

            JsonObject result =
                    element.getAsJsonObject();

            String type =
                    result.get(
                                    "type"
                            )
                            .getAsString();

            if (!"ok".equals(type)) {
                throw new IOException(
                        "Turso query failed: "
                                + result
                );
            }
        }
    }


    private String requireBaton(
            JsonObject response
    ) throws IOException {

        JsonElement baton =
                response.get(
                        "baton"
                );

        if (
                baton == null
                        || baton.isJsonNull()
                        || baton.getAsString()
                        .isBlank()
        ) {
            throw new IOException(
                    "Turso did not return a connection baton"
            );
        }

        return baton.getAsString();
    }


    private String responseBaton(
            JsonObject response,
            String previousBaton
    ) {
        JsonElement baton =
                response.get(
                        "baton"
                );

        if (
                baton == null
                        || baton.isJsonNull()
        ) {
            return previousBaton;
        }

        return baton.getAsString();
    }


    // =========================================================
    // SQL statement helpers
    // =========================================================

    private SqlStatement statement(
            String sql,
            JsonObject... args
    ) {
        return new SqlStatement(
                sql,
                List.of(
                        args
                )
        );
    }


    private JsonObject textArg(
            String value
    ) {
        JsonObject arg =
                new JsonObject();

        arg.addProperty(
                "type",
                "text"
        );

        arg.addProperty(
                "value",
                value
        );

        return arg;
    }


    private JsonObject integerArg(
            long value
    ) {
        JsonObject arg =
                new JsonObject();

        arg.addProperty(
                "type",
                "integer"
        );

        /*
         * Turso represents integer parameter values as
         * strings in the JSON protocol.
         */
        arg.addProperty(
                "value",
                Long.toString(
                        value
                )
        );

        return arg;
    }


    private JsonObject floatArg(
            double value
    ) {
        JsonObject arg =
                new JsonObject();

        arg.addProperty(
                "type",
                "float"
        );

        arg.addProperty(
                "value",
                value
        );

        return arg;
    }


    private JsonObject boolArg(
            boolean value
    ) {
        return integerArg(
                value
                        ? 1
                        : 0
        );
    }


    private JsonObject blobArg(
            byte[] value
    ) {
        JsonObject arg =
                new JsonObject();

        arg.addProperty(
                "type",
                "blob"
        );

        arg.addProperty(
                "base64",
                Base64.getEncoder()
                        .encodeToString(
                                value
                        )
        );

        return arg;
    }


    private JsonObject nullArg() {
        JsonObject arg =
                new JsonObject();

        arg.addProperty(
                "type",
                "null"
        );

        return arg;
    }


    // =========================================================
    // Turso row helpers
    // =========================================================

    private JsonObject cell(
            JsonArray row,
            int index
    ) {
        return row.get(
                        index
                )
                .getAsJsonObject();
    }


    private boolean nullCell(
            JsonArray row,
            int index
    ) {
        return "null".equals(
                cell(
                        row,
                        index
                )
                        .get(
                                "type"
                        )
                        .getAsString()
        );
    }


    private String textCell(
            JsonArray row,
            int index
    ) {
        return cell(
                row,
                index
        )
                .get(
                        "value"
                )
                .getAsString();
    }


    private long integerCell(
            JsonArray row,
            int index
    ) {
        return Long.parseLong(
                textCell(
                        row,
                        index
                )
        );
    }


    private boolean boolCell(
            JsonArray row,
            int index
    ) {
        return integerCell(
                row,
                index
        ) != 0;
    }


    private double doubleCell(
            JsonArray row,
            int index
    ) {
        return cell(
                row,
                index
        )
                .get(
                        "value"
                )
                .getAsDouble();
    }


    private byte[] blobCell(
            JsonArray row,
            int index
    ) {
        return Base64.getDecoder()
                .decode(
                        cell(
                                row,
                                index
                        )
                                .get(
                                        "base64"
                                )
                                .getAsString()
                );
    }


    // =========================================================
    // Domain helpers
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


    // =========================================================
    // General helpers
    // =========================================================

    private String normaliseDatabaseUrl(
            String databaseUrl
    ) {
        String url =
                databaseUrl.trim();

        if (url.startsWith("libsql://")) {
            url =
                    "https://"
                            + url.substring(
                            "libsql://".length()
                    );

        } else if (url.startsWith("turso://")) {
            url =
                    "https://"
                            + url.substring(
                            "turso://".length()
                    );
        }

        while (url.endsWith("/")) {
            url =
                    url.substring(
                            0,
                            url.length() - 1
                    );
        }

        return url;
    }


    private String readAll(
            InputStream stream
    ) throws IOException {

        if (stream == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {
            String line;

            while (
                    (
                            line =
                                    reader.readLine()
                    ) != null
            ) {
                builder.append(
                        line
                );
            }
        }

        return builder.toString();
    }


    // =========================================================
    // Internal Types
    // =========================================================

    private record SqlStatement(
            String sql,
            List<JsonObject> args
    ) {
    }
}