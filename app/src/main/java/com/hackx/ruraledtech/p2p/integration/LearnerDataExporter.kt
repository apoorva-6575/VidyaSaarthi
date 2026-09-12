package com.hackx.ruraledtech.p2p.integration

interface LearnerDataExporter {
    suspend fun exportLearnerData(learnerId: String): String
}
