package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Group 1 owns only the role split; the real teacher platform is Group 4's scope. */
@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    fun switchToStudentRole(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setUserRole(null)
            onDone()
        }
    }
}
