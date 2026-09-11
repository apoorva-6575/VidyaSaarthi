package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.RecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recommendation: RecommendationEntity)

    @Query("SELECT * FROM recommendations WHERE learnerId = :learnerId")
    fun observeLatest(learnerId: String): Flow<RecommendationEntity?>
}
