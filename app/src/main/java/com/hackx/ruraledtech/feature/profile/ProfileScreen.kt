package com.hackx.ruraledtech.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.SupportedLanguage

@Composable
fun ProfileScreen(
    onSwitchedProfile: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenContentLibrary: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val learner by viewModel.learner.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(learner?.name ?: "", style = MaterialTheme.typography.headlineMedium)
            Text("Grade ${learner?.grade ?: ""} · ${SupportedLanguage.fromTag(learner?.preferredLanguage ?: "en").nativeName}", style = MaterialTheme.typography.bodyMedium)

            Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showLanguageDialog = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Change language")
                }
                OutlinedButton(onClick = onOpenContentLibrary, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Content library & storage")
                }
                OutlinedButton(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Accessibility settings")
                }
                Button(
                    onClick = {
                        viewModel.switchProfile()
                        onSwitchedProfile()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Switch learner")
                }
            }
        }
    }

    if (showLanguageDialog) {
        val currentLanguage = SupportedLanguage.fromTag(learner?.preferredLanguage ?: "en")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose your language") },
            text = {
                Column {
                    SupportedLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = language == currentLanguage,
                                onClick = {
                                    viewModel.changeLanguage(language)
                                    showLanguageDialog = false
                                },
                            )
                            Text("${language.nativeName} (${language.displayName})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            },
        )
    }
}
