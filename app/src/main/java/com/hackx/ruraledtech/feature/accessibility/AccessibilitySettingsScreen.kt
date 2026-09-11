package com.hackx.ruraledtech.feature.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AccessibilitySettingsScreen(viewModel: AccessibilitySettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Accessibility", style = MaterialTheme.typography.headlineMedium)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Large text", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = state.largeText, onCheckedChange = viewModel::setLargeText)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Audio navigation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = state.audioNav, onCheckedChange = viewModel::setAudioNav)
            }
        }
    }
}
