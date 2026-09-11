package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(learner: LearnerEntity)

    /** Alias for [insert] (REPLACE already gives upsert semantics) — matches the naming
     * every other DAO in this codebase uses (ContentPackageDao, MasteryDao, ProgressDao). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(learner: LearnerEntity)

    @Update
    suspend fun update(learner: LearnerEntity)

    @Query("SELECT * FROM learners WHERE learnerId = :id")
    suspend fun getById(id: String): LearnerEntity?

    @Query("SELECT * FROM learners ORDER BY lastActiveAt DESC")
    fun observeAll(): Flow<List<LearnerEntity>>

    @Query("UPDATE learners SET lastActiveAt = :timestamp WHERE learnerId = :id")
    suspend fun touchLastActive(id: String, timestamp: Long)

    @Delete
    suspend fun delete(learner: LearnerEntity)

    @Query("DELETE FROM learners WHERE learnerId = :id")
    suspend fun deleteById(id: String)
}
