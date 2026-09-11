package com.hackx.ruraledtech.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(
    onSwitchedProfile: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenContentLibrary: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val learner by viewModel.learner.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(learner?.name ?: "", style = MaterialTheme.typography.headlineMedium)
            Text("Grade ${learner?.grade ?: ""} · ${learner?.preferredLanguage ?: ""}", style = MaterialTheme.typography.bodyMedium)

            Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
}
