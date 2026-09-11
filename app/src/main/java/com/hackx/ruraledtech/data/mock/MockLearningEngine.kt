package com.hackx.ruraledtech.data.mock

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.ConceptDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.domain.integration.LearningEngine
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.model.RecommendationType
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Placeholder for Group 2's real adaptive engine (PS section 6/19/50). It implements the
 * exact reference formula from the PS so the offline learning loop is genuinely functional
 * end-to-end during Group 1 development, and is swapped out purely via DI (see AppModule) —
 * no call site elsewhere in the app changes when the real engine lands.
 *
 * This class only *computes*; it never writes to Room. Whoever calls it
 * (SubmitQuizAttemptUseCase) owns persisting the returned mastery/recommendation, so there
 * is exactly one writer for the mastery_scores and recommendations tables regardless of
 * which LearningEngine implementation is wired in.
 *
 *   mastery = 0.50*accuracy + 0.25*recent_accuracy + 0.15*difficulty_factor + 0.10*consistency
 */
class MockLearningEngine @Inject constructor(
    private val attemptDao: AttemptDao,
    private val questionDao: QuestionDao,
    private val conceptDao: ConceptDao,
    private val clock: AppClock,
) : LearningEngine {

    private companion object {
        const val RECENT_WINDOW = 5
    }

    override suspend fun processAttempt(learnerId: String, attempt: Attempt): LearningResult {
        val allAttempts = attemptDao.getRecent(learnerId, attempt.conceptId, limit = 1000)
        val accuracy = allAttempts.count { it.correct }.toFloat() / allAttempts.size.coerceAtLeast(1)

        val recent = allAttempts.take(RECENT_WINDOW)
        val recentAccuracy = recent.count { it.correct }.toFloat() / recent.size.coerceAtLeast(1)

        val question = questionDao.getById(attempt.questionId)
        val difficultyFactor = if (attempt.correct) (question?.difficulty ?: 0.5f) else 0f

        val consistency = if (recent.size < 2) {
            1f
        } else {
            1f - (recent.zipWithNext().count { (a, b) -> a.correct != b.correct }.toFloat() / (recent.size - 1))
        }

        val score = (0.50f * accuracy) + (0.25f * recentAccuracy) + (0.15f * difficultyFactor) + (0.10f * consistency)
        val clampedScore = max(0f, min(1f, score))
        val conceptName = conceptDao.getById(attempt.conceptId)?.name ?: attempt.conceptId

        val mastery = Mastery(
            learnerId = learnerId,
            conceptId = attempt.conceptId,
            conceptName = conceptName,
            score = clampedScore,
            confidence = min(1f, allAttempts.size / 10f),
            attemptCount = allAttempts.size,
            lastUpdated = clock.nowMillis(),
        )

        return LearningResult(updatedMastery = mastery, recommendation = buildRecommendation(learnerId, mastery))
    }

    /** ViewModels observe [com.hackx.ruraledtech.domain.repository.RecommendationRepository] directly instead. */
    override suspend fun getRecommendation(learnerId: String): Recommendation? = null

    private fun buildRecommendation(learnerId: String, mastery: Mastery): Recommendation {
        val type = when (mastery.level) {
            MasteryLevel.BEGINNER -> RecommendationType.REMEDIATION
            MasteryLevel.DEVELOPING -> RecommendationType.PRACTICE
            MasteryLevel.PROFICIENT -> RecommendationType.NEXT_CONCEPT
            MasteryLevel.MASTERED -> RecommendationType.ADVANCED
        }
        val targetDifficulty = when (mastery.level) {
            MasteryLevel.BEGINNER -> 0.25f
            MasteryLevel.DEVELOPING -> 0.45f
            MasteryLevel.PROFICIENT -> 0.65f
            MasteryLevel.MASTERED -> 0.85f
        }
        return Recommendation(
            learnerId = learnerId,
            conceptId = mastery.conceptId,
            conceptName = mastery.conceptName,
            mastery = mastery.score,
            recommendationType = type,
            contentPackageId = mastery.conceptId,
            targetLessonId = null,
            difficulty = targetDifficulty,
            generatedAt = clock.nowMillis(),
        )
    }
}
