package com.hackx.ruraledtech.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    fun selectRole(role: UserRole, onDone: (UserRole) -> Unit) {
        viewModelScope.launch {
            preferencesManager.setUserRole(role.storageKey)
            onDone(role)
        }
    }
}
