package com.hackx.ruraledtech.p2p.passport.model

import kotlinx.serialization.Serializable

@Serializable
data class LearningPassport(
    val passportId: String,
    val learnerId: String,
    val version: Int,
    val timestamp: Long,
    val encryptedPayload: String, // Base64 encoded AES ciphertext
    val iv: String, // Base64 encoded Initialization Vector
    val salt: String // Base64 encoded Salt for key derivation
)
