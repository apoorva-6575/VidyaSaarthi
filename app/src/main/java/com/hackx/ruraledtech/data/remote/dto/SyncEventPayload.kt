package com.hackx.ruraledtech.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SyncEventPayload(
    val event_id: String,
    val learner_id: String,
    val device_id: String,
    val event_type: String,
    val timestamp: String,
    val payload: JsonObject,
    val schema_version: Int = 1
)
