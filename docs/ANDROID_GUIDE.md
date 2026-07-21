# Android app — build & run guide

The Android app lives in [`android/`](../android). It's a native Kotlin + Jetpack Compose app
(Clean Architecture + MVVM, Hilt, Retrofit, Room). It talks to the Node API.

## Prerequisites

- **Android Studio** (Ladybug 2024.2+ recommended) with **JDK 17** (bundled).
- Android SDK Platform **35** + Build-Tools (Android Studio installs these on first sync).
- A device or emulator on **Android 8.0 (API 26)** or newer.

## 1. Open the project

Android Studio → **Open** → select the `android/` folder (not the repo root).
Let Gradle sync finish. The Gradle wrapper (8.11.1) is committed, so no manual Gradle install.

## 2. Point the app at your API

Create `android/local.properties` (git-ignored) — copy from `local.properties.example`:

```
API_BASE_URL=https://<your-render-service>.onrender.com/api/v1
```

- **Production:** your Render URL (above).
- **Local server + emulator:** `API_BASE_URL=http://10.0.2.2:8080/api/v1`
  (`10.0.2.2` is the host loopback from inside the emulator).

Android Studio also writes `sdk.dir` into this file automatically.

## 3. Run

- Pick a device/emulator in the toolbar → **Run ▶** (`Shift+F10`).
- The first screen calls `GET /health` and shows **“API connected ✓”** with the service
  version and AI provider. If the API is asleep (Render free tier), the first call can take
  ~30–60 s — the app waits (70 s read timeout) then shows the result; tap **Retry** if needed.

## 4. Build an APK (to install/share)

From `android/`:

```bash
# Debug APK (no signing needed):
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Run unit tests:
./gradlew testDebugUnitTest
```

On Windows use `gradlew.bat` instead of `./gradlew`.

Install the debug APK on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 5. Release build (later — Phase 7)

A signed release AAB/APK needs a keystore (`*.jks`, git-ignored). We'll wire up
`signingConfigs` + a Play Store checklist in Phase 7. Don't commit keystores or passwords.

## Troubleshooting

- **“API_BASE_URL not set” / connects to the wrong host:** check `android/local.properties`.
- **Cleartext HTTP blocked:** only `10.0.2.2` and `localhost` allow cleartext (see
  `network_security_config.xml`); production must be HTTPS.
- **Gradle sync fails on SDK:** open **SDK Manager**, install **Android 15 (API 35)**.
