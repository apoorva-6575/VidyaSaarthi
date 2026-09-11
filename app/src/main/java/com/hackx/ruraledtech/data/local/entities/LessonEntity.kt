package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [blocksJson] is a serialized List<ContentBlockDto> — see LessonMapper. */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val lessonId: String,
    val packageId: String,
    val subject: String,
    val grade: Int,
    val conceptId: String,
    val language: String,
    val title: String,
    val orderIndex: Int,
    val blocksJson: String,
)
