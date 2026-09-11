package com.hackx.ruraledtech.core.session

import com.hackx.ruraledtech.core.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for "who is learning right now." Every learner-scoped
 * ViewModel/repository call keys off [currentLearnerId] instead of the Android account or
 * device identity (PS section 6 / 30) so profile switches never leak data across learners.
 */
@Singleton
class CurrentLearnerManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val externalScope: CoroutineScope,
) {
    val currentLearnerId: StateFlow<String?> = preferencesManager.currentLearnerId
        .stateIn(externalScope, SharingStarted.Eagerly, null)

    fun selectLearner(learnerId: String) {
        externalScope.launch { preferencesManager.setCurrentLearnerId(learnerId) }
    }

    fun clearSelection() {
        externalScope.launch { preferencesManager.setCurrentLearnerId(null) }
    }

    fun requireLearnerId(): String =
        currentLearnerId.value ?: error("No learner selected — screens beyond LearnerSelection must not be reachable without one")
}
