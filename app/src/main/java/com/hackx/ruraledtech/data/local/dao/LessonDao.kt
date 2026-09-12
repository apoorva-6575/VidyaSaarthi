package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.LessonEntity

@Dao
interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Query("SELECT * FROM lessons WHERE lessonId = :id")
    suspend fun getById(id: String): LessonEntity?

    @Query("SELECT DISTINCT subject FROM lessons WHERE grade = :grade")
    suspend fun getSubjects(grade: Int): List<String>

    @Query(
        "SELECT * FROM lessons WHERE subject = :subject AND grade = :grade AND language = :language " +
            "ORDER BY orderIndex ASC",
    )
    suspend fun getForSubject(subject: String, grade: Int, language: String): List<LessonEntity>

    @Query(
        "SELECT * FROM lessons WHERE subject = :subject AND grade = :grade " +
            "ORDER BY orderIndex ASC",
    )
    suspend fun getForSubjectAnyLanguage(subject: String, grade: Int): List<LessonEntity>

    @Query("SELECT DISTINCT subject FROM lessons")
    suspend fun getAllSubjects(): List<String>

    @Query("SELECT * FROM lessons WHERE subject = :subject ORDER BY orderIndex ASC")
    suspend fun getForSubjectAllGrades(subject: String): List<LessonEntity>

    @Query("SELECT * FROM lessons ORDER BY orderIndex ASC")
    suspend fun getAllLessons(): List<LessonEntity>

    @Query("SELECT * FROM lessons ORDER BY orderIndex ASC")
    fun observeAllLessons(): kotlinx.coroutines.flow.Flow<List<LessonEntity>>

    /** Lessons belonging to a specific class — shown under that class on the student home screen. */
    @Query("SELECT * FROM lessons WHERE classId = :classId ORDER BY orderIndex ASC")
    fun observeByClassId(classId: String): kotlinx.coroutines.flow.Flow<List<LessonEntity>>

    /** General lessons not tied to any class (available to all learners). */
    @Query("SELECT * FROM lessons WHERE classId IS NULL ORDER BY orderIndex ASC")
    fun observeUnassigned(): kotlinx.coroutines.flow.Flow<List<LessonEntity>>

    @Query("DELETE FROM lessons WHERE packageId = :packageId")
    suspend fun deleteForPackage(packageId: String)

    @Query("SELECT packageId FROM lessons WHERE conceptId = :conceptId LIMIT 1")
    suspend fun getPackageIdForConcept(conceptId: String): String?
}
