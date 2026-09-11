package com.hackx.ruraledtech.domain.model

data class SyncEvent(
    val eventId: String,
    val learnerId: String,
    val deviceId: String,
    val eventType: SyncEventType,
    val timestamp: Long,
    val payloadJson: String,
    val syncStatus: SyncStatus,
)

enum class SyncEventType {
    QUIZ_ATTEMPT,
    LESSON_STARTED,
    LESSON_COMPLETED,
    MASTERY_UPDATED,
    PROGRESS_UPDATED,
    PROFILE_UPDATED,
    CONTENT_ACQUIRED,
}
