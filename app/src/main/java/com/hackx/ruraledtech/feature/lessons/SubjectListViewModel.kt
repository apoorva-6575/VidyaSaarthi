package com.hackx.ruraledtech.feature.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.content.GetSubjectsUseCase
import com.hackx.ruraledtech.feature.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectListViewModel @Inject constructor(
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val learnerRepository: LearnerRepository,
    private val currentLearnerManager: CurrentLearnerManager,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<String>>>(UiState.Loading)
    val state: StateFlow<UiState<List<String>>> = _state

    init {
        viewModelScope.launch {
            val learnerId = currentLearnerManager.currentLearnerId.value ?: return@launch
            val learner = learnerRepository.getLearner(learnerId) ?: return@launch
            val subjects = getSubjectsUseCase(learner.grade, learnerId)
            _state.value = if (subjects.isEmpty()) UiState.Empty else UiState.Success(subjects)
        }
    }
}
