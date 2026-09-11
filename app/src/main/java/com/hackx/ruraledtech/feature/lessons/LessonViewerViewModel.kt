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

    init {
        viewModelScope.launch {
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
        val learnerId = currentLearnerManager.currentLearnerId.value ?: return
        viewModelScope.launch { updateLessonProgressUseCase(learnerId, lessonId, 1f, completed = true, lastPosition = 0) }
    }

    /** Reads a text block aloud in the current learner's own language via offline TTS (PS section 25/26). */
    fun speakText(text: String) {
        viewModelScope.launch {
            val learnerId = currentLearnerManager.currentLearnerId.value ?: return@launch
            val languageTag = learnerRepository.getLearner(learnerId)?.preferredLanguage ?: "en"
            textToSpeechEngine.speak(text, languageTag)
        }
    }

    override fun onCleared() {
        audioManager.release()
        // stop() only wraps a quick synchronous Android TTS call; runBlocking is fine here
        // since viewModelScope may already be cancelling by the time onCleared runs.
        kotlinx.coroutines.runBlocking { textToSpeechEngine.stop() }
    }
}
