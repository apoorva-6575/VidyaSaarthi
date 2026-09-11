package com.hackx.ruraledtech.domain.repository

import com.hackx.ruraledtech.domain.model.Attempt
import com.hackx.ruraledtech.domain.model.Question
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    suspend fun getQuestionsForLesson(lessonId: String): List<Question>
    suspend fun saveAttempt(attempt: Attempt)
    fun observeAttempts(learnerId: String, conceptId: String): Flow<List<Attempt>>
    suspend fun getAttemptCount(learnerId: String, conceptId: String): Int
}
