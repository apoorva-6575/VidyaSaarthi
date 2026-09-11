package com.hackx.ruraledtech.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.domain.usecase.learner.CreateLearnerUseCase
import com.hackx.ruraledtech.domain.usecase.learner.SelectLearnerUseCase
import com.hackx.ruraledtech.feature.common.SupportedLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLearnerFormState(
    val name: String = "",
    val grade: Int = 5,
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val submitting: Boolean = false,
    val error: String? = null,
)

/**
 * Every learner creation asks for a language here — not just the very first one during
 * onboarding. Previously "+ Add learner" (for a second/third learner on a shared device)
 * skipped this screen entirely and silently inherited whatever the device-wide language
 * happened to be, which meant a second child could never actually pick their own language.
 */
@HiltViewModel
class AddLearnerViewModel @Inject constructor(
    private val createLearnerUseCase: CreateLearnerUseCase,
    private val selectLearnerUseCase: SelectLearnerUseCase,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddLearnerFormState())
    val formState: StateFlow<AddLearnerFormState> = _formState

    init {
        viewModelScope.launch {
            val current = preferencesManager.uiLanguage.first()
            _formState.value = _formState.value.copy(language = SupportedLanguage.fromTag(current))
        }
    }

    fun onNameChanged(name: String) {
        _formState.value = _formState.value.copy(name = name, error = null)
    }

    fun onGradeChanged(grade: Int) {
        _formState.value = _formState.value.copy(grade = grade)
    }

    fun onLanguageChanged(language: SupportedLanguage) {
        _formState.value = _formState.value.copy(language = language)
    }

    fun submit(onCreated: () -> Unit) {
        val name = _formState.value.name.trim()
        if (name.isEmpty()) {
            _formState.value = _formState.value.copy(error = "Please enter a name")
            return
        }
        viewModelScope.launch {
            _formState.value = _formState.value.copy(submitting = true, error = null)
            val learner = createLearnerUseCase(name, _formState.value.grade, _formState.value.language.tag)
            selectLearnerUseCase(learner.learnerId)
            preferencesManager.setOnboardingComplete(true)
            _formState.value = _formState.value.copy(submitting = false)
            onCreated()
        }
    }
}
