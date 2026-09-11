package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.MasteryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mastery: MasteryEntity)

    @Query("SELECT * FROM mastery_scores WHERE learnerId = :learnerId")
    fun observeForLearner(learnerId: String): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM mastery_scores WHERE learnerId = :learnerId AND conceptId = :conceptId")
    suspend fun get(learnerId: String, conceptId: String): MasteryEntity?
}
