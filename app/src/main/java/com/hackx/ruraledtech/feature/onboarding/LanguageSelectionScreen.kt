package com.hackx.ruraledtech.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.SupportedLanguage

/**
 * First screen the learner ever sees (PS section 5). No network call is possible here —
 * language choice is a local DataStore write, full stop.
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel(),
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Choose your language",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                text = "अपनी भाषा चुनें / اپنی زبان منتخب کریں",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(SupportedLanguage.entries) { language ->
                    Button(
                        onClick = { viewModel.selectLanguage(language.tag, onLanguageSelected) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text(
                            text = "${language.nativeName}  (${language.displayName})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.openTtsSettings() },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp)
            ) {
                Icon(Icons.Filled.SettingsVoice, contentDescription = null)
                Text(" ⚙️ Add Language / Voice Package (Google TTS)", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
