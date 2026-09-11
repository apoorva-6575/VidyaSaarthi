package com.hackx.ruraledtech.data.passport

import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val json = Json { ignoreUnknownKeys = true }

/**
 * Bridges Group 3's simpler [com.hackx.ruraledtech.p2p.integration.LearnerDataExporter]
 * (used by [com.hackx.ruraledtech.p2p.passport.transport.PassportManager]) onto Group 1's
 * real [com.hackx.ruraledtech.domain.integration.LearnerDataExporter], which does the actual
 * Room extraction. Same duplicate-interface situation as ContentInstaller — see
 * [com.hackx.ruraledtech.data.contentpackage.P2PContentInstallerAdapter] and INTEGRATION.md.
 */
class P2PLearnerDataExporterAdapter @Inject constructor(
    private val delegate: com.hackx.ruraledtech.domain.integration.LearnerDataExporter,
) : com.hackx.ruraledtech.p2p.integration.LearnerDataExporter {

    override suspend fun exportLearnerData(learnerId: String): String =
        json.encodeToString(LearnerExportData.serializer(), delegate.exportLearnerData(learnerId))
}

class P2PLearnerDataImporterAdapter @Inject constructor(
    private val delegate: com.hackx.ruraledtech.domain.integration.LearnerDataImporter,
) : com.hackx.ruraledtech.p2p.integration.LearnerDataImporter {

    override suspend fun importLearnerData(passportJson: String): Boolean {
        val data = try {
            json.decodeFromString(LearnerExportData.serializer(), passportJson)
        } catch (e: Exception) {
            return false
        }
        return delegate.importLearnerData(data) !is ImportResult.Rejected
    }
}
