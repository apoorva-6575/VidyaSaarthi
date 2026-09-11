package com.hackx.ruraledtech.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val currentLearnerManager: CurrentLearnerManager,
    learnerRepository: LearnerRepository,
) : ViewModel() {

    val learner: StateFlow<Learner?> = currentLearnerManager.currentLearnerId
        .filterNotNull()
        .flatMapLatest { id -> learnerRepository.observeLearners().map { list -> list.firstOrNull { it.learnerId == id } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun switchProfile() {
        currentLearnerManager.clearSelection()
    }
}
