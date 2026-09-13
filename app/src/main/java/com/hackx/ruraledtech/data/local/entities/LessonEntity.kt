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
    /** The class this lesson belongs to. Null = available to all learners (general content). */
    val classId: String? = null,
    /**
     * Which learner profile's device session received/created this lesson. Null = shared
     * curriculum content (e.g. the factory-preloaded demo package), visible to every profile
     * on this device. Non-null = personal content (received via P2P mesh or authored locally)
     * that must stay private to that one profile — previously every lesson was visible to
     * every learner sharing the device regardless of who actually received it.
     */
    val receivedByLearnerId: String? = null,
)

