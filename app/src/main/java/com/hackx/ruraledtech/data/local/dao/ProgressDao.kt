package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    @Query("SELECT * FROM lesson_progress WHERE learnerId = :learnerId")
    fun observeForLearner(learnerId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE learnerId = :learnerId AND lessonId = :lessonId")
    suspend fun get(learnerId: String, lessonId: String): LessonProgressEntity?
}
