package com.hackx.ruraledtech.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.domain.usecase.learner.EnsureTtsVoiceInstalledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val ensureTtsVoiceInstalledUseCase: EnsureTtsVoiceInstalledUseCase,
) : ViewModel() {

    fun selectLanguage(languageTag: String, onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setUiLanguage(languageTag)
            ensureTtsVoiceInstalledUseCase(languageTag)
            onDone()
        }
    }
}
