package com.hackx.ruraledtech.domain

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import com.hackx.ruraledtech.domain.usecase.progress.UpdateLessonProgressUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lesson progress must persist locally even when the durable sync-event enqueue that follows
 * it fails (offline-first: the local write is the source of truth, never gated on sync).
 */
class OfflineProgressTest {

    @Test
    fun `lesson progress saves locally even if sync enqueue fails`() = runBlocking {
        var localSaved = false
        val progressRepo: ProgressRepository = mockk(relaxed = true)
        coEvery { progressRepo.upsertLessonProgress(any()) } answers { localSaved = true }

        val syncRepo: SyncRepository = mockk()
        coEvery { syncRepo.enqueueEvent(any()) } throws Exception("Network or Disk Error - Cannot Enqueue Sync Event")

        val idGenerator: IdGenerator = mockk()
        coEvery { idGenerator.attemptId() } returns "a1"
        coEvery { idGenerator.eventId() } returns "e1"

        val deviceIdProvider: DeviceIdProvider = mockk()
        coEvery { deviceIdProvider.get() } returns "d1"

        val clock: AppClock = mockk()
        coEvery { clock.nowMillis() } returns 1000L

        val useCase = UpdateLessonProgressUseCase(
            repository = progressRepo,
            syncRepository = syncRepo,
            idGenerator = idGenerator,
            deviceIdProvider = deviceIdProvider,
            clock = clock,
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
