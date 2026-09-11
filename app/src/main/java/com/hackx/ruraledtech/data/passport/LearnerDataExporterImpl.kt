package com.hackx.ruraledtech.data.passport

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao
import com.hackx.ruraledtech.data.passport.dto.PassportAttemptDto
import com.hackx.ruraledtech.data.passport.dto.PassportMasteryDto
import com.hackx.ruraledtech.data.passport.dto.PassportProgressDto
import com.hackx.ruraledtech.domain.integration.LearnerDataExporter
import com.hackx.ruraledtech.domain.model.LearnerExportData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val json = Json { ignoreUnknownKeys = true }

/**
 * Group 1's half of the Learning Passport contract (PS section 38/51). This only extracts
 * and serializes local Room state — Group 3 owns the encrypted P2P/QR transport of the blob.
 */
class LearnerDataExporterImpl @Inject constructor(
    private val learnerDao: LearnerDao,
    private val progressDao: ProgressDao,
    private val masteryDao: MasteryDao,
    private val attemptDao: AttemptDao,
    private val clock: AppClock,
) : LearnerDataExporter {

    override suspend fun exportLearnerData(learnerId: String): LearnerExportData {
        val learner = learnerDao.getById(learnerId) ?: error("Cannot export unknown learner $learnerId")
        val progress = progressDao.observeForLearner(learnerId).first().map {
            PassportProgressDto(it.lessonId, it.completionPercentage, it.completed, it.lastPosition, it.updatedAt)
        }
        val mastery = masteryDao.observeForLearner(learnerId).first().map {
            PassportMasteryDto(it.conceptId, it.conceptName, it.score, it.confidence, it.attemptCount, it.lastUpdated)
        }
        val conceptIds = mastery.map { it.conceptId }.distinct()
        val attempts = conceptIds.flatMap { conceptId ->
            attemptDao.getRecent(learnerId, conceptId, limit = 50)
        }.map { PassportAttemptDto(it.attemptId, it.questionId, it.conceptId, it.correct, it.timestamp) }

        return LearnerExportData(
            learnerId = learner.learnerId,
            name = learner.name,
            grade = learner.grade,
            preferredLanguage = learner.preferredLanguage,
            passportVersion = clock.nowMillis(),
            progressJson = json.encodeToString(ListSerializer(PassportProgressDto.serializer()), progress),
            masteryJson = json.encodeToString(ListSerializer(PassportMasteryDto.serializer()), mastery),
            attemptsJson = json.encodeToString(ListSerializer(PassportAttemptDto.serializer()), attempts),
            exportedAt = clock.nowMillis(),
        )
    }
}
