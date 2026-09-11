package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.TeacherAuthStore
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.TeacherCreateDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TeacherAuthMode { LOGIN, REGISTER }

data class TeacherLoginUiState(
    val mode: TeacherAuthMode = TeacherAuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

/**
 * A teacher account only exists on Group 4's backend, so — unlike learner profiles — this
 * screen genuinely requires connectivity. [TeacherAuthStore] persists the session (token +
 * real teacher id) so [TeacherHomeViewModel] no longer needs the "teacher-123" placeholder.
 */
@HiltViewModel
class TeacherLoginViewModel @Inject constructor(
    private val api: RuralEdTechApi,
    private val authStore: TeacherAuthStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherLoginUiState())
    val uiState: StateFlow<TeacherLoginUiState> = _uiState

    fun setMode(mode: TeacherAuthMode) {
        _uiState.value = _uiState.value.copy(mode = mode, error = null)
    }

    fun onNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || (state.mode == TeacherAuthMode.REGISTER && state.name.isBlank())) {
            _uiState.value = state.copy(error = "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(submitting = true, error = null)
            try {
                if (state.mode == TeacherAuthMode.REGISTER) {
                    val registerResponse = api.registerTeacher(TeacherCreateDto(name = state.name, email = state.email, password = state.password))
                    if (!registerResponse.isSuccessful) {
                        _uiState.value = _uiState.value.copy(submitting = false, error = "Registration failed: ${registerResponse.code()}")
                        return@launch
                    }
                }

                val loginResponse = api.loginTeacher(state.email, state.password)
                if (!loginResponse.isSuccessful || loginResponse.body() == null) {
                    _uiState.value = _uiState.value.copy(submitting = false, error = "Login failed — check your email and password")
                    return@launch
                }

                // Save the token first so the auth interceptor attaches it to the next call.
                val token = loginResponse.body()!!.access_token
                authStore.saveSession(token = token, teacherId = "", name = state.name, email = state.email)

                val meResponse = api.getCurrentTeacher()
                if (!meResponse.isSuccessful || meResponse.body() == null) {
                    _uiState.value = _uiState.value.copy(submitting = false, error = "Signed in, but couldn't load your profile")
                    return@launch
                }

                val teacher = meResponse.body()!!
                authStore.saveSession(token = token, teacherId = teacher.id, name = teacher.name, email = teacher.email)
                _uiState.value = _uiState.value.copy(submitting = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = e.message ?: "Network error — check your connection")
            }
        }
    }
}
