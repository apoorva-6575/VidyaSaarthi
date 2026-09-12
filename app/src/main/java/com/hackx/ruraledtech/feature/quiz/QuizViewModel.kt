package com.hackx.ruraledtech.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.quiz.GetQuestionsForLessonUseCase
import com.hackx.ruraledtech.domain.usecase.quiz.QuizAttemptOutcome
import com.hackx.ruraledtech.domain.usecase.quiz.SubmitQuizAttemptUseCase
import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine
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
    private val learnerRepository: LearnerRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
) : ViewModel() {

    private val lessonId: String = checkNotNull(savedStateHandle[Routes.ARG_LESSON_ID])

    private val _state = MutableStateFlow<QuizScreenState>(QuizScreenState.Loading)
    val state: StateFlow<QuizScreenState> = _state

    private var speechJob: kotlinx.coroutines.Job? = null
    val speakingState = MutableStateFlow<String?>(null)
    val learnerLanguage = MutableStateFlow("en")

    init {
        viewModelScope.launch {
            val learnerId = currentLearnerManager.currentLearnerId.value
            if (learnerId != null) {
                learnerRepository.getLearner(learnerId)?.let { learnerLanguage.value = it.preferredLanguage }
            }
            val questions = if (learnerId != null) {
                getQuestionsForLessonUseCase(lessonId, learnerId)
            } else {
                emptyList()
            }
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

    /** "Read question" for low-literacy support (PS section 25/26) — offline TTS in the learner's own language. */
    fun readQuestionAloud(question: Question) {
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            textToSpeechEngine.stop()
            val learnerId = currentLearnerManager.currentLearnerId.value
            val languageTag = if (learnerId != null) {
                learnerRepository.getLearner(learnerId)?.preferredLanguage ?: question.language
            } else question.language
            speakingState.value = languageTag
            val optionsText = question.options.mapIndexed { idx, opt -> "Option ${idx + 1}: ${opt.text}" }.joinToString(". ")
            textToSpeechEngine.speak("${question.prompt}. $optionsText", languageTag)
        }
    }

    fun stopSpeech() {
        speechJob?.cancel()
        speechJob = null
        speakingState.value = null
        viewModelScope.launch { textToSpeechEngine.stop() }
    }

    fun playAudioGuide() {
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            textToSpeechEngine.stop()
            val lang = learnerLanguage.value
            speakingState.value = lang
            textToSpeechEngine.speak(com.hackx.ruraledtech.feature.common.UiStrings.quizAudioGuide(lang), lang)
        }
    }

    fun submitCurrentAnswer() {
        val current = _state.value as? QuizScreenState.Ready ?: return
        val selectedOptionId = current.selectedOptionId ?: return
        val learnerId = currentLearnerManager.currentLearnerId.value ?: return
        val question = current.questions[current.currentIndex]

        speechJob?.cancel()
        speechJob = null
        viewModelScope.launch {
            textToSpeechEngine.stop()
            _state.value = QuizScreenState.Submitting
            val outcome = submitQuizAttemptUseCase(
                learnerId = learnerId,
                question = question,
                selectedOptionIds = listOf(selectedOptionId),
                responseTimeMs = System.currentTimeMillis() - current.questionStartedAtMs,
            )
            
            // Low-literacy immediate spoken feedback in regional language
            val lang = learnerLanguage.value
            val feedbackText = if (outcome.correct) {
                com.hackx.ruraledtech.feature.common.UiStrings.correctAnswerPraise(lang)
            } else {
                com.hackx.ruraledtech.feature.common.UiStrings.tryAgainPrompt(lang)
            }
            textToSpeechEngine.speak(feedbackText, lang)

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

    override fun onCleared() {
        speechJob?.cancel()
        speechJob = null
        kotlinx.coroutines.runBlocking { textToSpeechEngine.stop() }
    }
}
