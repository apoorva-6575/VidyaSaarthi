package com.hackx.ruraledtech.domain

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.model.LessonProgress
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import com.hackx.ruraledtech.domain.usecase.progress.UpdateLessonProgressUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineProgressTest {

    @Test
    fun `lesson progress saves locally even if sync enqueue fails`() = runBlocking {
        var localSaved = false
        val progressRepo = object : ProgressRepository {
            override suspend fun upsertLessonProgress(progress: LessonProgress) {
                localSaved = true
            }
            override suspend fun upsertMastery(mastery: com.hackx.ruraledtech.domain.model.Mastery) {}
            override suspend fun getMastery(learnerId: String, conceptId: String) = null
            override fun observeLessonProgress(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<List<LessonProgress>>()
            override fun observeSubjectProgress(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.hackx.ruraledtech.domain.model.SubjectProgress>>()
            override fun observeMastery(learnerId: String) = kotlinx.coroutines.flow.emptyFlow<List<com.hackx.ruraledtech.domain.model.Mastery>>()
        }

        val syncRepo = object : SyncRepository {
            override suspend fun enqueueEvent(event: SyncEvent) {
                throw Exception("Network or Disk Error - Cannot Enqueue Sync Event")
            }
            override suspend fun getPendingEvents() = emptyList<SyncEvent>()
            override suspend fun markEventSynced(eventId: String) {}
            override suspend fun markEventFailed(eventId: String, error: String) {}
        }

        val useCase = UpdateLessonProgressUseCase(
            repository = progressRepo,
            syncRepository = syncRepo,
            idGenerator = object : IdGenerator {
                override fun attemptId() = "a1"
                override fun eventId() = "e1"
            },
            deviceIdProvider = object : DeviceIdProvider {
                override fun get() = "d1"
            },
            clock = object : AppClock {
                override fun nowMillis() = 1000L
            }
        )

        try {
            useCase("learner1", "lesson1", 0.5f, false, 0)
        } catch (e: Exception) {
            // Expected exception from syncRepo
        }

        // Even if enqueueing the sync event fails (or in general it's completely disconnected),
        // the local upsert MUST have completed successfully before it.
        assertTrue("Local progress must be saved regardless of sync failure", localSaved)
    }
}
