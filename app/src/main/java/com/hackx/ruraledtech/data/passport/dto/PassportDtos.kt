package com.hackx.ruraledtech.data.passport.dto

import kotlinx.serialization.Serializable

@Serializable
data class PassportProgressDto(
    val lessonId: String,
    val completionPercentage: Float,
    val completed: Boolean,
    val lastPosition: Int,
    val updatedAt: Long,
)

@Serializable
data class PassportMasteryDto(
    val conceptId: String,
    val conceptName: String,
    val score: Float,
    val confidence: Float,
    val attemptCount: Int,
    val lastUpdated: Long,
)

@Serializable
data class PassportAttemptDto(
    val attemptId: String,
    val questionId: String,
    val conceptId: String,
    val correct: Boolean,
    val timestamp: Long,
)
