package com.hackx.ruraledtech.feature.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLearnerScreen(
    onLearnerCreated: () -> Unit,
    viewModel: AddLearnerViewModel = hiltViewModel(),
) {
    val state by viewModel.formState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Who is learning?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Create a student profile. No email or phone number needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Student Name") },
                singleLine = true,
                isError = state.error != null,
                supportingText = { state.error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text("Grade / Class", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..8).forEach { grade ->
                    val selected = state.grade == grade
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onGradeChanged(grade) },
                        label = { Text("Grade $grade") },
                    )
                }
            }

            Text("Preferred Language", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Select language for lessons, quizzes, and voice read-aloud:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SupportedLanguage.entries.forEach { language ->
                    val selected = state.language == language
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onLanguageChanged(language) },
                        label = {
                            Text("${language.nativeName} (${language.displayName})")
                        },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.openTtsSettings() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
            ) {
                Icon(Icons.Filled.SettingsVoice, contentDescription = null)
                Text(" ⚙️ Add Language / Voice Package (Google TTS)", modifier = Modifier.padding(start = 6.dp))
            }

            Button(
                onClick = { viewModel.submit(onLearnerCreated) },
                enabled = !state.submitting,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp),
            ) {
                Text(if (state.submitting) "Creating profile..." else "Start Learning", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
