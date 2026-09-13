package com.hackx.ruraledtech.feature.lessons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Lesson
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.content.GetLessonsUseCase
import com.hackx.ruraledtech.feature.common.UiState
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLessonsUseCase: GetLessonsUseCase,
    private val learnerRepository: LearnerRepository,
    private val currentLearnerManager: CurrentLearnerManager,
) : ViewModel() {

    private val subject: String = checkNotNull(savedStateHandle[Routes.ARG_SUBJECT])

    private val _state = MutableStateFlow<UiState<List<Lesson>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Lesson>>> = _state

    init {
        viewModelScope.launch {
            val learnerId = currentLearnerManager.currentLearnerId.value ?: return@launch
            val learner = learnerRepository.getLearner(learnerId) ?: return@launch
            val lessons = getLessonsUseCase(subject, learner.grade, learner.preferredLanguage, learnerId)
            _state.value = if (lessons.isEmpty()) UiState.Empty else UiState.Success(lessons)
        }
    }
}
