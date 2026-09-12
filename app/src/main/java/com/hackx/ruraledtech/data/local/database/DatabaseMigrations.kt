package com.hackx.ruraledtech.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `class_groups` (`classId` TEXT NOT NULL, `name` TEXT NOT NULL, `grade` TEXT, `subject` TEXT, `teacherId` TEXT NOT NULL, `lastSynced` INTEGER NOT NULL, PRIMARY KEY(`classId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `class_group_learners` (`classId` TEXT NOT NULL, `learnerId` TEXT NOT NULL, PRIMARY KEY(`classId`, `learnerId`))")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `teacher_dashboard_cache` (`teacherId` TEXT NOT NULL, `classesCount` INTEGER NOT NULL, `learnersCount` INTEGER NOT NULL, `conceptAveragesJson` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`teacherId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `class_analytics_cache` (`classId` TEXT NOT NULL, `generatedAt` TEXT NOT NULL, `classAveragesJson` TEXT NOT NULL, `learnerMetricsJson` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`classId`))")
        }
    }

    /** Adds classId (nullable) to lessons so materials can be tied to a specific class. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE lessons ADD COLUMN classId TEXT DEFAULT NULL")
        }
    }
}

