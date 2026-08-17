package com.karstonn.alarm.server;

import com.karstonn.alarm.AlarmRepo;
import com.karstonn.alarm.TursoAlarmRepo;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ServerMain {

    public static void main(String[] args)
            throws IOException, InterruptedException {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        String tursoDatabaseUrl =
                System.getenv("TURSO_DATABASE_URL");

        String tursoAuthToken =
                System.getenv("TURSO_AUTH_TOKEN");

        if (tursoDatabaseUrl == null || tursoDatabaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "TURSO_DATABASE_URL environment variable is not set"
            );
        }

        if (tursoAuthToken == null || tursoAuthToken.isBlank()) {
            throw new IllegalStateException(
                    "TURSO_AUTH_TOKEN environment variable is not set"
            );
        }

        AlarmRepo alarmRepo =
                new TursoAlarmRepo(
                        tursoDatabaseUrl,
                        tursoAuthToken
                );

        AlarmRepoServiceImpl alarmService =
                new AlarmRepoServiceImpl(alarmRepo);

        Server server = NettyServerBuilder
                .forPort(port)
                .addService(alarmService)
                .build()
                .start();

        System.out.println(
                "Alarm System gRPC server running on port " + port
        );

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println(
                            "Shutting down Alarm System server"
                    );

                    try {
                        server.shutdown()
                                .awaitTermination(
                                        10,
                                        TimeUnit.SECONDS
                                );
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
        );

        server.awaitTermination();
    }
}