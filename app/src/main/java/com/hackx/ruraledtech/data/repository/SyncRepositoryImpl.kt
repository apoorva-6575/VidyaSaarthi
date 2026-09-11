package com.hackx.ruraledtech.data.repository

import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.data.local.dao.SyncEventDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.repository.SyncOutcome
import com.hackx.ruraledtech.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Group 4 owns the real backend upload. Until that lands, [syncNow] just reports how many
 * events are waiting so the UI's "Saved on this device / Waiting to sync" messaging (PS
 * section 49) is real even before a server exists — see MockRemoteSyncDataSource.
 */
class SyncRepositoryImpl @Inject constructor(
    private val syncEventDao: SyncEventDao,
    private val connectivityObserver: ConnectivityObserver,
    private val api: com.hackx.ruraledtech.data.remote.RuralEdTechApi,
    private val syncPrefs: com.hackx.ruraledtech.data.local.prefs.SyncPreferences,
    private val masteryDao: com.hackx.ruraledtech.data.local.dao.MasteryDao,
    private val progressDao: com.hackx.ruraledtech.data.local.dao.ProgressDao,
) : SyncRepository {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override suspend fun enqueueEvent(event: SyncEvent) = syncEventDao.insert(event.toEntity())

    override suspend fun getPendingEvents(): List<SyncEvent> = syncEventDao.getPending().map { it.toDomain() }

    override fun observePendingCount(): Flow<Int> = syncEventDao.observePendingCount()

    override suspend fun markSynced(eventIds: List<String>) = syncEventDao.markSynced(eventIds)

    override suspend fun syncNow(): SyncOutcome {
        if (connectivityObserver.current() == ConnectivityState.OFFLINE) {
            return SyncOutcome.Deferred
        }

        val pending = getPendingEvents()

        try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val payloads = pending.map { event ->
                com.hackx.ruraledtech.data.remote.dto.SyncEventPayload(
                    event_id = event.eventId,
                    learner_id = event.learnerId,
                    device_id = event.deviceId,
                    event_type = event.eventType.name,
                    timestamp = dateFormat.format(java.util.Date(event.timestamp)),
                    payload = json.parseToJsonElement(event.payloadJson) as kotlinx.serialization.json.JsonObject,
                    schema_version = 1
                )
            }

            val request = com.hackx.ruraledtech.data.remote.dto.SyncRequest(
                device_id = syncPrefs.deviceId,
                last_server_sequence = syncPrefs.lastServerSequence,
                events = payloads
            )

            val response = api.syncEvents(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Mark local events as synced
                if (pending.isNotEmpty()) {
                    markSynced(pending.map { it.eventId })
                }

                // Process new events from server (pull sync)
                body.new_server_events.forEach { payload ->
                    val timestampLong = try {
                        dateFormat.parse(payload.timestamp)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }

                    val eventEntity = com.hackx.ruraledtech.data.local.entities.SyncEventEntity(
                        eventId = payload.event_id,
                        learnerId = payload.learner_id,
                        deviceId = payload.device_id,
                        eventType = payload.event_type,
                        timestamp = timestampLong,
                        payloadJson = payload.payload.toString(),
                        syncStatus = "SYNCED"
                    )
                    syncEventDao.insert(eventEntity)

                    // Reconcile local state based on event
                    try {
                        when (payload.event_type) {
                            "MASTERY_UPDATED" -> {
                                val conceptId = payload.payload["concept_id"]?.let {
                                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                                } ?: ""
                                val score = payload.payload["mastery"]?.let {
                                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toFloatOrNull() else 0f
                                } ?: 0f

                                // Preserve conceptName/confidence/attemptCount from any existing
                                // local row (this event only carries a score) rather than
                                // clobbering them with placeholders.
                                val existing = masteryDao.get(payload.learner_id, conceptId)
                                val masteryEntity = com.hackx.ruraledtech.data.local.entities.MasteryEntity(
                                    learnerId = payload.learner_id,
                                    conceptId = conceptId,
                                    conceptName = existing?.conceptName ?: conceptId,
                                    score = score,
                                    confidence = existing?.confidence ?: 0.5f,
                                    attemptCount = existing?.attemptCount ?: 1,
                                    lastUpdated = timestampLong
                                )
                                masteryDao.upsert(masteryEntity)
                            }
                            "PROGRESS_UPDATED" -> {
                                val contentId = payload.payload["content_id"]?.let {
                                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                                } ?: ""
                                val progress = payload.payload["progress"]?.let {
                                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toFloatOrNull() else 0f
                                } ?: 0f

                                val existing = progressDao.get(payload.learner_id, contentId)
                                val progressEntity = com.hackx.ruraledtech.data.local.entities.LessonProgressEntity(
                                    learnerId = payload.learner_id,
                                    lessonId = contentId,
                                    completionPercentage = progress,
                                    completed = progress >= 1.0f,
                                    lastPosition = existing?.lastPosition ?: 0,
                                    updatedAt = timestampLong
                                )
                                progressDao.upsert(progressEntity)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                syncPrefs.lastServerSequence = body.next_server_sequence
                return SyncOutcome.Success(pending.size)
            } else {
                return SyncOutcome.PartialFailure(uploaded = 0, failed = pending.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return SyncOutcome.PartialFailure(uploaded = 0, failed = pending.size)
        }
    }
}
