package com.hackx.ruraledtech.p2p.integration

interface LearnerDataImporter {
    suspend fun importLearnerData(passportJson: String): Boolean
}
