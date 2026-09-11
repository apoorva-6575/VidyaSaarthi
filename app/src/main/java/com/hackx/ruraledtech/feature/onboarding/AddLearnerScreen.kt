package com.hackx.ruraledtech.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.SupportedLanguage

@Composable
fun AddLearnerScreen(
    onLearnerCreated: () -> Unit,
    viewModel: AddLearnerViewModel = hiltViewModel(),
) {
    val state by viewModel.formState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Who is learning?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Create a profile. No email or phone number needed.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Name") },
                singleLine = true,
                isError = state.error != null,
                supportingText = { state.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Grade", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..8).forEach { grade ->
                    val selected = state.grade == grade
                    if (selected) {
                        Button(onClick = { viewModel.onGradeChanged(grade) }) { Text("$grade") }
                    } else {
                        OutlinedButton(onClick = { viewModel.onGradeChanged(grade) }) { Text("$grade") }
                    }
                }
            }

            Text("Language", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportedLanguage.entries.forEach { language ->
                    val selected = state.language == language
                    if (selected) {
                        Button(onClick = { viewModel.onLanguageChanged(language) }) { Text(language.nativeName) }
                    } else {
                        OutlinedButton(onClick = { viewModel.onLanguageChanged(language) }) { Text(language.nativeName) }
                    }
                }
            }

            Button(
                onClick = { viewModel.submit(onLearnerCreated) },
                enabled = !state.submitting,
                modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(56.dp),
            ) {
                Text(if (state.submitting) "Creating..." else "Start Learning", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
