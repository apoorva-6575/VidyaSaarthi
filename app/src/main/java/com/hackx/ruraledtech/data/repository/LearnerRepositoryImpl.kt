package com.hackx.ruraledtech.data.repository

import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LearnerRepositoryImpl @Inject constructor(
    private val learnerDao: LearnerDao,
) : LearnerRepository {

    override suspend fun createLearner(learner: Learner) = learnerDao.insert(learner.toEntity())

    override suspend fun getLearner(id: String): Learner? = learnerDao.getById(id)?.toDomain()

    override fun observeLearners(): Flow<List<Learner>> =
        learnerDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun updateLearner(learner: Learner) = learnerDao.update(learner.toEntity())

    override suspend fun deleteLearner(id: String) = learnerDao.deleteById(id)

    override suspend fun touchLastActive(id: String, timestamp: Long) = learnerDao.touchLastActive(id, timestamp)
}
