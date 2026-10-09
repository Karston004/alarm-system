# Android app

The Android application is module `:app` and uses the Android Gradle plugin. The checked-in configuration targets SDK 36, with minimum SDK 26.

Open the **repository root** in Android Studio (not just this subfolder), allow Gradle sync, connect an Android phone with USB debugging enabled, then use **Run**.

From PowerShell at the repository root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Output APK: `app/build/outputs/apk/debug/`.

`app` depends on `:proto`, `:repos:java`, `:repos:android` and `:repos:grpc`, so Gradle handles their required build steps. These instructions have not yet been checked against a clean checkout.

For prerequisites and backend build information, see [development instructions](../docs/development.md).

---
*Note: Generative Ai was used to aid the development of this document.*
