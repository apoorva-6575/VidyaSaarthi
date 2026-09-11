package com.hackx.ruraledtech.domain.usecase.passport

import com.hackx.ruraledtech.domain.integration.LearnerDataExporter
import com.hackx.ruraledtech.domain.integration.LearnerDataImporter
import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData
import javax.inject.Inject

class ExportLearnerDataUseCase @Inject constructor(private val exporter: LearnerDataExporter) {
    suspend operator fun invoke(learnerId: String): LearnerExportData = exporter.exportLearnerData(learnerId)
}

class ImportLearnerDataUseCase @Inject constructor(private val importer: LearnerDataImporter) {
    suspend operator fun invoke(data: LearnerExportData): ImportResult = importer.importLearnerData(data)
}
