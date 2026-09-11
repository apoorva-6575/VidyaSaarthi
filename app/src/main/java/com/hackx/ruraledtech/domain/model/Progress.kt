package com.hackx.ruraledtech.domain.model

data class LessonProgress(
    val learnerId: String,
    val lessonId: String,
    val completionPercentage: Float,
    val completed: Boolean,
    val lastPosition: Int,
    val updatedAt: Long,
)

data class SubjectProgress(
    val subject: String,
    val completionPercentage: Float,
    val lessonsCompleted: Int,
    val lessonsTotal: Int,
)
