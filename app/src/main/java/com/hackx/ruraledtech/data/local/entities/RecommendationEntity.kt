package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey val learnerId: String,
    val conceptId: String,
    val conceptName: String,
    val mastery: Float,
    val recommendationType: String,
    val contentPackageId: String,
    val targetLessonId: String?,
    val difficulty: Float,
    val generatedAt: Long,
)
