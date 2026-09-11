package com.hackx.ruraledtech.data.passport

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao
import com.hackx.ruraledtech.data.local.entities.AttemptEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.local.entities.LessonProgressEntity
import com.hackx.ruraledtech.data.local.entities.MasteryEntity
import com.hackx.ruraledtech.data.passport.dto.PassportAttemptDto
import com.hackx.ruraledtech.data.passport.dto.PassportMasteryDto
import com.hackx.ruraledtech.data.passport.dto.PassportProgressDto
import com.hackx.ruraledtech.domain.integration.LearnerDataImporter
import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val json = Json { ignoreUnknownKeys = true }

/**
 * Merge, don't overwrite (PS section 39): an imported mastery/progress row only replaces
 * the local one if it's actually newer. This is what lets a learner bounce between a
 * mother's phone and a school tablet without silently losing whichever device wrote last.
 */
class LearnerDataImporterImpl @Inject constructor(
    private val learnerDao: LearnerDao,
    private val progressDao: ProgressDao,
    private val masteryDao: MasteryDao,
    private val attemptDao: AttemptDao,
    private val clock: AppClock,
) : LearnerDataImporter {

    override suspend fun importLearnerData(data: LearnerExportData): ImportResult {
        val existing = learnerDao.getById(data.learnerId)
        if (existing == null) {
            learnerDao.insert(
                LearnerEntity(
                    learnerId = data.learnerId,
                    name = data.name,
                    grade = data.grade,
                    preferredLanguage = data.preferredLanguage,
                    avatarKey = "default",
                    createdAt = data.exportedAt,
                    updatedAt = data.exportedAt,
                    lastActiveAt = clock.nowMillis(),
                ),
            )
        }

        var mergedCount = 0

        val incomingProgress = json.decodeFromString(ListSerializer(PassportProgressDto.serializer()), data.progressJson)
        incomingProgress.forEach { incoming ->
            val local = progressDao.get(data.learnerId, incoming.lessonId)
            if (local == null || incoming.updatedAt > local.updatedAt) {
                progressDao.upsert(
                    LessonProgressEntity(
                        learnerId = data.learnerId,
                        lessonId = incoming.lessonId,
                        completionPercentage = incoming.completionPercentage,
                        completed = incoming.completed,
                        lastPosition = incoming.lastPosition,
                        updatedAt = incoming.updatedAt,
                    ),
                )
                mergedCount++
            }
        }

        val incomingMastery = json.decodeFromString(ListSerializer(PassportMasteryDto.serializer()), data.masteryJson)
        incomingMastery.forEach { incoming ->
            val local = masteryDao.get(data.learnerId, incoming.conceptId)
            if (local == null || incoming.lastUpdated > local.lastUpdated) {
                masteryDao.upsert(
                    MasteryEntity(
                        learnerId = data.learnerId,
                        conceptId = incoming.conceptId,
                        conceptName = incoming.conceptName,
                        score = incoming.score,
                        confidence = incoming.confidence,
                        attemptCount = maxOf(incoming.attemptCount, local?.attemptCount ?: 0),
                        lastUpdated = incoming.lastUpdated,
                    ),
                )
                mergedCount++
            }
        }

        if (data.attemptsJson.isNotBlank()) {
            val incomingAttempts = json.decodeFromString(ListSerializer(PassportAttemptDto.serializer()), data.attemptsJson)
            incomingAttempts.forEach { incoming ->
                val localAttempt = attemptDao.getById(incoming.attemptId)
                if (localAttempt == null) {
                    attemptDao.insert(
                        AttemptEntity(
                            attemptId = incoming.attemptId,
                            learnerId = data.learnerId,
                            questionId = incoming.questionId,
                            conceptId = incoming.conceptId,
                            selectedOptionIds = emptyList(),
                            correct = incoming.correct,
                            responseTimeMs = 0L,
                            timestamp = incoming.timestamp,
                            deviceId = "passport_import",
                            syncStatus = "SYNCED",
                        ),
                    )
                    mergedCount++
                }
            }
        }

        return if (existing == null) ImportResult.Imported(data.learnerId) else ImportResult.Merged(data.learnerId, mergedCount)
    }
}
