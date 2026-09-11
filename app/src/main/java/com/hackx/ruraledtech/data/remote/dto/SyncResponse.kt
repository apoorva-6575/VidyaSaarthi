package com.hackx.ruraledtech.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(
    val accepted_events: Int,
    val duplicate_events: Int,
    val next_server_sequence: Long,
    val new_server_events: List<SyncEventPayload>
)
