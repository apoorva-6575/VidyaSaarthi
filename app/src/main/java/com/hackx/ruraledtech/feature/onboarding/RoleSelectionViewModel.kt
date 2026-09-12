package com.hackx.ruraledtech.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UserRole(val storageKey: String) {
    STUDENT("student"),
    TEACHER("teacher"),
}

/**
 * The very first choice the app asks (before language, before anything else): who is using
 * this device right now. This is separate from learner *profiles* (PS section 6/15) — a
 * teacher and the learners they support share physical devices, but never share this role
 * split, since the two sides show completely different screens.
 */
@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val learnerRepository: LearnerRepository,
) : ViewModel() {

    fun selectRole(role: UserRole, onNavigate: (String) -> Unit) {
        viewModelScope.launch {
            preferencesManager.setUserRole(role.storageKey)
            if (role == UserRole.TEACHER) {
                onNavigate(Routes.TEACHER_LOGIN)
            } else {
                val learners = learnerRepository.observeLearners().first()
                if (learners.isNotEmpty()) {
                    onNavigate(Routes.LEARNER_SELECTION)
                } else {
                    onNavigate(Routes.LANGUAGE_SELECTION)
                }
            }
        }
    }
}
