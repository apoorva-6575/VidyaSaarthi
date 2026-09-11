# Architecture

## Layering

```
UI (Compose)
   ↓
ViewModel
   ↓
Use Case (domain/usecase)
   ↓
Repository interface (domain/repository, domain/integration)
   ↓
Repository implementation (data/repository) ──→ Room (data/local)
                                             └──→ Mock/real Group 2-4 implementation
```

`domain/` has no Android, Room, or Retrofit imports — it is plain Kotlin and unit-testable on
the JVM with no emulator. `data/` is the only layer that knows Room exists. `feature/` is the
only layer that knows Compose exists. Nothing skips a layer (no ViewModel touches a DAO
directly, no Composable touches a repository directly).

## Why Room is the source of truth, not the network

PS section 10/34: making the backend the primary runtime dependency would violate the whole
premise of the app. Every repository implementation in `data/repository/` reads and writes
Room first. `SyncRepositoryImpl` is the only place that knows the network exists, and it is
called opportunistically (`SyncWorker`), never synchronously from the learner's critical path.

## The single-writer rule

Each Room table has exactly one class that writes to it, regardless of which
group/implementation is calling in:

| Table | Writer |
|---|---|
| `content_packages`, `lessons`, `questions` | `ContentInstallerImpl` (called by Group 3's P2P mesh and Group 4's backend downloader alike) |
| `learners` | `LearnerRepositoryImpl`, plus `LearnerDataImporterImpl` for passport imports |
| `attempts`, `mastery_scores`, `recommendations` | `SubmitQuizAttemptUseCase` (the only orchestrator of the adaptive-learning write path) |
| `sync_events` | `SubmitQuizAttemptUseCase` and other event-producing use cases, never a UI layer |

This is why `MockLearningEngine` only *computes* mastery and never persists it — persistence
always happens in exactly one place per table, so swapping in Group 2's real engine later
can't accidentally create a second writer.

## Connectivity as a first-class state, not a scattered check

`core/connectivity/ConnectivityObserver` is the only thing in the app that calls
`ConnectivityManager`. Everything else (Home's badge, `SyncRepositoryImpl.syncNow()`)
observes `Flow<ConnectivityState>` where `ConnectivityState` is `OFFLINE | LIMITED | ONLINE`
(PS section 16). No file anywhere else calls `isOnline` ad hoc.

## Shared-device learner isolation

`core/session/CurrentLearnerManager` is the single source of truth for "who is learning."
Every learner-scoped Flow in every ViewModel derives from
`currentLearnerManager.currentLearnerId`, and every Room query that returns learner data is
parameterized by `learnerId` — never by device identity. `core/device/DeviceIdProvider` is
deliberately a separate concept from learner identity (PS section 31): one physical device,
many learners, one device id used only for sync/P2P provenance.

## The adaptive-learning loop (works with zero network)

```
Quiz answer selected
   ↓
SubmitQuizAttemptUseCase.evaluateAnswer()   — deterministic, local, no LLM
   ↓
QuizRepository.saveAttempt()                — Room write #1
   ↓
SyncRepository.enqueueEvent()               — Room write #2, durable, idempotent by event_id
   ↓
LearningEngine.processAttempt()             — Mock today; Group 2's real engine later
   ↓
ProgressRepository.upsertMastery() + RecommendationRepository.saveRecommendation()
```

`MockLearningEngine` implements the exact reference formula from the PS
(`0.50×accuracy + 0.25×recent_accuracy + 0.15×difficulty_factor + 0.10×consistency`) so this
loop is genuinely functional today, not a stub returning fake data.

## Content package pipeline (one path, three sources)

`ContentInstaller.install(packagePath)` is the single ingestion point for content, whether it
arrived via:
1. A backend download (Group 4),
2. A P2P transfer from a nearby device (Group 3), or
3. A pre-installed demo package (`DemoContentSeeder`, which bypasses checksum verification
   deliberately — a factory-preloaded package was never transferred over any wire).

`ContentInstallerImpl` verifies the SHA-256 checksum declared in `manifest.json` against the
actual bytes of `lessons/*.json` + `questions/*.json` before touching Room, so a corrupted or
tampered P2P transfer is rejected rather than silently ingested.

## Learning Passport (portable identity)

`LearnerDataExporterImpl` reads Room and serializes progress/mastery/recent-attempts into a
`LearnerExportData` blob. `LearnerDataImporterImpl` merges it back in — per-row, comparing
`lastUpdated`/`updatedAt` timestamps, never blindly overwriting — so a learner bouncing
between a mother's phone and a school tablet doesn't lose whichever device wrote last. Group
1 owns extraction/merge; Group 3 owns the actual encrypted P2P/QR transport of the blob.
