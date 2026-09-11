package com.hackx.ruraledtech.domain.usecase.recommendation

import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecommendationUseCase @Inject constructor(private val repository: RecommendationRepository) {
    operator fun invoke(learnerId: String): Flow<Recommendation?> = repository.observeLatestRecommendation(learnerId)
}
