package com.hackx.ruraledtech.p2p.integration

data class ContentRequirement(
    val packageId: String,
    val version: Int?,
    val conceptId: String?,
    val priority: Float,
    val reason: String?
)
