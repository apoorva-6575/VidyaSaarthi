package com.hackx.ruraledtech.feature.lessons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.audio.AudioManager
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.domain.model.ContentLookupResult
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.content.GetLessonUseCase
import com.hackx.ruraledtech.domain.usecase.progress.UpdateLessonProgressUseCase
import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine
import com.hackx.ruraledtech.feature.common.UiState
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLessonUseCase: GetLessonUseCase,
    private val updateLessonProgressUseCase: UpdateLessonProgressUseCase,
    private val currentLearnerManager: CurrentLearnerManager,
    private val learnerRepository: LearnerRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    val audioManager: AudioManager,
) : ViewModel() {

    val lessonId: String = checkNotNull(savedStateHandle[Routes.ARG_LESSON_ID])

    private val _state = MutableStateFlow<UiState<ContentLookupResult>>(UiState.Loading)
    val state: StateFlow<UiState<ContentLookupResult>> = _state

    private var speechJob: kotlinx.coroutines.Job? = null

    val speakingState = MutableStateFlow<String?>(null)
    val learnerLanguage = MutableStateFlow("en")

    init {
        viewModelScope.launch {
            val learnerId = currentLearnerManager.currentLearnerId.value
            if (learnerId != null) {
                learnerRepository.getLearner(learnerId)?.let { learnerLanguage.value = it.preferredLanguage }
            }
            when (val result = getLessonUseCase(lessonId)) {
                is ContentLookupResult.Available -> _state.value = UiState.Success(result)
                is ContentLookupResult.NotAvailableLocally -> _state.value = UiState.Empty
                else -> _state.value = UiState.Success(result)
            }
        }
    }

    fun markStarted() {
        val learnerId = currentLearnerManager.currentLearnerId.value ?: return
        viewModelScope.launch { updateLessonProgressUseCase(learnerId, lessonId, 0.1f, completed = false, lastPosition = 0) }
    }

    fun markCompleted() {
        stopSpeech()
        val learnerId = currentLearnerManager.currentLearnerId.value ?: return
        viewModelScope.launch { updateLessonProgressUseCase(learnerId, lessonId, 1f, completed = true, lastPosition = 0) }
    }

    /** Reads a text block aloud in the current learner's own language via offline TTS (PS section 25/26). */
    fun speakText(text: String) {
        audioManager.stop()
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            textToSpeechEngine.stop()
            val learnerId = currentLearnerManager.currentLearnerId.value
            val languageTag = if (learnerId != null) {
                learnerRepository.getLearner(learnerId)?.preferredLanguage ?: learnerLanguage.value
            } else learnerLanguage.value
            speakingState.value = languageTag
            textToSpeechEngine.speak(text, languageTag)
        }
    }

    fun stopSpeech() {
        speechJob?.cancel()
        speechJob = null
        speakingState.value = null
        viewModelScope.launch {
            textToSpeechEngine.stop()
        }
    }

    override fun onCleared() {
        speechJob?.cancel()
        speechJob = null
        audioManager.release()
        kotlinx.coroutines.runBlocking { textToSpeechEngine.stop() }
    }
}
