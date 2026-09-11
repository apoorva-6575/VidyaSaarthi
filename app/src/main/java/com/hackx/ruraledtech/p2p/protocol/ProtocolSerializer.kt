package com.hackx.ruraledtech.p2p.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProtocolSerializer {
    // Configured to handle polymorphic sealed classes and ignore unknown fields for forward compatibility
    val json = Json { 
        ignoreUnknownKeys = true 
        classDiscriminator = "type" 
    }

    fun serialize(message: P2PMessage): ByteArray {
        return json.encodeToString(message).toByteArray(Charsets.UTF_8)
    }

    fun deserialize(bytes: ByteArray): P2PMessage? {
        return try {
            val stringData = String(bytes, Charsets.UTF_8)
            json.decodeFromString<P2PMessage>(stringData)
        } catch (e: Exception) {
            null
        }
    }
}
