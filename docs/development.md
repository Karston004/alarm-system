# Development and builds

All Gradle commands below run **from the repository root**, in Windows PowerShell. Use `./gradlew` rather than `.\gradlew.bat` on Linux/macOS. Commands are inferred from the checked-in build configurations; **they have not been validated by executing a clean build (Project is in active development)**.

## Prerequisites

- JDK 21, or a compatible Gradle Java toolchain installation (the server and `:proto` specify Java 21).
- Android Studio / Android SDK with API 36 to build the Android app.
- Gradle wrapper is pinned to Gradle 8.13 (`gradle/wrapper/gradle-wrapper.properties`). Use the included wrapper; you do not need a globally installed Gradle.

## Android

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK (build) should be under `app/build/outputs/apk/debug/`. Alternatively, open the root project in Android Studio, select the app run configuration and run on a connected phone. Android Studio manages SDK/ADB/device installation.


```powershell
.\gradlew.bat :app:testDebugUnitTest
```

This is the conventional Android unit-test Gradle task however no tests have been made or maintained.

## Java backend

```powershell
.\gradlew.bat :server:build
.\gradlew.bat :server:run
```

`server/build.gradle` uses the Java application plugin, with main class `com.karstonn.alarm.server.ServerMain`. Its `run` task loads key/value pairs from the root `.env` if present. This is a lightweight parser, not a complete dotenv implementation (e.g. values are not shell-expanded).

The backend runs on Google Cloud Run and uses a Turso database. See the [server README](../server/README.md) for deployment details.

## Protobuf generation

The `:proto` Gradle module generates Java and gRPC classes for normal builds. Run Java/gRPC generation directly via:

```powershell
.\gradlew.bat :proto:generateProto
```

Embedded nanopb C generation is a **separate** task:

```powershell
.\gradlew.bat :proto:generateNanopb
```

For nanopb generation, `NANOPB_PLUGIN` must be set in the shell to the actual `protoc-gen-nanopb` executable location. The build script suggests installing `nanopb==0.4.9.1`. Consult [proto README](../proto/README.md) for details. The ESP32 PlatformIO script currently *consumes* generated code; it does not invoke this Gradle task automatically. **This is expected to be changed and automated**

## Docker backend

The repository Dockerfile uses a multi-stage Java 21 build, invokes Gradle with `cloud-settings.gradle`, and packages `:server:installDist`.

```powershell
docker build -t alarm-server .
```

Docker does **not** automatically read or inject your root `.env` file. Cloud Run deployment is documented in the [server README](../server/README.md).

## Clean builds and debugging

```powershell
.\gradlew.bat :server:clean :server:build
.\gradlew.bat :app:clean :app:assembleDebug
.\gradlew.bat tasks --all
```

Nanopb output is generated outside the PlatformIO source tree. Its generation and firmware integration will be documented when controller development resumes.

## Verification checklist (to complete later)

- [ ] Fresh clone: Gradle wrapper and JDK toolchain work.
- [ ] Android debug APK builds and installs.
- [ ] Protobuf Java/gRPC code generates.
- [ ] Server builds and starts with non-secret test config.
- [ ] nanopb C generation works on Windows (when embedded development resumes).
- [ ] Docker image builds and server starts.

---

*Note: Generative Ai was used to aid the development of this document.*
