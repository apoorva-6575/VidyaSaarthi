package com.hackx.ruraledtech.data.mapper

import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.domain.model.Learner

fun LearnerEntity.toDomain() = Learner(
    learnerId = learnerId,
    name = name,
    grade = grade,
    preferredLanguage = preferredLanguage,
    avatarKey = avatarKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastActiveAt = lastActiveAt,
)

fun Learner.toEntity() = LearnerEntity(
    learnerId = learnerId,
    name = name,
    grade = grade,
    preferredLanguage = preferredLanguage,
    avatarKey = avatarKey,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastActiveAt = lastActiveAt,
)
