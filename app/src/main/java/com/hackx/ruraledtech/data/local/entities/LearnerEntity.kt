package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learners")
data class LearnerEntity(
    @PrimaryKey val learnerId: String,
    val name: String,
    val grade: Int,
    val preferredLanguage: String,
    val avatarKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastActiveAt: Long,
)
