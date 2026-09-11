package com.hackx.ruraledtech.domain.usecase.progress

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.model.LessonProgress
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.SubjectProgress
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.model.SyncEventType
import com.hackx.ruraledtech.domain.model.SyncStatus
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class ObserveLessonProgressUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke(learnerId: String): Flow<List<LessonProgress>> = repository.observeLessonProgress(learnerId)
}

class ObserveSubjectProgressUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke(learnerId: String): Flow<List<SubjectProgress>> = repository.observeSubjectProgress(learnerId)
}

class ObserveMasteryUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke(learnerId: String): Flow<List<Mastery>> = repository.observeMastery(learnerId)
}

/**
 * Every lesson-progress change is durably queued as a PROGRESS_UPDATED [SyncEvent] (same
 * pattern as SubmitQuizAttemptUseCase's MASTERY_UPDATED event) so a teacher's dashboard can
 * eventually reflect it, without a second sync mechanism — this reuses the existing
 * SyncRepository/SyncEventDao queue that WorkManager already drains.
 */
class UpdateLessonProgressUseCase @Inject constructor(
    private val repository: ProgressRepository,
    private val syncRepository: SyncRepository,
    private val idGenerator: IdGenerator,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: AppClock,
) {
    suspend operator fun invoke(learnerId: String, lessonId: String, completionPercentage: Float, completed: Boolean, lastPosition: Int) {
        val timestamp = clock.nowMillis()
        repository.upsertLessonProgress(
            LessonProgress(learnerId, lessonId, completionPercentage, completed, lastPosition, timestamp),
        )

        syncRepository.enqueueEvent(
            SyncEvent(
                eventId = idGenerator.eventId(),
                learnerId = learnerId,
                deviceId = deviceIdProvider.get(),
                eventType = SyncEventType.PROGRESS_UPDATED,
                timestamp = timestamp,
                payloadJson = Json.encodeToString(
                    JsonObject.serializer(),
                    JsonObject(
                        mapOf(
                            "content_id" to JsonPrimitive(lessonId),
                            "progress" to JsonPrimitive(completionPercentage),
                        ),
                    ),
                ),
                syncStatus = SyncStatus.PENDING,
            ),
        )
    }
}
