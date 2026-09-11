package com.hackx.ruraledtech.domain.model

data class Question(
    val questionId: String,
    val lessonId: String,
    val conceptId: String,
    val questionType: QuestionType,
    val language: String,
    val prompt: String,
    val options: List<QuestionOption>,
    val correctOptionId: String,
    val difficulty: Float,
    val explanation: String?,
)

data class QuestionOption(
    val optionId: String,
    val text: String,
)

enum class QuestionType { SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE }
