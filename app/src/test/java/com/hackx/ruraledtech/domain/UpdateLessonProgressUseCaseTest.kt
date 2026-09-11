package com.hackx.ruraledtech.domain

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.model.SyncEventType
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import com.hackx.ruraledtech.domain.repository.SyncRepository
import com.hackx.ruraledtech.domain.usecase.progress.UpdateLessonProgressUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Before
import org.junit.Test

/** Verifies lesson-progress changes are durably queued as PROGRESS_UPDATED sync events, same pattern as mastery. */
class UpdateLessonProgressUseCaseTest {

    private val repository: ProgressRepository = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val idGenerator: IdGenerator = mockk()
    private val deviceIdProvider: DeviceIdProvider = mockk()
    private val clock: AppClock = mockk()

    private lateinit var useCase: UpdateLessonProgressUseCase

    @Before
    fun setUp() {
        every { idGenerator.eventId() } returns "E-1"
        every { clock.nowMillis() } returns 2_000L
        coEvery { deviceIdProvider.get() } returns "D-1"
        useCase = UpdateLessonProgressUseCase(repository, syncRepository, idGenerator, deviceIdProvider, clock)
    }

    @Test
    fun `completing a lesson queues a PROGRESS_UPDATED event with content id and progress`() = runTest {
        val eventSlot = slot<SyncEvent>()
        coEvery { syncRepository.enqueueEvent(capture(eventSlot)) } returns Unit

        useCase(learnerId = "L-1", lessonId = "lesson1", completionPercentage = 1f, completed = true, lastPosition = 0)

        coVerify { repository.upsertLessonProgress(match { it.learnerId == "L-1" && it.lessonId == "lesson1" && it.completed }) }

        val event = eventSlot.captured
        assertThat(event.eventType).isEqualTo(SyncEventType.PROGRESS_UPDATED)
        assertThat(event.learnerId).isEqualTo("L-1")
        assertThat(event.deviceId).isEqualTo("D-1")
        val payload = Json.decodeFromString(JsonObject.serializer(), event.payloadJson)
        assertThat(payload["content_id"].toString()).contains("lesson1")
        assertThat(payload["progress"].toString()).contains("1.0")
    }
}
