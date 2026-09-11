package com.hackx.ruraledtech.domain.model

data class Attempt(
    val attemptId: String,
    val learnerId: String,
    val questionId: String,
    val conceptId: String,
    val selectedOptionIds: List<String>,
    val correct: Boolean,
    val responseTimeMs: Long,
    val timestamp: Long,
    val deviceId: String,
    val syncStatus: SyncStatus,
)

enum class SyncStatus { PENDING, SYNCING, SYNCED, FAILED }
