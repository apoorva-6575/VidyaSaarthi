package com.hackx.ruraledtech.domain.usecase.progress

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.domain.model.LessonProgress
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.SubjectProgress
import com.hackx.ruraledtech.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLessonProgressUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke(learnerId: String): Flow<List<LessonProgress>> = repository.observeLessonProgress(learnerId)
}

class ObserveSubjectProgressUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke(learnerId: String): Flow<List<SubjectProgress>> = repository.observeSubjectProgress(learnerId)
}

class ObserveMasteryUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke(learnerId: String): Flow<List<Mastery>> = repository.observeMastery(learnerId)
}

class UpdateLessonProgressUseCase @Inject constructor(
    private val repository: ProgressRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(learnerId: String, lessonId: String, completionPercentage: Float, completed: Boolean, lastPosition: Int) {
        repository.upsertLessonProgress(
            LessonProgress(learnerId, lessonId, completionPercentage, completed, lastPosition, clock.nowMillis()),
        )
    }
}
