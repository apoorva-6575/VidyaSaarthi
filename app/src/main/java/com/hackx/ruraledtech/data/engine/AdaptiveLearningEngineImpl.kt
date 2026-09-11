package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.domain.engine.MasteryCalculator
import com.hackx.ruraledtech.domain.engine.RecommendationEngine
import com.hackx.ruraledtech.domain.integration.ContentAvailabilityProvider
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.ContentRequirement
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.model.RecommendationType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [LearningEngine] owned by Group 2.
 * Calculates concept mastery via [MasteryCalculator], resolves concept graph, and generates
 * adaptive recommendations via [RecommendationEngine]. Emits [ContentRequirement] for Group 3 P2P mesh
 * when content is missing locally or needed for remediation.
 * Operates 100% offline without direct Room database writes (persistence is owned by the calling use case).
 */
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.RecommendationDao
import com.hackx.ruraledtech.domain.repository.RecommendationRepository

@Singleton
class AdaptiveLearningEngineImpl @Inject constructor(
    private val attemptDao: AttemptDao,
    private val questionDao: QuestionDao,
    private val conceptDao: ConceptDao,
    private val masteryCalculator: MasteryCalculator,
    private val recommendationEngine: RecommendationEngine,
    private val contentAvailabilityProvider: ContentAvailabilityProvider,
    private val clock: AppClock,
    private val lessonDao: LessonDao? = null,
    private val recommendationDao: RecommendationDao? = null,
    private val recommendationRepository: RecommendationRepository? = null,
) : LearningEngine {

    override suspend fun processAttempt(learnerId: String, attempt: Attempt): LearningResult {
        val dbAttemptEntities = attemptDao.getRecent(learnerId, attempt.conceptId, limit = 1000)
        var attempts = dbAttemptEntities.map { it.toDomain() }

        if (attempts.none { it.attemptId == attempt.attemptId }) {
            attempts = listOf(attempt) + attempts
        }

        val question = questionDao.getById(attempt.questionId)
        val difficulty = question?.difficulty ?: 0.5f
        val conceptEntity = conceptDao.getById(attempt.conceptId)
        val conceptName = conceptEntity?.name ?: attempt.conceptId

        val mastery = masteryCalculator.calculateMastery(
            learnerId = learnerId,
            conceptId = attempt.conceptId,
            conceptName = conceptName,
            attempts = attempts,
            questionDifficulty = difficulty,
            timestamp = clock.nowMillis(),
        )

        val rawRecommendation = recommendationEngine.generateRecommendation(mastery)
        val realPackageId = lessonDao?.getPackageIdForConcept(rawRecommendation.conceptId) ?: rawRecommendation.contentPackageId
        val recommendation = rawRecommendation.copy(contentPackageId = realPackageId)

        val isAvailable = contentAvailabilityProvider.isPackageAvailable(recommendation.contentPackageId)
        val contentRequirements = if (!isAvailable || recommendation.recommendationType == RecommendationType.REMEDIATION) {
            listOf(
                ContentRequirement(
                    packageId = recommendation.contentPackageId,
                    conceptId = recommendation.conceptId,
                    priority = if (!isAvailable) 1.0f else 0.90f,
                    reason = if (!isAvailable) {
                        "Package ${recommendation.contentPackageId} unavailable locally for ${recommendation.conceptName}"
                    } else {
                        "Remedial content required for ${recommendation.conceptName} (Mastery: ${mastery.score})"
                    },
                ),
            )
        } else {
            emptyList()
        }

        return LearningResult(
            updatedMastery = mastery,
            recommendation = recommendation,
            contentRequirements = contentRequirements,
        )
    }

    override suspend fun getRecommendation(learnerId: String): Recommendation? {
        return recommendationRepository?.getLatestRecommendation(learnerId)
            ?: recommendationDao?.getLatest(learnerId)?.toDomain()
    }
}
