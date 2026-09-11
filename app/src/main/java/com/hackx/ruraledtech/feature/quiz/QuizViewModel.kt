package com.hackx.ruraledtech.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.usecase.quiz.GetQuestionsForLessonUseCase
import com.hackx.ruraledtech.domain.usecase.quiz.QuizAttemptOutcome
import com.hackx.ruraledtech.domain.usecase.quiz.SubmitQuizAttemptUseCase
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QuizScreenState {
    data object Loading : QuizScreenState()
    data class Ready(
        val questions: List<Question>,
        val currentIndex: Int,
        val selectedOptionId: String?,
        val outcomes: List<QuizAttemptOutcome>,
        val questionStartedAtMs: Long,
    ) : QuizScreenState()
    data object Submitting : QuizScreenState()
    data class Completed(val outcomes: List<QuizAttemptOutcome>, val correctCount: Int, val total: Int) : QuizScreenState()
    data class Error(val message: String) : QuizScreenState()
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getQuestionsForLessonUseCase: GetQuestionsForLessonUseCase,
    private val submitQuizAttemptUseCase: SubmitQuizAttemptUseCase,
    private val currentLearnerManager: CurrentLearnerManager,
) : ViewModel() {

    private val lessonId: String = checkNotNull(savedStateHandle[Routes.ARG_LESSON_ID])

    private val _state = MutableStateFlow<QuizScreenState>(QuizScreenState.Loading)
    val state: StateFlow<QuizScreenState> = _state

    init {
        viewModelScope.launch {
            val questions = getQuestionsForLessonUseCase(lessonId)
            _state.value = if (questions.isEmpty()) {
                QuizScreenState.Error("No quiz questions are available for this lesson yet.")
            } else {
                QuizScreenState.Ready(questions, 0, null, emptyList(), System.currentTimeMillis())
            }
        }
    }

    fun selectOption(optionId: String) {
        val current = _state.value as? QuizScreenState.Ready ?: return
        _state.value = current.copy(selectedOptionId = optionId)
    }

    fun submitCurrentAnswer() {
        val current = _state.value as? QuizScreenState.Ready ?: return
        val selectedOptionId = current.selectedOptionId ?: return
        val learnerId = currentLearnerManager.currentLearnerId.value ?: return
        val question = current.questions[current.currentIndex]

        viewModelScope.launch {
            _state.value = QuizScreenState.Submitting
            val outcome = submitQuizAttemptUseCase(
                learnerId = learnerId,
                question = question,
                selectedOptionIds = listOf(selectedOptionId),
                responseTimeMs = System.currentTimeMillis() - current.questionStartedAtMs,
            )
            val newOutcomes = current.outcomes + outcome
            val nextIndex = current.currentIndex + 1
            _state.value = if (nextIndex >= current.questions.size) {
                QuizScreenState.Completed(
                    outcomes = newOutcomes,
                    correctCount = newOutcomes.count { it.correct },
                    total = current.questions.size,
                )
            } else {
                QuizScreenState.Ready(current.questions, nextIndex, null, newOutcomes, System.currentTimeMillis())
            }
        }
    }
}
