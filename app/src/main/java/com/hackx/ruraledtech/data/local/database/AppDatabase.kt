package com.hackx.ruraledtech.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.local.dao.RecommendationDao
import com.hackx.ruraledtech.data.local.dao.SyncEventDao
import com.hackx.ruraledtech.data.local.entities.AttemptEntity
import com.hackx.ruraledtech.data.local.entities.ConceptEntity
import com.hackx.ruraledtech.data.local.entities.ContentPackageEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.local.entities.LessonEntity
import com.hackx.ruraledtech.data.local.entities.LessonProgressEntity
import com.hackx.ruraledtech.data.local.entities.MasteryEntity
import com.hackx.ruraledtech.data.local.entities.QuestionEntity
import com.hackx.ruraledtech.data.local.entities.RecommendationEntity
import com.hackx.ruraledtech.data.local.entities.SyncEventEntity

/**
 * This database (Room over SQLite) is the local source of truth for every learner-facing
 * operation. Nothing about learning, quizzing, or progress may depend on the network — see
 * PS section 10/34. Backend sync (Group 4) only ever reads FROM here via SyncEventDao and
 * writes back acknowledgements; it never becomes the primary store.
 */
@Database(
    entities = [
        LearnerEntity::class,
        ConceptEntity::class,
        ContentPackageEntity::class,
        LessonEntity::class,
        QuestionEntity::class,
        AttemptEntity::class,
        LessonProgressEntity::class,
        MasteryEntity::class,
        RecommendationEntity::class,
        SyncEventEntity::class,
        com.hackx.ruraledtech.data.local.entities.ClassGroupEntity::class,
        com.hackx.ruraledtech.data.local.entities.ClassGroupLearnerEntity::class,
        com.hackx.ruraledtech.data.local.entities.TeacherDashboardCacheEntity::class,
        com.hackx.ruraledtech.data.local.entities.ClassAnalyticsCacheEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learnerDao(): LearnerDao
    abstract fun conceptDao(): ConceptDao
    abstract fun contentPackageDao(): ContentPackageDao
    abstract fun lessonDao(): LessonDao
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun progressDao(): ProgressDao
    abstract fun masteryDao(): MasteryDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun syncEventDao(): SyncEventDao
    abstract fun classGroupDao(): com.hackx.ruraledtech.data.local.dao.ClassGroupDao
    abstract fun teacherCacheDao(): com.hackx.ruraledtech.data.local.dao.TeacherCacheDao

    companion object {
        const val DATABASE_NAME = "rural_edtech.db"
    }
}
