package com.hackx.ruraledtech.data.repository

import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao,
) : QuizRepository {

    override suspend fun getQuestionsForLesson(lessonId: String): List<Question> =
        questionDao.getForLesson(lessonId).map { it.toDomain() }

    override suspend fun saveAttempt(attempt: Attempt) = attemptDao.insert(attempt.toEntity())

    override fun observeAttempts(learnerId: String, conceptId: String): Flow<List<Attempt>> =
        attemptDao.observeForConcept(learnerId, conceptId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAttemptCount(learnerId: String, conceptId: String): Int =
        attemptDao.countForConcept(learnerId, conceptId)
}
