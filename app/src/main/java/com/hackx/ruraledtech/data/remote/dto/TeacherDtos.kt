package com.hackx.ruraledtech.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassGroupDto(
    val id: String,
    val name: String,
    val grade: String? = null,
    val subject: String? = null,
    val created_at: String? = null,
    val learners: List<LearnerDto> = emptyList()
)

@Serializable
data class LearnerDto(
    val id: String,
    val name: String,
    val grade: Int = 1,
    val preferred_language: String = "hi",
    val avatar_key: String = "default"
)

@Serializable
data class ClassAnalyticsDto(
    val class_id: String,
    val generated_at: String,
    val learner_metrics: Map<String, LearnerMetricsDto>,
    val class_averages: Map<String, Float>
)

@Serializable
data class LearnerMetricsDto(
    val mastery_levels: Map<String, Float>,
    val completed_lessons: Int,
    val average_score: Float,
    val needs_attention: Boolean
)
