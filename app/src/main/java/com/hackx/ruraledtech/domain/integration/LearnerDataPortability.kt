package com.hackx.ruraledtech.domain.integration

import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData

/**
 * Group 1 extracts/merges local Room state; Group 3 owns the actual encrypted P2P/QR
 * transport of the resulting [LearnerExportData] blob (the Learning Passport).
 */
interface LearnerDataExporter {
    suspend fun exportLearnerData(learnerId: String): LearnerExportData
}

interface LearnerDataImporter {
    suspend fun importLearnerData(data: LearnerExportData): ImportResult
}
