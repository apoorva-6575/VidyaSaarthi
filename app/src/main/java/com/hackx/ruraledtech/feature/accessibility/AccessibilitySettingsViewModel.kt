package com.hackx.ruraledtech.feature.accessibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccessibilityUiState(val largeText: Boolean = false, val audioNav: Boolean = true)

@HiltViewModel
class AccessibilitySettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    val uiState = combine(preferencesManager.largeTextEnabled, preferencesManager.audioNavEnabled, ::AccessibilityUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccessibilityUiState())

    fun setLargeText(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setLargeTextEnabled(enabled) }
    }

    fun setAudioNav(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAudioNavEnabled(enabled) }
    }
}
