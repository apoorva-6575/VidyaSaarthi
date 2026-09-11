package com.hackx.ruraledtech.data.engine

import com.hackx.ruraledtech.domain.engine.MasteryCalculator
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.Mastery
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Production implementation of [MasteryCalculator].
 * Implements the required 4-component mastery formula:
 * mastery = 0.50 * accuracy + 0.25 * recent_accuracy + 0.15 * difficulty_factor + 0.10 * consistency
 */
class DefaultMasteryCalculator @Inject constructor() : MasteryCalculator {

    companion object {
        const val RECENT_WINDOW = 5
        const val WEIGHT_ACCURACY = 0.50f
        const val WEIGHT_RECENT_ACCURACY = 0.25f
        const val WEIGHT_DIFFICULTY = 0.15f
        const val WEIGHT_CONSISTENCY = 0.10f
    }

    override fun calculateMastery(
        learnerId: String,
        conceptId: String,
        conceptName: String,
        attempts: List<Attempt>,
        questionDifficulty: Float,
        timestamp: Long,
    ): Mastery {
        if (attempts.isEmpty()) {
            return Mastery(
                learnerId = learnerId,
                conceptId = conceptId,
                conceptName = conceptName,
                score = 0.0f,
                confidence = 0.0f,
                attemptCount = 0,
                lastUpdated = timestamp,
            )
        }

        val accuracy = attempts.count { it.correct }.toFloat() / attempts.size

        val recent = attempts.take(RECENT_WINDOW)
        val recentAccuracy = recent.count { it.correct }.toFloat() / recent.size

        val latestAttempt = attempts.firstOrNull()
        val difficultyFactor = if (latestAttempt?.correct == true) {
            questionDifficulty.coerceIn(0f, 1f)
        } else {
            0.0f
        }

        val consistency = if (recent.size < 2) {
            1.0f
        } else {
            val fluctuations = recent.zipWithNext().count { (a, b) -> a.correct != b.correct }
            1.0f - (fluctuations.toFloat() / (recent.size - 1))
        }

        val rawScore = (WEIGHT_ACCURACY * accuracy) +
            (WEIGHT_RECENT_ACCURACY * recentAccuracy) +
            (WEIGHT_DIFFICULTY * difficultyFactor) +
            (WEIGHT_CONSISTENCY * consistency)

        val clampedScore = rawScore.coerceIn(0f, 1f)
        val confidence = min(1.0f, attempts.size / 10.0f)

        return Mastery(
            learnerId = learnerId,
            conceptId = conceptId,
            conceptName = conceptName,
            score = clampedScore,
            confidence = confidence,
            attemptCount = attempts.size,
            lastUpdated = timestamp,
        )
    }
}
