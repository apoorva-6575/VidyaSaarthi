package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.hackx.ruraledtech.data.local.database.Converters

@Entity(tableName = "attempts")
@TypeConverters(Converters::class)
data class AttemptEntity(
    @PrimaryKey val attemptId: String,
    val learnerId: String,
    val questionId: String,
    val conceptId: String,
    val selectedOptionIds: List<String>,
    val correct: Boolean,
    val responseTimeMs: Long,
    val timestamp: Long,
    val deviceId: String,
    val syncStatus: String,
)
