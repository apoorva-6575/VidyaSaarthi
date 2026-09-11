package com.hackx.ruraledtech.domain.repository

import com.hackx.ruraledtech.domain.model.Recommendation
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun observeLatestRecommendation(learnerId: String): Flow<Recommendation?>
    suspend fun getLatestRecommendation(learnerId: String): Recommendation?
    suspend fun saveRecommendation(recommendation: Recommendation)
}
