package com.hackx.ruraledtech.data.repository

import com.hackx.ruraledtech.data.local.dao.RecommendationDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecommendationRepositoryImpl @Inject constructor(
    private val recommendationDao: RecommendationDao,
) : RecommendationRepository {

    override fun observeLatestRecommendation(learnerId: String): Flow<Recommendation?> =
        recommendationDao.observeLatest(learnerId).map { it?.toDomain() }

    override suspend fun saveRecommendation(recommendation: Recommendation) =
        recommendationDao.upsert(recommendation.toEntity())
}
