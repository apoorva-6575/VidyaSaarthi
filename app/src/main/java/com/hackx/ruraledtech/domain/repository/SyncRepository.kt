package com.hackx.ruraledtech.domain.repository

import com.hackx.ruraledtech.domain.model.SyncEvent
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun enqueueEvent(event: SyncEvent)
    suspend fun getPendingEvents(): List<SyncEvent>
    fun observePendingCount(): Flow<Int>
    suspend fun markSynced(eventIds: List<String>)
    suspend fun syncNow(): SyncOutcome
}

sealed class SyncOutcome {
    data object Deferred : SyncOutcome()
    data class Success(val uploaded: Int) : SyncOutcome()
    data class PartialFailure(val uploaded: Int, val failed: Int) : SyncOutcome()
}
