# Integration Guide — Groups 2, 3, 4

Every seam below is a Kotlin interface in `domain/integration/` or `domain/repository/`,
bound to a mock implementation in `core/di/IntegrationModule.kt`. To integrate your real
implementation: implement the interface in your own package under `data/`, then change the
one `@Binds` line in `IntegrationModule.kt` (or `RepositoryModule.kt`) that points to it. No
other file in the app should need to change.

## Group 2 — Adaptive Learning, Content & Voice

### `LearningEngine` (`domain/integration/LearningEngine.kt`)

```kotlin
interface LearningEngine {
    suspend fun processAttempt(learnerId: String, attempt: Attempt): LearningResult
    suspend fun getRecommendation(learnerId: String): Recommendation?
}
```

Called from `SubmitQuizAttemptUseCase` immediately after an attempt is saved to Room. Your
implementation should **compute only** — do not write to Room yourself. The use case persists
whatever `LearningResult.updatedMastery` / `.recommendation` you return, via
`ProgressRepository` / `RecommendationRepository`. This keeps exactly one writer per table
regardless of which `LearningEngine` is wired in (see ARCHITECTURE.md's single-writer rule).

Reference implementation to replace: `data/mock/MockLearningEngine.kt` — it already
implements the PS's reference mastery formula, so you can compare your engine's output
against it during integration.

### Content package schema

Group 1 owns the Room-side schema, but the **on-disk content-package format** is a shared
contract you author against: `manifest.json` / `lessons/*.json` / `questions/*.json` /
`checksums.json`, documented field-by-field in
`data/contentpackage/dto/PackageFileDtos.kt`. Whatever you generate must be readable by
`ContentPackageReader` unmodified — coordinate before changing field names or adding block
types beyond `TEXT | IMAGE | AUDIO | VIDEO | EXAMPLE | CALLOUT` (unrecognized types render as
`ContentBlock.Unsupported` rather than crashing, so you can add new ones incrementally).

### `OfflineSpeechRecognizer` (`domain/integration/Voice.kt`)

```kotlin
interface OfflineSpeechRecognizer {
    fun listen(languageTag: String): Flow<VoiceIntent>
    fun stop()
}
```

`VoiceIntent` is the closed set from PS section 27: `START_LESSON, NEXT, BACK, REPEAT,
EXPLAIN, HELP, PAUSE, SUBMIT, UNKNOWN`. Replace `data/mock/MockOfflineSpeechRecognizer.kt`
with your Vosk/ONNX-backed implementation; the UI layer (`VoiceController`, once built) only
ever consumes the `Flow<VoiceIntent>`, never a raw transcript.

## Group 3 — P2P Learning Mesh & Learning Passport

### `ContentInstaller` (`domain/integration/ContentInstaller.kt`)

```kotlin
interface ContentInstaller {
    suspend fun install(packagePath: String): InstallResult
    suspend fun remove(packageId: String, version: Int)
    suspend fun validate(packagePath: String): ValidationResult
}
```

This is your entry point once a package's bytes have landed on local storage by whatever
transport you own (Nearby Connections / Wi-Fi Direct / BLE). Call `validate()` first if you
want a lightweight manifest sanity check before committing to a full transfer; call
`install()` once the full package directory is present. `InstallResult.ChecksumMismatch`
means the transfer was corrupted — surface that back into your retry logic, don't retry
`install()` blindly on the same bytes.

**Do not write to `content_packages`, `lessons`, or `questions` tables directly** — see
ARCHITECTURE.md's single-writer rule. `ContentInstallerImpl` is already implemented and
production-ready from Group 1's side; you should not need to modify it, only call it.

### `LearnerDataExporter` / `LearnerDataImporter` (`domain/integration/LearnerDataPortability.kt`)

```kotlin
interface LearnerDataExporter { suspend fun exportLearnerData(learnerId: String): LearnerExportData }
interface LearnerDataImporter { suspend fun importLearnerData(data: LearnerExportData): ImportResult }
```

Group 1's implementations (`LearnerDataExporterImpl` / `LearnerDataImporterImpl`) already
handle extraction and timestamp-aware merge. Your job is purely transport: take the
`LearnerExportData` (a serializable data class — see `domain/model/LearnerExportData.kt`),
encrypt it, move it device-to-device (P2P or QR-assisted pairing), decrypt it on the
receiving end, and call `LearnerDataImporter.importLearnerData()`. Never build your own Room
writes for learner progress/mastery in the P2P layer.

### Peer manifest exchange

Your peer discovery/manifest-exchange protocol should query
`ContentRepository.getInstalledPackages()` (via `ObserveInstalledPackagesUseCase` if you need
a reactive `Flow`) to know what's already on this device before deciding what to request from
a peer — never transfer a package whose `checksum` already matches what's installed.

## Group 4 — Teacher Platform, Backend & Synchronisation

### `SyncRepository` (`domain/repository/SyncRepository.kt`)

```kotlin
interface SyncRepository {
    suspend fun enqueueEvent(event: SyncEvent)
    suspend fun getPendingEvents(): List<SyncEvent>
    fun observePendingCount(): Flow<Int>
    suspend fun markSynced(eventIds: List<String>)
    suspend fun syncNow(): SyncOutcome
}
```

`SyncRepositoryImpl.syncNow()` currently just marks pending events as synced locally once
connectivity is available (a placeholder for your real upload). Replace the body of
`syncNow()` to actually `POST /sync/events` against your FastAPI backend, honoring the
existing event shape in `domain/model/SyncEvent.kt`. **Idempotency matters**: `event_id` is
already a stable UUID generated once at creation time (`IdGenerator.eventId()`) — your
backend should dedupe on it so a retried upload never double-counts a learning event.

`SyncWorker` (`core/work/SyncWorker.kt`) already schedules this opportunistically via
WorkManager with network constraints + exponential backoff — you should not need to touch
WorkManager scheduling, only the upload logic inside `SyncRepositoryImpl`.

### Content downloads

`ContentUpdateWorker` (`core/work/ContentUpdateWorker.kt`) is currently a no-op placeholder
for your `GET /content/manifest` / `GET /content/{package_id}` calls. Once you download a
package's bytes to local storage, call the same `ContentInstaller.install()` Group 3 uses —
this keeps backend downloads and P2P transfers on one ingestion path.

### Reading learner/progress data for teacher analytics

Don't add new Room queries directly against Group 1's DAOs from teacher-side code. Instead,
consume `LearnerRepository`, `ProgressRepository`, and `SyncRepository` (all in
`domain/repository/`) the same way Group 1's own ViewModels do. If you need an aggregation
Group 1 doesn't expose yet (e.g. "all attempts across all learners in a device"), ask for a
new method to be added to the relevant repository interface rather than querying Room
directly — this keeps the single-writer/single-reader-path discipline intact.

## Shared data contracts — do not rename without coordinating

`Learner`, `Attempt`, `Recommendation`, `SyncEvent`, `ContentPackage` field names in
`domain/model/` are frozen contracts across all four groups (PS section 53). If a field must
change, open a discussion before editing — a silent rename breaks whichever group's mock or
real implementation still expects the old shape.
