# Heartbeats

A standalone **Wear OS** app that **pauses your music when your heart rate drops below your
target zone** and **resumes it when you recover**. No phone companion, no accounts, no cloud —
just the watch.

- Pick your zone (Fat Burn / Cardio / Peak), computed from your age (`maxHr = 220 − age`).
- A configurable warmup period where music plays freely and the zone isn't enforced.
- Heart rate via **Health Services** (`ExerciseClient`); music via **`MediaSessionManager`**
  (works with Spotify, YT Music, Pocket Casts, … — nothing is hardcoded).
- Differentiated haptics for below / above / back-in-zone and the warmup countdown.
- Runs as a `health` foreground service so sensors and media control stay alive with the screen off.

## Tech stack

Kotlin · Jetpack Compose for Wear OS (`androidx.wear.compose`) · MVVM (`ViewModel` + `StateFlow`) ·
Hilt · Coroutines/Flow · `androidx.health:health-services-client` · DataStore (Preferences) ·
Gradle Kotlin DSL with a version catalog. `minSdk 30` (Wear OS 3+), `targetSdk 36` (Wear OS 6).

## Build & run on a Wear OS emulator

Prerequisites: a recent Android Studio, **JDK 17+**, and the **Android SDK with platform 36**
installed (also accept the SDK licenses). Create `local.properties` with `sdk.dir=/path/to/Android/Sdk`
(Android Studio does this for you).

1. Create a **Wear OS emulator**, API 30 or newer (Tools → Device Manager → Create device →
   Wear OS Round). API 33+ recommended so Health Services synthetic data and the latest
   permission model behave as on a device.
2. Build & install:
   ```bash
   ./gradlew :app:installDebug
   ```
   or just press Run in Android Studio with the emulator selected.
3. Launch **Heartbeats**. First run walks you through age → zone → warmup length.

> The emulator has no real heart-rate sensor — feed it synthetic data (below).

## Grant Notification Listener access (the trickiest step)

To control whatever media app is playing, Heartbeats needs **Notification access** (this is what
unlocks `MediaSessionManager.getActiveSessions()` on Wear OS — Heartbeats never reads notification
content). The app shows a shortcut, or grant it manually:

- In the app: **Settings → "Music access"** (or the hint on the idle screen) → opens the system screen → enable **Heartbeats**.
- Via system: **Settings → Apps & notifications → Special app access → Notification access** → enable **Heartbeats**.
- Via adb (handy on the emulator):
  ```bash
  adb shell cmd notification allow_listener com.heartbeats/com.heartbeats.media.HeartbeatsNotificationListenerService
  ```

If it isn't granted, the workout still runs (heart rate, warmup, haptics) — it just can't pause music.

## Test with synthetic heart-rate data

Health Services on the emulator (and Wear devices) can emit synthetic heart rate:

```bash
# Switch Health Services to synthetic providers
adb shell am broadcast -a "whs.USE_SYNTHETIC_PROVIDERS" com.google.android.wearable.healthservices

# Start a synthetic "running" session (heart rate ramps into a workout range)
adb shell am broadcast -a "whs.synthetic.user.START_RUNNING" com.google.android.wearable.healthservices

# ... walk it down / stop to see the zone drop and the music pause:
adb shell am broadcast -a "whs.synthetic.user.START_WALKING" com.google.android.wearable.healthservices
adb shell am broadcast -a "whs.synthetic.user.STOP_EXERCISE" com.google.android.wearable.healthservices

# Back to real sensors
adb shell am broadcast -a "whs.USE_SENSOR_PROVIDERS" com.google.android.wearable.healthservices
```

Suggested manual QA loop: start a media app → start a Heartbeats workout → run synthetic data →
verify the warmup countdown + 30s/10s/complete haptics, no enforcement during warmup, music pausing
~10s after the rate falls below the zone and resuming ~5s after it recovers, an above-zone buzz with
no music change, and the "check watch fit" hint when availability goes unavailable.

## Permissions matrix

| Permission | API 30 | API 33 | API 36 | Why |
|---|---|---|---|---|
| `android.permission.BODY_SENSORS` (`maxSdkVersion="35"`) | runtime | runtime | — (replaced) | Heart rate on older platforms |
| `android.permission.health.READ_HEART_RATE` | — | — | runtime | Heart rate on the new health permission model |
| `android.permission.ACTIVITY_RECOGNITION` | runtime | runtime | runtime | Exercise session |
| `android.permission.POST_NOTIFICATIONS` | — | runtime | runtime | Foreground-service notification |
| `android.permission.FOREGROUND_SERVICE` | install | install | install | Run the workout service |
| `android.permission.FOREGROUND_SERVICE_HEALTH` | — | — | install (enforced) | `health`-typed foreground service |
| `android.permission.WAKE_LOCK` | install | install | install | Keep working with screen off / ambient |
| `android.permission.VIBRATE` | install | install | install | Haptics |
| Notification Listener access | special access | special access | special access | See active media sessions |

