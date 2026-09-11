package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [optionsJson] is a serialized List<QuestionOptionDto>. */
@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val questionId: String,
    val lessonId: String,
    val conceptId: String,
    val questionType: String,
    val language: String,
    val prompt: String,
    val optionsJson: String,
    val correctOptionId: String,
    val difficulty: Float,
    val explanation: String?,
)
