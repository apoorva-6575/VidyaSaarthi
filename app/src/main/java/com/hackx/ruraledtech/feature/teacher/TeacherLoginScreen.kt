package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.AnimatedBrandLogo

@Composable
fun TeacherLoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: TeacherLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            AnimatedBrandLogo(modifier = Modifier.padding(bottom = 24.dp))

            TabRow(selectedTabIndex = if (state.mode == TeacherAuthMode.LOGIN) 0 else 1) {
                Tab(
                    selected = state.mode == TeacherAuthMode.LOGIN,
                    onClick = { viewModel.setMode(TeacherAuthMode.LOGIN) },
                    text = { Text("Log in") },
                )
                Tab(
                    selected = state.mode == TeacherAuthMode.REGISTER,
                    onClick = { viewModel.setMode(TeacherAuthMode.REGISTER) },
                    text = { Text("Register") },
                )
            }

            Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.mode == TeacherAuthMode.REGISTER) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("Full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = state.error != null,
                    supportingText = { state.error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { viewModel.submit(onLoginSuccess) },
                    enabled = !state.submitting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(
                        if (state.submitting) "Please wait..." else if (state.mode == TeacherAuthMode.LOGIN) "Log in" else "Create account",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
