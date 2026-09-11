package com.hackx.ruraledtech.domain.engine

import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.Mastery

/**
 * Calculates concept mastery from attempt history and question difficulty.
 * Formula: mastery = 0.50 * accuracy + 0.25 * recent_accuracy + 0.15 * difficulty_factor + 0.10 * consistency
 * Bounded cleanly to [0.0, 1.0].
 */
interface MasteryCalculator {
    fun calculateMastery(
        learnerId: String,
        conceptId: String,
        conceptName: String,
        attempts: List<Attempt>,
        questionDifficulty: Float,
        timestamp: Long,
    ): Mastery
}
