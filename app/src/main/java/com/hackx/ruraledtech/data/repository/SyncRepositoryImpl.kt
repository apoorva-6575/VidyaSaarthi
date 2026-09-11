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
                    // In a complete implementation we would map server events 
                    // and store them. For MVP Group 4, we store the raw event:
                    val timestampLong = try {
                        dateFormat.parse(payload.timestamp)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                    val eventEntity = com.hackx.ruraledtech.data.local.entity.SyncEventEntity(
                        eventId = payload.event_id,
                        learnerId = payload.learner_id,
                        deviceId = payload.device_id,
                        eventType = payload.event_type,
                        timestamp = timestampLong,
                        payloadJson = payload.payload.toString(),
                        syncStatus = "SYNCED"
                    )
                    syncEventDao.insert(eventEntity)
                }
                
                syncPrefs.lastServerSequence = body.next_server_sequence
                return SyncOutcome.Success(pending.size)
            } else {
                return SyncOutcome.PartialFailure(Exception("Server returned ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return SyncOutcome.PartialFailure(e)
        }
    }
}

