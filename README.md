# Rural EdTech — Learner App (Group 1)

HACKX 4.0, PS #4: **Accessible Education for Rural India**.

This repo is **Group 1's** scope only: the Android learner-facing application and its
offline foundation. It does not include the adaptive learning engine (Group 2), the P2P
learning mesh (Group 3), or the backend/teacher platform (Group 4) — see
[INTEGRATION.md](INTEGRATION.md) for how those plug in.

## Core principle

The internet is an accelerator, not a dependency. Every learner-facing operation — lessons,
quizzes, evaluation, progress, mastery, recommendations, shared-device profiles — works with
the device in airplane mode. Nothing routes through a network call on the golden path.

## Tech stack

Kotlin, Jetpack Compose, MVVM + Clean Architecture, Room (SQLite), Hilt, WorkManager,
Retrofit/OkHttp, Kotlin Coroutines/Flow, kotlinx.serialization.

## Getting started

1. Open the project root in Android Studio (Koala or newer recommended).
2. Let Gradle sync — it will download the AGP/Kotlin/Compose/Room/Hilt versions pinned in
   `gradle/libs.versions.toml`.
3. Run the `app` configuration on an emulator or device (minSdk 24 / API 24, targetSdk 34).
4. On first launch the app seeds itself with a small "Fractions" content package
   (English + Hindi) so there's something to demo immediately — see
   `core/seed/DemoContentSeeder.kt`.

## Demoing the acceptance journey (PS section 56)

1. Launch the app, pick a language, create a learner profile (no email/phone required).
2. From Home, open **Mathematics → Understanding Fractions**, read the lesson, play the
   audio block if present.
3. Take the quiz — answers are evaluated locally and mastery updates immediately.
4. Turn on airplane mode. Force-close and reopen the app.
5. Select the same learner again — progress, mastery, and the last recommendation are all
   still there, entirely from Room. No network round-trip occurred anywhere in this flow.
6. From Profile → Switch learner, create a second learner and confirm their progress is
   completely independent (PS section 57's mandatory shared-device isolation test).

## Project layout

```
app/src/main/java/com/hackx/ruraledtech/
├── core/            connectivity, datastore, DI modules, audio, security, session, work
├── data/            Room entities/DAOs, repository implementations, content package
│                    installer, learning passport, mock Group 2/3 implementations
├── domain/          pure Kotlin models, repository interfaces, integration contracts,
│                    use cases — zero Android/Room/Retrofit imports
└── feature/         Compose screens + ViewModels, one package per screen area, plus
                     the navigation graph
```

## Tests

- `app/src/test` — JVM unit tests (mastery threshold logic, quiz evaluation orchestration).
- `app/src/androidTest` — Room-backed tests, including the mandatory shared-device
  learner-isolation test (PS section 45/57).

Run unit tests: `./gradlew testDebugUnitTest`
Run instrumented tests (needs a device/emulator): `./gradlew connectedDebugAndroidTest`
