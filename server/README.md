# Java Server

The Java backend manages persistent storage, device registration, controller synchronisation, and alarm scheduling data.

The server exposes:
- **gRPC APIs** for communication with the Android application.
- **HTTP APIs** for communication with embedded controllers.
- **MQTT notifications** to inform controllers when they should synchronise with the server.

**Main class:** `com.karstonn.alarm.server.ServerMain`

## Prerequisites

See the [development guide](../docs/development.md) for shared build requirements. Local execution requires access to the configured database and MQTT broker; Cloud Run deployment additionally requires the Google Cloud CLI and suitable permissions. Docker is optional.

All commands below should be executed from the **monorepo root**.

## Local Development

### Build

Compile the Java server using Gradle:

```powershell
.\gradlew.bat :server:build
```

### Run

Start the server locally:

```powershell
.\gradlew.bat :server:run
```

The `run` task loads environment variables from the root `.env` file through configuration in `server/build.gradle`.

The loader supports `KEY=VALUE` pairs, ignores empty lines and comments, and splits values at the first `=`.

## Docker

Build the server's Docker image:

```powershell
docker build -t alarm-server .
```

The root Dockerfile uses a multi-stage build:

1. Builds the Java server using Gradle and `cloud-settings.gradle`.
2. Packages the server using `:server:installDist`.
3. Copies the distribution into a Java 21 runtime image.

The Docker container does not automatically inherit variables loaded by the local Gradle `run` task. Environment variables must be supplied separately.

## Google Cloud Run Deployment

The backend is hosted on Google Cloud Run in the **`europe-west2` (London)** region.

### Initial Setup

Before deploying:

1. Install and authenticate the Google Cloud CLI.
2. Select the appropriate Google Cloud project.
3. Configure the required environment variables in the root `.env` file.
4. Ensure the Cloud Run and Cloud Build APIs are enabled and the necessary permissions are configured.

### Deploy

Run from the monorepo root using PowerShell:

```powershell
gcloud run deploy alarm-system-server `
    --source . `
    --region europe-west2 `
    --env-vars-file .env `
    --use-http2
```

The deployment builds the server from the root Dockerfile and deploys it to Cloud Run.

The `--use-http2` flag enables HTTP/2 between Cloud Run and the container, which is required for the server's gRPC communication.

## Configuration and Security

Configuration is provided through environment variables.

- The root `.env` file is excluded from version control.
- Never commit credentials, API keys, or database authentication tokens.
- Local Gradle execution and Cloud Run deployment have separate environment configuration mechanisms.
- For production credentials, Google Cloud Secret Manager is preferable to storing secrets in deployment files.

---

*Note: Generative AI was used to assist with the development of this documentation.*