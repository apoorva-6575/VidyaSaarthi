package com.hackx.ruraledtech.feature.passport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.passport.transport.PassportImportResult

@Composable
fun PassportScreen(viewModel: PassportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.startMesh() }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Learning Passport", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Move this learner's progress to another device by sending an encrypted passport over a nearby offline connection — no internet needed.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (state.pendingPassport != null) {
                ImportPassportCard(state, viewModel)
            }

            ExportPassportCard(state, viewModel)
        }
    }
}

@Composable
private fun ExportPassportCard(state: PassportUiState, viewModel: PassportViewModel) {
    var pin by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Send this learner's passport", style = MaterialTheme.typography.titleMedium)

            val meshStatus = when (state.meshState) {
                MeshState.OFFLINE, MeshState.IDLE -> "Starting nearby search..."
                MeshState.DISCOVERING -> "Looking for a nearby device..."
                MeshState.PEERS_AVAILABLE -> "Found a nearby device — connecting..."
                MeshState.CONNECTED, MeshState.TRANSFERRING -> "Connected to ${state.connectedEndpoints.size} nearby device(s)"
                MeshState.ERROR -> "Nearby search failed"
            }
            Text(meshStatus, style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                label = { Text("Choose a 4-6 digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "The receiving device will need this exact PIN to unlock the passport — share it with them separately (say it out loud, don't send it over the same connection).",
                style = MaterialTheme.typography.bodySmall,
            )

            Button(
                onClick = { viewModel.exportPassport(pin) },
                enabled = pin.length >= 4 && state.connectedEndpoints.isNotEmpty() && !state.exporting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (state.exporting) "Sending..." else "Send to nearby device")
            }

            if (state.exportSent) {
                Text("Passport sent. Ask the other device to enter the same PIN to finish importing.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ImportPassportCard(state: PassportUiState, viewModel: PassportViewModel) {
    var pin by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A passport was received", style = MaterialTheme.typography.titleMedium)
            Text("Enter the PIN the sender gave you to unlock and import it.", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { viewModel.importPendingPassport(pin) },
                enabled = pin.length >= 4 && !state.importing,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (state.importing) "Importing..." else "Unlock and import")
            }

            OutlinedButton(onClick = viewModel::dismissPendingPassport, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }

            state.importResult?.let { result ->
                Text(importResultMessage(result), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun importResultMessage(result: PassportImportResult): String = when (result) {
    is PassportImportResult.Success -> "Imported successfully for learner ${result.learnerId}."
    PassportImportResult.InvalidPin -> "That PIN didn't work — check it with the sender and try again."
    PassportImportResult.MalformedPassport -> "This passport looks corrupted and can't be imported."
    is PassportImportResult.UnsupportedVersion -> "This passport (v${result.passportVersion}) needs a newer app version (this device supports up to v${result.supportedVersion})."
    is PassportImportResult.Error -> "Import failed: ${result.message}"
}