Heart-rate permission requested at runtime is `BODY_SENSORS` on API < 36 and
`android.permission.health.READ_HEART_RATE` on API 36+.

## Quality gates

`./gradlew check` runs **`test` + `lint` + `spotlessCheck`** — all three must pass.

- **Unit tests** (`./gradlew test`): pure business logic only — `ComputeZoneRangeUseCase`,
  `ZoneStatusClassifier`, `WarmupTimerUseCase`, `ShouldPauseMusicUseCase`, `SettingsRepository`.
  Composables, the Health Services / media wrappers and Hilt wiring are out of scope for v1.
- **Lint** (`./gradlew lintDebug`): config in `app/lint.xml` — Wear correctness rules are errors,
  known Wear-Compose false positives are silenced, real Compose perf rules and `MissingPermission`
  / `ForegroundServiceType` / `NewApi` stay enabled. Build fails on **new** issues only, measured
  against `app/lint-baseline.xml`. The committed baseline is empty; regenerate it with
  `./gradlew :app:updateLintBaseline`. If the baseline ever grows large, something is misconfigured.
  If a future AGP renames a lint issue id and reports "unknown issue id", remove that line from
  `lint.xml` — don't fight the tool. Inline suppressions must be `@Suppress("LintId")` with a
  comment explaining why — never global.
- **Spotless** (`./gradlew spotlessCheck`): ktlint over all Kotlin (and the Gradle scripts);
  `./gradlew spotlessApply` to fix.
- **Compose compiler metrics**: written to `app/build/compose_compiler/` in debug builds — check
  for unstable parameters.

## Architecture

```
com.heartbeats
├── data/repository/SettingsRepository      DataStore-backed settings; first launch = no age key
├── domain/model/…                          ZonePreset, ZoneRange, WorkoutState, WarmupDuration, Haptic*
├── domain/usecase/
│   ├── ComputeZoneRangeUseCase             age + preset -> BPM range (220-age)
│   ├── ZoneStatusClassifier                BPM -> BELOW / IN_ZONE / ABOVE (inclusive bounds)
│   ├── WarmupTimerUseCase                  countdown flow: ticks, 30s/10s markers, completion, skip
│   └── ShouldPauseMusicUseCase             debounced pause/resume state machine (10s below / 5s recover)
├── health/HealthServicesManager            wraps ExerciseClient; ExerciseGoalBuilder builds the below-zone goal
├── media/MediaControllerManager            MediaSessionManager pause/resume; HeartbeatsNotificationListenerService
├── haptics/HapticsManager                  VibrationEffect.createWaveform patterns
├── service/WorkoutService + WorkoutController   health foreground service; process-wide state + commands
├── di/AppModule                            provides DataStore + ShouldPauseMusicUseCase
└── ui/                                     setup / main (idle, warmup, active) / settings; Wear Compose theme
```

The on-device Health Services `ExerciseGoal` is a battery-efficient trigger; `ShouldPauseMusicUseCase`
is the authoritative, unit-tested debounce.

## Privacy & safety

- Heart-rate values are treated as sensitive health data: they are **never logged or sent anywhere**
  (no analytics) and don't appear in the notification.
- The `maxHr = 220 − age` estimate is rough. If you take heart-rate-affecting medication
  (e.g. beta blockers), don't rely on it — there's a note in Settings.

## Not in v1

No phone companion, no workout history/stats, no custom BPM ranges, no multiple workout types,
no accounts/cloud, no "above zone" music penalty (haptic warning only), no social/gamification,
no Compose UI tests (deferred to v2). Ambient mode is basic (the system dims the app; HR keeps
streaming via the foreground service).

## Note on this checkout

This project was generated in an environment **without the Android SDK** and without network access
to Google's Maven repository, so the Android build, lint and unit tests **could not be executed
here** — verify them on a machine/CI with the Android SDK + network (`./gradlew check`,
`./gradlew assembleDebug`). Dependency versions in `gradle/libs.versions.toml` reflect the latest
stable releases known at authoring time; bump as needed. `androidx.health:health-services-client`
has no stable release line yet — the alpha pinned here is the current one. The exact Wear Compose
Material / Health Services API surface may need minor adjustments against the resolved versions.
