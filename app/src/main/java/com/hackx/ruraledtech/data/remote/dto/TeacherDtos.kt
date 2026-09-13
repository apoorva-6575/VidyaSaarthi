package com.hackx.ruraledtech.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeacherCreateDto(
    val name: String,
    val email: String,
    val password: String,
    val organization_id: String? = null,
)

@Serializable
data class TeacherResponseDto(
    val id: String,
    val name: String,
    val email: String,
    val organization_id: String? = null,
)

@Serializable
data class TokenDto(
    val access_token: String,
    val token_type: String,
)

@Serializable
data class ClassGroupCreateDto(
    val name: String,
    val grade: String? = null,
    val subject: String? = null,
)

@Serializable
data class LearnerDto(
    val id: String,
    val name: String,
    val grade: String? = null,
    val preferred_language: String = "hi",
    val avatar_key: String = "default",
)

@Serializable
data class ClassGroupDto(
    val id: String,
    val join_code: String = "",
    val name: String,
    val grade: String? = null,
    val subject: String? = null,
    val teacher_id: String? = null,
    val created_at: String? = null,
    val learners: List<LearnerDto> = emptyList(),
)

@Serializable
data class ClassMaterialAssignmentDto(
    val package_id: String,
    val version: Int,
    val shared_at: String? = null
)

@Serializable
data class ClassMaterialRequestDto(
    val package_id: String,
    val version: Int
)

@Serializable
data class MaterialRequestCreateDto(
    val class_id: String,
    val package_id: String,
    val requester_id: String
)

@Serializable
data class MaterialRequestUpdateDto(
    val status: String,
    val provider_id: String? = null
)

@Serializable
data class MaterialRequestResponseDto(
    val id: String,
    val class_id: String,
    val package_id: String,
    val requester_id: String,
    val provider_id: String?,
    val status: String,
    val created_at: String,
    val updated_at: String?
)

@Serializable
data class ClassAnalyticsDto(
    val class_id: String,
    val generated_at: String,
    val learner_metrics: Map<String, LearnerMetricsDto>,
    val class_averages: Map<String, Float>,
)

@Serializable
data class LearnerMetricsDto(
    val name: String = "",
    val grade: String? = null,
    val mastery_levels: Map<String, Float> = emptyMap(),
    val completed_lessons: Int = 0,
    val average_score: Float = 0f,
    val needs_attention: Boolean = false,
)

@Serializable
data class TeacherDashboardDto(
    val classes_count: Int,
    val learners_count: Int,
    val concept_averages: Map<String, Float> = emptyMap(),
)

@Serializable
data class LearnerCreateDto(
    val id: String,
    val name: String,
    val grade: String? = null,
    val preferred_language: String = "en",
)

@Serializable
data class JoinClassRequestDto(
    val code: String,
    val learner_id: String,
    val name: String? = null,
    val grade: String? = null,
    val preferred_language: String? = "en",
)
