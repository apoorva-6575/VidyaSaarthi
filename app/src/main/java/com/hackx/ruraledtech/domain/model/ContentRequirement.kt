package com.hackx.ruraledtech.domain.model

/**
 * Integration seam for Group 3 (P2P Mesh).
 * Expresses an offline content requirement identified by Group 2's adaptive learning engine
 * when specific content/remediation is needed for a learner.
 */
data class ContentRequirement(
    val packageId: String,
    val conceptId: String?,
    val priority: Float,
    val reason: String,
)
