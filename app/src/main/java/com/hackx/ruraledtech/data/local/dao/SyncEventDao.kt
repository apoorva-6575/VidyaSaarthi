package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.SyncEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: SyncEventEntity)

    @Query("SELECT * FROM sync_events WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPending(): List<SyncEventEntity>

    @Query("SELECT COUNT(*) FROM sync_events WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE sync_events SET syncStatus = 'SYNCED' WHERE eventId IN (:eventIds)")
    suspend fun markSynced(eventIds: List<String>)
}
