package com.hackx.ruraledtech.domain.engine

import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.Recommendation

/**
 * Generates actionable learning recommendations based on mastery state and concept hierarchy.
 * Operates deterministically and offline.
 */
interface RecommendationEngine {
    suspend fun generateRecommendation(mastery: Mastery): Recommendation
}
