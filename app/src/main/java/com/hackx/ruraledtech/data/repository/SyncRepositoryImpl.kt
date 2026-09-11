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
) : SyncRepository {

    override suspend fun enqueueEvent(event: SyncEvent) = syncEventDao.insert(event.toEntity())

    override suspend fun getPendingEvents(): List<SyncEvent> = syncEventDao.getPending().map { it.toDomain() }

    override fun observePendingCount(): Flow<Int> = syncEventDao.observePendingCount()

    override suspend fun markSynced(eventIds: List<String>) = syncEventDao.markSynced(eventIds)

    override suspend fun syncNow(): SyncOutcome {
        if (connectivityObserver.current() == ConnectivityState.OFFLINE) {
            return SyncOutcome.Deferred
        }
        val pending = getPendingEvents()
        if (pending.isEmpty()) return SyncOutcome.Success(0)
        markSynced(pending.map { it.eventId })
        return SyncOutcome.Success(pending.size)
    }
}
