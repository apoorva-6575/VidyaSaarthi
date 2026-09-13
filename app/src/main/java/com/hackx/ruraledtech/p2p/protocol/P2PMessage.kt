package com.hackx.ruraledtech.p2p.protocol

import kotlinx.serialization.Serializable
import com.hackx.ruraledtech.p2p.manifest.ContentManifest

@Serializable
sealed interface P2PMessage {
    val messageId: String
    val senderDeviceId: String
    val timestamp: Long

    @Serializable
    data class Hello(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val protocolVersion: Int
    ) : P2PMessage

    @Serializable
    data class Manifest(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val manifest: ContentManifest
    ) : P2PMessage

    @Serializable
    data class Request(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val packageId: String,
        val version: Int
    ) : P2PMessage
    
    @Serializable
    data class Offer(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val packageId: String,
        val version: Int,
        val expectedHash: String,
        val transferId: String,
        val sizeBytes: Long = 0L,
    ) : P2PMessage

    @Serializable
    data class Accept(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val transferId: String,
        val packageId: String,
        val version: Int = 1,
    ) : P2PMessage

    @Serializable
    data class PassportTransfer(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val passport: com.hackx.ruraledtech.p2p.passport.model.LearningPassport
    ) : P2PMessage

    @Serializable
    data class LearnerSync(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val learnerId: String,
        val name: String,
        val grade: Int,
        val preferredLanguage: String,
        val enrolledClassIds: List<String> = emptyList(),
    ) : P2PMessage

    @Serializable
    data class ClassMaterialOffer(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val packageId: String,
        val version: Int,
        val subject: String,
        val sizeBytes: Long,
        val expectedHash: String = "",
        val title: String = ""
    ) : P2PMessage

    @Serializable
    data class ClassMaterialRequest(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val requestId: String,
        val packageId: String,
        val requesterId: String
    ) : P2PMessage

    @Serializable
    data class TransferPayloadCorrelation(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val transferId: String,
        val payloadId: Long
    ) : P2PMessage

    @Serializable
    data class MaterialRequestDecision(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val requestId: String,
        val approved: Boolean
    ) : P2PMessage
}
