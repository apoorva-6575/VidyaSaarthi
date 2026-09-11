package com.hackx.ruraledtech.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.model.SubjectProgress
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.progress.ObserveSubjectProgressUseCase
import com.hackx.ruraledtech.domain.usecase.recommendation.ObserveRecommendationUseCase
import com.hackx.ruraledtech.domain.usecase.sync.ObserveConnectivityUseCase
import com.hackx.ruraledtech.domain.usecase.sync.ObservePendingSyncCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val learner: Learner? = null,
    val connectivity: ConnectivityState = ConnectivityState.OFFLINE,
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val recommendation: Recommendation? = null,
    val pendingSyncCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    currentLearnerManager: CurrentLearnerManager,
    learnerRepository: LearnerRepository,
    observeSubjectProgressUseCase: ObserveSubjectProgressUseCase,
    observeRecommendationUseCase: ObserveRecommendationUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    observePendingSyncCountUseCase: ObservePendingSyncCountUseCase,
) : ViewModel() {

    private val learnerId = currentLearnerManager.currentLearnerId.filterNotNull()

    private val learnerFlow = learnerId.flatMapLatest { id -> learnerRepository.observeLearners().map { list -> list.firstOrNull { it.learnerId == id } } }
    private val subjectProgressFlow = learnerId.flatMapLatest { observeSubjectProgressUseCase(it) }
    private val recommendationFlow = learnerId.flatMapLatest { observeRecommendationUseCase(it) }

    val uiState = combine(
        learnerFlow,
        observeConnectivityUseCase(),
        subjectProgressFlow,
        recommendationFlow,
        observePendingSyncCountUseCase(),
    ) { learner, connectivity, subjects, recommendation, pending ->
        HomeUiState(learner, connectivity, subjects, recommendation, pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
