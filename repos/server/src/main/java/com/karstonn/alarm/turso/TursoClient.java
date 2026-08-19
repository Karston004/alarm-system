package com.karstonn.alarm.turso;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

public class TursoClient {

    private final String pipelineUrl;
    private final String authToken;

    private final Gson gson =
            new GsonBuilder()
                    .disableHtmlEscaping()
                    .create();


    // =========================================================
    // Construction
    // =========================================================

    public TursoClient(
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

        this.authToken =
                authToken;
    }


    // =========================================================
    // Query / Transaction
    // =========================================================

    public JsonObject executeQuery(
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


    public void executeTransaction(
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
    // SQL Statement / Argument Helpers
    // =========================================================

    public SqlStatement statement(
            String sql,
            JsonObject... args
    ) {
        return new SqlStatement(
                sql,
                List.of(args)
        );
    }


    public JsonObject textArg(
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


    public JsonObject integerArg(
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


    public JsonObject floatArg(
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


    public JsonObject boolArg(
            boolean value
    ) {
        return integerArg(
                value ? 1 : 0
        );
    }


    public JsonObject blobArg(
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


    public JsonObject nullArg() {
        JsonObject arg =
                new JsonObject();

        arg.addProperty(
                "type",
                "null"
        );

        return arg;
    }


    // =========================================================
    // Row Helpers
    // =========================================================

    public JsonObject cell(
            JsonArray row,
            int index
    ) {
        return row.get(index)
                .getAsJsonObject();
    }


    public boolean nullCell(
            JsonArray row,
            int index
    ) {
        return "null".equals(
                cell(row, index)
                        .get("type")
                        .getAsString()
        );
    }


    public String textCell(
            JsonArray row,
            int index
    ) {
        return cell(row, index)
                .get("value")
                .getAsString();
    }


    public long integerCell(
            JsonArray row,
            int index
    ) {
        return Long.parseLong(
                textCell(row, index)
        );
    }


    public boolean boolCell(
            JsonArray row,
            int index
    ) {
        return integerCell(
                row,
                index
        ) != 0;
    }


    public double doubleCell(
            JsonArray row,
            int index
    ) {
        return cell(row, index)
                .get("value")
                .getAsDouble();
    }


    public byte[] blobCell(
            JsonArray row,
            int index
    ) {
        return Base64.getDecoder()
                .decode(
                        cell(row, index)
                                .get("base64")
                                .getAsString()
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
                gson.toJson(body)
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
    // Turso Request Builders
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
    // Turso Response Handling
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
    // General Helpers
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
                builder.append(
                        line
                );
            }
        }

        return builder.toString();
    }
}