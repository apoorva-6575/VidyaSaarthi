package com.hackx.ruraledtech.data.repository

import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao
import com.hackx.ruraledtech.data.mapper.toDomain
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.model.LessonProgress
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.SubjectProgress
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
    private val masteryDao: MasteryDao,
    private val lessonDao: LessonDao,
) : ProgressRepository {

    override fun observeLessonProgress(learnerId: String): Flow<List<LessonProgress>> =
        progressDao.observeForLearner(learnerId).map { list -> list.map { it.toDomain() } }

    override fun observeSubjectProgress(learnerId: String): Flow<List<SubjectProgress>> =
        progressDao.observeForLearner(learnerId).map { progressList ->
            val bySubjectLesson = progressList.associateBy { it.lessonId }
            bySubjectLesson.values
                .mapNotNull { progress -> lessonDao.getById(progress.lessonId)?.subject?.let { it to progress } }
                .groupBy({ it.first }, { it.second })
                .map { (subject, entries) ->
                    SubjectProgress(
                        subject = subject,
                        completionPercentage = entries.map { it.completionPercentage }.average().toFloat(),
                        lessonsCompleted = entries.count { it.completed },
                        lessonsTotal = entries.size,
                    )
                }
        }

    override fun observeMastery(learnerId: String): Flow<List<Mastery>> =
        masteryDao.observeForLearner(learnerId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertLessonProgress(progress: LessonProgress) = progressDao.upsert(progress.toEntity())

    override suspend fun upsertMastery(mastery: Mastery) = masteryDao.upsert(mastery.toEntity())

    override suspend fun getMastery(learnerId: String, conceptId: String): Mastery? =
        masteryDao.get(learnerId, conceptId)?.toDomain()
}
