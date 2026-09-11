package com.hackx.ruraledtech.data.contentpackage.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * On-disk JSON shapes for a content-package directory (PS section 11):
 *   manifest.json, lesson JSONs under lessons/, question JSONs under questions/, checksums.json
 * This schema is a shared contract with Group 2 (who authors the content) and Group 3
 * (who moves the raw bytes between devices) — see INTEGRATION.md before changing field names.
 */
@Serializable
data class ManifestDto(
    @SerialName("package_id") val packageId: String,
    val version: Int,
    val subject: String,
    val grade: Int,
    val languages: List<String>,
    val size: Long,
    val dependencies: List<String> = emptyList(),
    val checksum: String,
    @SerialName("created_at") val createdAt: String = "",
    val priority: String = "normal",
)

@Serializable
data class LessonFileDto(
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("concept_id") val conceptId: String,
    val language: String,
    val title: String,
    @SerialName("order_index") val orderIndex: Int,
    val blocks: List<ContentBlockFileDto>,
)

@Serializable
data class ContentBlockFileDto(
    @SerialName("block_id") val blockId: String,
    val type: String,
    val body: String? = null,
    @SerialName("asset_path") val assetPath: String? = null,
    @SerialName("alt_text") val altText: String? = null,
    val transcript: String? = null,
    val prompt: String? = null,
    val explanation: String? = null,
    val message: String? = null,
    val tone: String? = null,
)

@Serializable
data class QuestionFileDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("concept_id") val conceptId: String,
    @SerialName("question_type") val questionType: String,
    val language: String,
    val prompt: String,
    val options: List<QuestionOptionFileDto>,
    @SerialName("correct_option_id") val correctOptionId: String,
    val difficulty: Float,
    val explanation: String? = null,
)

@Serializable
data class QuestionOptionFileDto(
    @SerialName("option_id") val optionId: String,
    val text: String,
)

@Serializable
data class ChecksumsFileDto(
    val files: Map<String, String>,
)
