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
        val version: Int
    ) : P2PMessage

    @Serializable
    data class Accept(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val packageId: String
    ) : P2PMessage

    @Serializable
    data class PassportTransfer(
        override val messageId: String,
        override val senderDeviceId: String,
        override val timestamp: Long,
        val passport: com.hackx.ruraledtech.p2p.passport.model.LearningPassport
    ) : P2PMessage
}
