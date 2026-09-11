package com.hackx.ruraledtech.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.domain.usecase.learner.CreateLearnerUseCase
import com.hackx.ruraledtech.domain.usecase.learner.SelectLearnerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLearnerFormState(
    val name: String = "",
    val grade: Int = 5,
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AddLearnerViewModel @Inject constructor(
    private val createLearnerUseCase: CreateLearnerUseCase,
    private val selectLearnerUseCase: SelectLearnerUseCase,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddLearnerFormState())
    val formState: StateFlow<AddLearnerFormState> = _formState

    fun onNameChanged(name: String) {
        _formState.value = _formState.value.copy(name = name, error = null)
    }

    fun onGradeChanged(grade: Int) {
        _formState.value = _formState.value.copy(grade = grade)
    }

    fun submit(onCreated: () -> Unit) {
        val name = _formState.value.name.trim()
        if (name.isEmpty()) {
            _formState.value = _formState.value.copy(error = "Please enter a name")
            return
        }
        viewModelScope.launch {
            _formState.value = _formState.value.copy(submitting = true, error = null)
            val language = preferencesManager.uiLanguage.first()
            val learner = createLearnerUseCase(name, _formState.value.grade, language)
            selectLearnerUseCase(learner.learnerId)
            preferencesManager.setOnboardingComplete(true)
            _formState.value = _formState.value.copy(submitting = false)
            onCreated()
        }
    }
}
