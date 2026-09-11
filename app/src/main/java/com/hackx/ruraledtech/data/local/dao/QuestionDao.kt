package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.QuestionEntity

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions WHERE lessonId = :lessonId")
    suspend fun getForLesson(lessonId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE questionId = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Query("DELETE FROM questions WHERE lessonId = :lessonId")
    suspend fun deleteForLesson(lessonId: String)
}
