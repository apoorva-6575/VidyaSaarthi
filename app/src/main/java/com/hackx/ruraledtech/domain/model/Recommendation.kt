package com.hackx.ruraledtech.domain.model

data class Recommendation(
    val learnerId: String,
    val conceptId: String,
    val conceptName: String,
    val mastery: Float,
    val recommendationType: RecommendationType,
    val contentPackageId: String,
    val targetLessonId: String?,
    val difficulty: Float,
    val generatedAt: Long,
)

enum class RecommendationType { REMEDIATION, PRACTICE, NEXT_CONCEPT, ADVANCED }

data class LearningResult(
    val updatedMastery: Mastery,
    val recommendation: Recommendation?,
)
