package com.hackx.ruraledtech.feature.learner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.usecase.learner.GetLearnersUseCase
import com.hackx.ruraledtech.domain.usecase.learner.SelectLearnerUseCase
import com.hackx.ruraledtech.feature.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Renders the shared-device learner picker (PS section 6/15). Selecting a learner here
 * updates [com.hackx.ruraledtech.core.session.CurrentLearnerManager], which is what keeps
 * every downstream screen scoped to the right person's data — never the device's.
 */
@HiltViewModel
class LearnerSelectionViewModel @Inject constructor(
    getLearnersUseCase: GetLearnersUseCase,
    private val selectLearnerUseCase: SelectLearnerUseCase,
) : ViewModel() {

    val learners: StateFlow<UiState<List<Learner>>> = getLearnersUseCase()
        .map<List<Learner>, UiState<List<Learner>>> { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun selectLearner(learnerId: String, onSelected: () -> Unit) {
        viewModelScope.launch {
            selectLearnerUseCase(learnerId)
            onSelected()
        }
    }
}
