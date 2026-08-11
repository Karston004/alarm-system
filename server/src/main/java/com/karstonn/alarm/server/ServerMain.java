package com.karstonn.alarm.server;

import com.karstonn.alarmsystem.proto.Alarm;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ServerMain {

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0
        );

        server.createContext("/", ServerMain::handleRoot);

        server.start();

        System.out.println(
                "Alarm System server running on port " + port
        );
    }

    private static void handleRoot(
            HttpExchange exchange
    ) throws IOException {

        Alarm alarm = Alarm.getDefaultInstance();

        String message =
                "Alarm System server is online\n"
                        + "Proto loaded: "
                        + alarm.getClass().getSimpleName();

        byte[] response =
                message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", "text/plain; charset=utf-8");

        exchange.sendResponseHeaders(
                200,
                response.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {
            output.write(response);
        }
    }
}