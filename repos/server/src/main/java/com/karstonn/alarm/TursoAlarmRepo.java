package com.karstonn.alarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private static <T> CompletableFuture<T> failedFuture(
            Throwable throwable
    ) {
        CompletableFuture<T> future =
                new CompletableFuture<>();

        future.completeExceptionally(throwable);

        return future;
    }

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
    public CompletableFuture<Alarm> getAlarm(AlarmId id) {
        return failedFuture(
                new UnsupportedOperationException(
                        "getAlarm not implemented yet"
                )
        );
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
            AddAlarmRequest addAlarmRequest
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return addAlarmBlocking(addAlarmRequest);

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }


    @Override
    public CompletableFuture<AlarmRequestResponse> updateAlarm(
            UpdateAlarmRequest updateRequest
    ) {
        return failedFuture(
                new UnsupportedOperationException(
                        "updateAlarm not implemented yet"
                )
        );
    }


    @Override
    public CompletableFuture<AlarmRequestResponse> removeAlarm(
            AlarmId id
    ) {
        return failedFuture(
                new UnsupportedOperationException(
                        "removeAlarm not implemented yet"
                )
        );
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

        Alarm alarm = request.getAlarm();

        AlarmId alarmId = AlarmId.newBuilder()
                .setAlarmId(UUID.randomUUID().toString())
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
    // List Alarm
    // =========================================================

    private AlarmListResponse listAlarmsBlocking()
            throws IOException {

        SqlStatement statement =
                statement(
                        """
                        SELECT
                            alarm_id,
                            label,
                            is_enabled
                        FROM Alarms;
                        """
                );

        JsonObject result =
                executeQuery(statement);

        JsonArray rows =
                result.getAsJsonArray("rows");

        AlarmListResponse.Builder response =
                AlarmListResponse.newBuilder();

        for (JsonElement rowElement : rows) {

            JsonArray row =
                    rowElement.getAsJsonArray();

            String alarmId =
                    row.get(0)
                            .getAsJsonObject()
                            .get("value")
                            .getAsString();

            String label =
                    row.get(1)
                            .getAsJsonObject()
                            .get("value")
                            .getAsString();

            boolean isEnabled =
                    !"0".equals(
                            row.get(2)
                                    .getAsJsonObject()
                                    .get("value")
                                    .getAsString()
                    );

            response.addAlarms(
                    AlarmListing.newBuilder()
                            .setId(
                                    AlarmId.newBuilder()
                                            .setAlarmId(alarmId)
                                            .build()
                            )
                            .setLabel(label)
                            .setIsEnabled(isEnabled)
                            .build()
            );
        }

        return response
                .setSuccess(true)
                .setMsg("Successfully Listed Alarms")
                .build();
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

                        textArg(alarmId.getAlarmId()),
                        textArg(alarm.getLabel()),

                        boolArg(alarm.getIsRecurring()),
                        boolArg(alarm.getIsEnabled()),

                        boolArg(hasDay(alarm, DayOfWeek.MONDAY)),
                        boolArg(hasDay(alarm, DayOfWeek.TUESDAY)),
                        boolArg(hasDay(alarm, DayOfWeek.WEDNESDAY)),
                        boolArg(hasDay(alarm, DayOfWeek.THURSDAY)),
                        boolArg(hasDay(alarm, DayOfWeek.FRIDAY)),
                        boolArg(hasDay(alarm, DayOfWeek.SATURDAY)),
                        boolArg(hasDay(alarm, DayOfWeek.SUNDAY))
                )
        );


        // -----------------------------------------------------
        // Phases
        // -----------------------------------------------------

        for (AlarmPhase phase : alarm.getAlarmPhasesList()) {

            String phaseId =
                    usableId(
                            phase.getPhaseId().getPhaseId()
                    );

            int triggerTime =
                    phase.getTriggerTime().getHour() * 60
                            + phase.getTriggerTime().getMin();

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

                            textArg(phaseId),
                            textArg(alarmId.getAlarmId()),
                            textArg(phase.getLabel()),
                            integerArg(triggerTime)
                    )
            );


            // -------------------------------------------------
            // Actions
            // -------------------------------------------------

            for (Action action : phase.getActionsList()) {

                String actionId =
                        usableId(
                                action.getId().getActionId()
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

                                textArg(actionId),
                                textArg(phaseId),
                                textArg(action.getLabel()),

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

        return statements;
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
                usableId(parameter.getParameterId());

        ActionValue value =
                parameter.getValue();

        String valueType;

        JsonObject stringVal = nullArg();
        JsonObject uint32Val = nullArg();
        JsonObject int32Val = nullArg();
        JsonObject boolVal = nullArg();
        JsonObject rgbaVal = nullArg();
        JsonObject percentageVal = nullArg();
        JsonObject fileIdArg = nullArg();
        JsonObject doubleVal = nullArg();


        switch (value.getValueCase()) {

            case STRING_VAL -> {
                valueType = "STRING";
                stringVal =
                        textArg(value.getStringVal());
            }

            case UINT32_VAL -> {
                valueType = "UINT32";

                long unsigned =
                        Integer.toUnsignedLong(
                                value.getUint32Val()
                        );

                uint32Val =
                        integerArg(unsigned);
            }

            case INT32_VAL -> {
                valueType = "INT32";

                int32Val =
                        integerArg(
                                value.getInt32Val()
                        );
            }

            case BOOL_VAL -> {
                valueType = "BOOL";

                boolVal =
                        boolArg(
                                value.getBoolVal()
                        );
            }

            case RGBA_VAL -> {
                valueType = "RGBA";

                long rgba =
                        Integer.toUnsignedLong(
                                value.getRgbaVal()
                                        .getRgba()
                        );

                rgbaVal =
                        integerArg(rgba);
            }

            case PERCENTAGE -> {
                valueType = "PERCENTAGE";

                percentageVal =
                        integerArg(
                                value.getPercentage()
                                        .getValue()
                        );
            }

            case FILE -> {
                valueType = "FILE";

                String fileId =
                        UUID.randomUUID().toString();

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

                                textArg(fileId),
                                textArg(file.getFilename()),
                                textArg(file.getFileType()),
                                integerArg(file.getSizeBytes()),

                                blobArg(
                                        file.getFileContent()
                                                .toByteArray()
                                )
                        )
                );

                fileIdArg =
                        textArg(fileId);
            }

            case DOUBLE_VAL -> {
                valueType = "DOUBLE";

                doubleVal =
                        floatArg(
                                value.getDoubleVal()
                        );
            }

            case VALUE_NOT_SET -> throw new IllegalArgumentException(
                    "Action parameter "
                            + parameter.getParameterKey()
                            + " has no value"
            );

            default -> throw new IllegalArgumentException(
                    "Unsupported ActionValue: "
                            + value.getValueCase()
            );
        }


        JsonObject unitsArg =
                parameter.hasUnits()
                        ? textArg(parameter.getUnits())
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

                        textArg(parameterId),
                        textArg(actionId),
                        textArg(parameter.getParameterKey()),
                        textArg(parameter.getLabel()),
                        unitsArg,
                        textArg(valueType),

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

        ensureSuccessful(beginResponse);

        String baton =
                requireBaton(beginResponse);


        try {

            // ---------------------------------------------
            // Execute all alarm writes
            // ---------------------------------------------

            List<JsonObject> requests =
                    new ArrayList<>();

            for (SqlStatement statement : statements) {
                requests.add(
                        executeRequest(statement)
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

            ensureSuccessful(writeResponse);


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

            ensureSuccessful(commitResponse);

        } catch (Exception e) {

            try {
                rollback(baton);

            } catch (Exception rollbackException) {
                e.addSuppressed(
                        rollbackException
                );
            }

            if (e instanceof IOException ioException) {
                throw ioException;
            }

            if (e instanceof RuntimeException runtimeException) {
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
                                executeRequest(statement),
                                closeRequest()
                        )
                );

        ensureSuccessful(response);

        JsonArray results =
                response.getAsJsonArray("results");

        return results
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("response")
                .getAsJsonObject("result");
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

        ensureSuccessful(response);
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

        for (JsonObject request : requests) {
            requestArray.add(request);
        }

        body.add(
                "requests",
                requestArray
        );


        URL url =
                URI.create(pipelineUrl)
                        .toURL();

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setRequestMethod("POST");

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

        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(20_000);

        connection.setDoOutput(true);


        byte[] requestBody =
                gson.toJson(body)
                        .getBytes(StandardCharsets.UTF_8);


        try (
                OutputStream output =
                        connection.getOutputStream()
        ) {
            output.write(requestBody);
        }


        int statusCode =
                connection.getResponseCode();

        InputStream stream =
                statusCode >= 200
                        && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();


        String responseBody =
                readAll(stream);


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
                args.add(arg);
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
                    result.get("type")
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
                response.get("baton");

        if (
                baton == null
                        || baton.isJsonNull()
                        || baton.getAsString().isBlank()
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
                response.get("baton");

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
                List.of(args)
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
                Long.toString(value)
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
                value ? 1 : 0
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
                        .encodeToString(value)
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
    // Domain helpers
    // =========================================================

    private boolean hasDay(
            Alarm alarm,
            DayOfWeek day
    ) {
        return alarm.getDaysList()
                .contains(day);
    }


    private String usableId(
            String existingId
    ) {
        if (
                existingId == null
                        || existingId.isBlank()
                        || "NULL".equalsIgnoreCase(existingId)
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
                    (line = reader.readLine())
                            != null
            ) {
                builder.append(line);
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