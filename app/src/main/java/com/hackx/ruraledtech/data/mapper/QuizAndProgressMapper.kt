package com.hackx.ruraledtech.data.mapper

import com.hackx.ruraledtech.data.local.entities.AttemptEntity
import com.hackx.ruraledtech.data.local.entities.LessonProgressEntity
import com.hackx.ruraledtech.data.local.entities.MasteryEntity
import com.hackx.ruraledtech.data.local.entities.RecommendationEntity
import com.hackx.ruraledtech.data.local.entities.SyncEventEntity
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.LessonProgress
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.model.RecommendationType
import com.hackx.ruraledtech.domain.model.SyncEvent
import com.hackx.ruraledtech.domain.model.SyncEventType
import com.hackx.ruraledtech.domain.model.SyncStatus

fun AttemptEntity.toDomain() = Attempt(
    attemptId = attemptId,
    learnerId = learnerId,
    questionId = questionId,
    conceptId = conceptId,
    selectedOptionIds = selectedOptionIds,
    correct = correct,
    responseTimeMs = responseTimeMs,
    timestamp = timestamp,
    deviceId = deviceId,
    syncStatus = SyncStatus.valueOf(syncStatus),
)

fun Attempt.toEntity() = AttemptEntity(
    attemptId = attemptId,
    learnerId = learnerId,
    questionId = questionId,
    conceptId = conceptId,
    selectedOptionIds = selectedOptionIds,
    correct = correct,
    responseTimeMs = responseTimeMs,
    timestamp = timestamp,
    deviceId = deviceId,
    syncStatus = syncStatus.name,
)

fun LessonProgressEntity.toDomain() = LessonProgress(
    learnerId = learnerId,
    lessonId = lessonId,
    completionPercentage = completionPercentage,
    completed = completed,
    lastPosition = lastPosition,
    updatedAt = updatedAt,
)

fun LessonProgress.toEntity() = LessonProgressEntity(
    learnerId = learnerId,
    lessonId = lessonId,
    completionPercentage = completionPercentage,
    completed = completed,
    lastPosition = lastPosition,
    updatedAt = updatedAt,
)

fun MasteryEntity.toDomain() = Mastery(
    learnerId = learnerId,
    conceptId = conceptId,
    conceptName = conceptName,
    score = score,
    confidence = confidence,
    attemptCount = attemptCount,
    lastUpdated = lastUpdated,
)

fun Mastery.toEntity() = MasteryEntity(
    learnerId = learnerId,
    conceptId = conceptId,
    conceptName = conceptName,
    score = score,
    confidence = confidence,
    attemptCount = attemptCount,
    lastUpdated = lastUpdated,
)

fun RecommendationEntity.toDomain() = Recommendation(
    learnerId = learnerId,
    conceptId = conceptId,
    conceptName = conceptName,
    mastery = mastery,
    recommendationType = RecommendationType.valueOf(recommendationType),
    contentPackageId = contentPackageId,
    targetLessonId = targetLessonId,
    difficulty = difficulty,
    generatedAt = generatedAt,
)

fun Recommendation.toEntity() = RecommendationEntity(
    learnerId = learnerId,
    conceptId = conceptId,
    conceptName = conceptName,
    mastery = mastery,
    recommendationType = recommendationType.name,
    contentPackageId = contentPackageId,
    targetLessonId = targetLessonId,
    difficulty = difficulty,
    generatedAt = generatedAt,
)

fun SyncEventEntity.toDomain() = SyncEvent(
    eventId = eventId,
    learnerId = learnerId,
    deviceId = deviceId,
    eventType = SyncEventType.valueOf(eventType),
    timestamp = timestamp,
    payloadJson = payloadJson,
    syncStatus = SyncStatus.valueOf(syncStatus),
)

fun SyncEvent.toEntity() = SyncEventEntity(
    eventId = eventId,
    learnerId = learnerId,
    deviceId = deviceId,
    eventType = eventType.name,
    timestamp = timestamp,
    payloadJson = payloadJson,
    syncStatus = syncStatus.name,
)
