package com.hackx.ruraledtech.data.repository

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao
import com.hackx.ruraledtech.data.local.dao.SyncEventDao
import com.hackx.ruraledtech.data.local.entities.SyncEventEntity
import com.hackx.ruraledtech.data.local.prefs.SyncPreferences
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.SyncEventPayload
import com.hackx.ruraledtech.data.remote.dto.SyncRequest
import com.hackx.ruraledtech.data.remote.dto.SyncResponse
import com.hackx.ruraledtech.domain.repository.SyncOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Proves the offline teacher-intelligence pipeline end to end at the app boundary:
 * a MASTERY_UPDATED event queued while offline is neither dropped nor sent early, and once
 * sync runs it goes out with the exact envelope + payload fields a teacher-analytics backend
 * needs (learner_id, device_id, concept_id, mastery, confidence, attempt_count), then gets
 * marked SYNCED. Whether the backend's analytics endpoints actually consume it is Group 4's
 * contract and outside what an Android unit test can verify.
 */
class SyncRepositoryImplTest {

    private val syncEventDao: SyncEventDao = mockk(relaxed = true)
    private val connectivityObserver: ConnectivityObserver = mockk()
    private val api: RuralEdTechApi = mockk()
    private val syncPrefs: SyncPreferences = mockk(relaxed = true)
    private val masteryDao: MasteryDao = mockk(relaxed = true)
    private val progressDao: ProgressDao = mockk(relaxed = true)

    private lateinit var repository: SyncRepositoryImpl

    @Before
    fun setUp() {
        every { syncPrefs.deviceId } returns "D-1"
        every { syncPrefs.lastServerSequence } returns 0L
        repository = SyncRepositoryImpl(syncEventDao, connectivityObserver, api, syncPrefs, masteryDao, progressDao)
    }

    @Test
    fun `mastery update queued while offline is deferred, not dropped or sent`() = runTest {
        every { connectivityObserver.current() } returns ConnectivityState.OFFLINE

        val outcome = repository.syncNow()

        assertThat(outcome).isEqualTo(SyncOutcome.Deferred)
        coVerify(exactly = 0) { api.syncEvents(any()) }
    }

    @Test
    fun `queued MASTERY_UPDATED event is pushed with concept, mastery, confidence and attempt count, then marked synced`() = runTest {
        every { connectivityObserver.current() } returns ConnectivityState.ONLINE
        val pendingEntity = SyncEventEntity(
            eventId = "E-1",
            learnerId = "L-1",
            deviceId = "D-1",
            eventType = "MASTERY_UPDATED",
            timestamp = 1_000L,
            payloadJson = """{"concept_id":"fraction_basics","mastery":0.75,"confidence":0.6,"attempt_count":3}""",
            syncStatus = "PENDING",
        )
        coEvery { syncEventDao.getPending() } returns listOf(pendingEntity)

        val requestSlot = slot<SyncRequest>()
        coEvery { api.syncEvents(capture(requestSlot)) } returns Response.success(
            SyncResponse(accepted_events = 1, duplicate_events = 0, next_server_sequence = 5L, new_server_events = emptyList()),
        )

        val outcome = repository.syncNow()

        assertThat(outcome).isEqualTo(SyncOutcome.Success(1))
        val sentEvent = requestSlot.captured.events.single()
        assertThat(sentEvent.event_type).isEqualTo("MASTERY_UPDATED")
        assertThat(sentEvent.learner_id).isEqualTo("L-1")
        assertThat(sentEvent.device_id).isEqualTo("D-1")
        assertThat(sentEvent.payload["concept_id"]).isEqualTo(JsonPrimitive("fraction_basics"))
        assertThat(sentEvent.payload["mastery"]).isEqualTo(JsonPrimitive(0.75f))
        assertThat(sentEvent.payload["confidence"]).isEqualTo(JsonPrimitive(0.6f))
        assertThat(sentEvent.payload["attempt_count"]).isEqualTo(JsonPrimitive(3))

        coVerify { syncEventDao.markSynced(listOf("E-1")) }
    }

    @Test
    fun `pulling a MASTERY_UPDATED event from the server updates local mastery, preserving unset fields`() = runTest {
        every { connectivityObserver.current() } returns ConnectivityState.ONLINE
        coEvery { syncEventDao.getPending() } returns emptyList()
        coEvery { masteryDao.get("L-2", "fraction_basics") } returns null

        val serverPayload = SyncEventPayload(
            event_id = "E-server-1",
            learner_id = "L-2",
            device_id = "D-2",
            event_type = "MASTERY_UPDATED",
            timestamp = "2024-01-01T00:00:00.000Z",
            payload = JsonObject(
                mapOf(
                    "concept_id" to JsonPrimitive("fraction_basics"),
                    "mastery" to JsonPrimitive(0.9f),
                    "confidence" to JsonPrimitive(0.8f),
                    "attempt_count" to JsonPrimitive(5),
                ),
            ),
        )
        coEvery { api.syncEvents(any()) } returns Response.success(
            SyncResponse(accepted_events = 0, duplicate_events = 0, next_server_sequence = 9L, new_server_events = listOf(serverPayload)),
        )

        repository.syncNow()

        coVerify {
            masteryDao.upsert(
                match {
                    it.learnerId == "L-2" && it.conceptId == "fraction_basics" &&
                        it.score == 0.9f && it.confidence == 0.8f && it.attemptCount == 5
                },
            )
        }
    }
}
