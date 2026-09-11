package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity

@Entity(tableName = "lesson_progress", primaryKeys = ["learnerId", "lessonId"])
data class LessonProgressEntity(
    val learnerId: String,
    val lessonId: String,
    val completionPercentage: Float,
    val completed: Boolean,
    val lastPosition: Int,
    val updatedAt: Long,
)

@Entity(tableName = "mastery_scores", primaryKeys = ["learnerId", "conceptId"])
data class MasteryEntity(
    val learnerId: String,
    val conceptId: String,
    val conceptName: String,
    val score: Float,
    val confidence: Float,
    val attemptCount: Int,
    val lastUpdated: Long,
)
