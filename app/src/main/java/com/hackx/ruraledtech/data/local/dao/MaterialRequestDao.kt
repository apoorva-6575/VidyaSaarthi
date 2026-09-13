package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.MaterialRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: MaterialRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<MaterialRequestEntity>)

    @Query("SELECT * FROM material_requests WHERE isIncoming = 1 AND status = 'PENDING'")
    fun observeIncomingRequests(): Flow<List<MaterialRequestEntity>>

    @Query("SELECT * FROM material_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getById(requestId: String): MaterialRequestEntity?
    
    @Query("UPDATE material_requests SET status = :status, lastUpdated = :lastUpdated WHERE requestId = :requestId")
    suspend fun updateStatus(requestId: String, status: String, lastUpdated: Long = System.currentTimeMillis())
}
