package com.hackx.ruraledtech.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.SubjectProgress
import com.hackx.ruraledtech.domain.usecase.progress.ObserveMasteryUseCase
import com.hackx.ruraledtech.domain.usecase.progress.ObserveSubjectProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProgressUiState(
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val mastery: List<Mastery> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    currentLearnerManager: CurrentLearnerManager,
    observeSubjectProgressUseCase: ObserveSubjectProgressUseCase,
    observeMasteryUseCase: ObserveMasteryUseCase,
) : ViewModel() {

    private val learnerId = currentLearnerManager.currentLearnerId.filterNotNull()

    val uiState = combine(
        learnerId.flatMapLatest { observeSubjectProgressUseCase(it) },
        learnerId.flatMapLatest { observeMasteryUseCase(it) },
    ) { subjects, mastery ->
        ProgressUiState(subjects, mastery.sortedBy { it.score })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())
}
