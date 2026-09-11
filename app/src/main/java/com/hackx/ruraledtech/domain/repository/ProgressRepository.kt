package com.hackx.ruraledtech.domain.repository

import com.hackx.ruraledtech.domain.model.LessonProgress
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.SubjectProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeLessonProgress(learnerId: String): Flow<List<LessonProgress>>
    fun observeSubjectProgress(learnerId: String): Flow<List<SubjectProgress>>
    fun observeMastery(learnerId: String): Flow<List<Mastery>>
    suspend fun upsertLessonProgress(progress: LessonProgress)
    suspend fun upsertMastery(mastery: Mastery)
    suspend fun getMastery(learnerId: String, conceptId: String): Mastery?
}
