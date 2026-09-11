package com.hackx.ruraledtech.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LearnerExportData(
    val learnerId: String,
    val name: String,
    val grade: Int,
    val preferredLanguage: String,
    val passportVersion: Long,
    val progressJson: String,
    val masteryJson: String,
    val attemptsJson: String,
    val exportedAt: Long,
)

sealed class ImportResult {
    data class Imported(val learnerId: String) : ImportResult()
    data class Merged(val learnerId: String, val mergedEvents: Int) : ImportResult()
    data class Rejected(val reason: String) : ImportResult()
}
