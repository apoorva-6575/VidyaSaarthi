package com.hackx.ruraledtech.domain.model

data class Learner(
    val learnerId: String,
    val name: String,
    val grade: Int,
    val preferredLanguage: String,
    val avatarKey: String = "default",
    val createdAt: Long,
    val updatedAt: Long,
    val lastActiveAt: Long,
)
