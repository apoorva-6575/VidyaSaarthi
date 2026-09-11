package com.hackx.ruraledtech.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.core.datastore.TeacherAuthStore
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val currentLearnerManager: CurrentLearnerManager,
    private val teacherAuthStore: TeacherAuthStore,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        viewModelScope.launch {
            val userRole = preferencesManager.userRole.first()
            val onboardingComplete = preferencesManager.onboardingComplete.first()
            val currentLearnerId = currentLearnerManager.currentLearnerId.value
                ?: preferencesManager.currentLearnerId.first()

            _startDestination.value = when {
                userRole == null -> Routes.ROLE_SELECTION
                userRole == "teacher" -> {
                    val hasSession = teacherAuthStore.currentTeacherId() != null
                    if (hasSession) Routes.TEACHER_HOME else Routes.TEACHER_LOGIN
                }
                !onboardingComplete -> Routes.LANGUAGE_SELECTION
                currentLearnerId == null -> Routes.LEARNER_SELECTION
                else -> Routes.HOME
            }
        }
    }
}
