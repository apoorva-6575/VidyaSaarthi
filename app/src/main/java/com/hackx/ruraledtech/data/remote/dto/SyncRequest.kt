package com.hackx.ruraledtech.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val device_id: String,
    val last_server_sequence: Long,
    val events: List<SyncEventPayload>
)
