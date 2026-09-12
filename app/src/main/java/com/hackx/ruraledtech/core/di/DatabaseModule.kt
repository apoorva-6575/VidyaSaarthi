package com.hackx.ruraledtech.core.di

import android.content.Context
import androidx.room.Room
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
import com.hackx.ruraledtech.data.local.database.AppDatabase
import com.hackx.ruraledtech.data.local.database.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
            )
            .fallbackToDestructiveMigration() // safety net only — real migrations above are tried first
            .build()

    @Provides
    fun provideLearnerDao(db: AppDatabase): LearnerDao = db.learnerDao()

    @Provides
    fun provideConceptDao(db: AppDatabase): ConceptDao = db.conceptDao()

    @Provides
    fun provideContentPackageDao(db: AppDatabase): ContentPackageDao = db.contentPackageDao()

    @Provides
    fun provideLessonDao(db: AppDatabase): LessonDao = db.lessonDao()

    @Provides
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideAttemptDao(db: AppDatabase): AttemptDao = db.attemptDao()

    @Provides
    fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideMasteryDao(db: AppDatabase): MasteryDao = db.masteryDao()

    @Provides
    fun provideRecommendationDao(db: AppDatabase): RecommendationDao = db.recommendationDao()

    @Provides
    fun provideSyncEventDao(db: AppDatabase): SyncEventDao = db.syncEventDao()
    
    @Provides
    fun provideClassGroupDao(db: AppDatabase): com.hackx.ruraledtech.data.local.dao.ClassGroupDao = db.classGroupDao()

    @Provides
    fun provideTeacherCacheDao(db: AppDatabase): com.hackx.ruraledtech.data.local.dao.TeacherCacheDao = db.teacherCacheDao()
}
