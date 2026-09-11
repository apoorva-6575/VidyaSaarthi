package com.hackx.ruraledtech.domain.usecase.learner

import com.hackx.ruraledtech.core.common.AppClock
import com.hackx.ruraledtech.core.common.IdGenerator
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLearnersUseCase @Inject constructor(private val repository: LearnerRepository) {
    operator fun invoke(): Flow<List<Learner>> = repository.observeLearners()
}

class CreateLearnerUseCase @Inject constructor(
    private val repository: LearnerRepository,
    private val idGenerator: IdGenerator,
    private val clock: AppClock,
) {
    suspend operator fun invoke(name: String, grade: Int, preferredLanguage: String): Learner {
        val now = clock.nowMillis()
        val learner = Learner(
            learnerId = idGenerator.learnerId(),
            name = name,
            grade = grade,
            preferredLanguage = preferredLanguage,
            createdAt = now,
            updatedAt = now,
            lastActiveAt = now,
        )
        repository.createLearner(learner)
        return learner
    }
}

/**
 * Also syncs the device-wide "last used" language to the selected learner's own preference
 * (PS section 4.3) — this is what lets a second learner's "+ Add learner" flow default
 * sensibly, and is the hook full UI localization (not just content) would build on later.
 */
class SelectLearnerUseCase @Inject constructor(
    private val repository: LearnerRepository,
    private val currentLearnerManager: CurrentLearnerManager,
    private val preferencesManager: PreferencesManager,
    private val clock: AppClock,
) {
    suspend operator fun invoke(learnerId: String) {
        repository.touchLastActive(learnerId, clock.nowMillis())
        currentLearnerManager.selectLearner(learnerId)
        repository.getLearner(learnerId)?.let { preferencesManager.setUiLanguage(it.preferredLanguage) }
    }
}

class UpdateLearnerLanguageUseCase @Inject constructor(
    private val repository: LearnerRepository,
    private val preferencesManager: PreferencesManager,
    private val clock: AppClock,
) {
    suspend operator fun invoke(learnerId: String, languageTag: String) {
        val learner = repository.getLearner(learnerId) ?: return
        repository.updateLearner(learner.copy(preferredLanguage = languageTag, updatedAt = clock.nowMillis()))
        preferencesManager.setUiLanguage(languageTag)
    }
}

class DeleteLearnerUseCase @Inject constructor(private val repository: LearnerRepository) {
    suspend operator fun invoke(learnerId: String) = repository.deleteLearner(learnerId)
}
