package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.AttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE learnerId = :learnerId AND conceptId = :conceptId ORDER BY timestamp DESC")
    fun observeForConcept(learnerId: String, conceptId: String): Flow<List<AttemptEntity>>

    @Query("SELECT COUNT(*) FROM attempts WHERE learnerId = :learnerId AND conceptId = :conceptId")
    suspend fun countForConcept(learnerId: String, conceptId: String): Int

    @Query("SELECT * FROM attempts WHERE learnerId = :learnerId AND conceptId = :conceptId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(learnerId: String, conceptId: String, limit: Int): List<AttemptEntity>
}
