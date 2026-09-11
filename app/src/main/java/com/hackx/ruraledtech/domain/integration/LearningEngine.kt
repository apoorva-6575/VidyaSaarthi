package com.hackx.ruraledtech.domain.integration

import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.LearningResult
import com.hackx.ruraledtech.domain.model.Recommendation

/**
 * Owned by Group 2 (Adaptive Learning). Group 1 depends only on this interface and
 * ships [com.hackx.ruraledtech.data.mock.MockLearningEngine] until the real engine lands.
 */
interface LearningEngine {
    suspend fun processAttempt(learnerId: String, attempt: Attempt): LearningResult
    suspend fun getRecommendation(learnerId: String): Recommendation?
}
