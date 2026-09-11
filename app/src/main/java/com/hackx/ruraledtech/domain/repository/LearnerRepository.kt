package com.hackx.ruraledtech.domain.repository

import com.hackx.ruraledtech.domain.model.Learner
import kotlinx.coroutines.flow.Flow

interface LearnerRepository {
    suspend fun createLearner(learner: Learner)
    suspend fun getLearner(id: String): Learner?
    fun observeLearners(): Flow<List<Learner>>
    suspend fun updateLearner(learner: Learner)
    suspend fun deleteLearner(id: String)
    suspend fun touchLastActive(id: String, timestamp: Long)
}
