FROM gradle:8.13-jdk21 AS build

WORKDIR /workspace

COPY cloud-settings.gradle .

COPY gradle/ ./gradle/

COPY server ./server
COPY proto ./proto
COPY repos/java ./repos/java
COPY repos/server ./repos/server

RUN gradle \
    --settings-file cloud-settings.gradle \
    :server:installDist \
    --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/server/build/install/server/ ./

ENV PORT=8080

EXPOSE 8080

CMD ["bin/server"]