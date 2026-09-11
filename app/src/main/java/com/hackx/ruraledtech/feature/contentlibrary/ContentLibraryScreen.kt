package com.hackx.ruraledtech.feature.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.mesh.TransferState
import com.hackx.ruraledtech.p2p.mesh.TransferTask

@Composable
fun ContentLibraryScreen(viewModel: ContentLibraryViewModel = hiltViewModel()) {
    val packages by viewModel.packages.collectAsState()
    val meshState by viewModel.meshState.collectAsState()
    val transfers by viewModel.transfers.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { LearningMeshCard(meshState, transfers, onFindNearbyDevice = viewModel::findNearbyDevice) }

            if (packages.isEmpty()) {
                item {
                    EmptyState(
                        "No content installed",
                        "Content shared by your teacher or nearby devices will appear here.",
                    )
                }
            } else {
                items(packages) { pkg ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("${pkg.subject} — Grade ${pkg.grade}", style = MaterialTheme.typography.titleMedium)
                                Text("v${pkg.version} · ${pkg.sizeBytes / 1_000_000} MB", style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { viewModel.remove(pkg.packageId) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningMeshCard(meshState: MeshState, transfers: List<TransferTask>, onFindNearbyDevice: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Village Learning Mesh", style = MaterialTheme.typography.titleMedium)
            Text(meshStateLabel(meshState), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))

            transfers.forEach { transfer ->
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(transfer.packageId, style = MaterialTheme.typography.bodyMedium)
                        Text(transfer.state.name, style = MaterialTheme.typography.labelLarge)
                    }
                    LinearProgressIndicator(
                        progress = { transfer.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }

            if (meshState == MeshState.OFFLINE) {
                Button(onClick = onFindNearbyDevice, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Find nearby learning device")
                }
            }
        }
    }
}

private fun meshStateLabel(state: MeshState): String = when (state) {
    MeshState.OFFLINE -> "Not searching for nearby devices"
    MeshState.DISCOVERING -> "Searching for nearby devices..."
    MeshState.PEERS_AVAILABLE -> "Found a nearby device"
    MeshState.CONNECTED -> "Connected to a nearby device"
    MeshState.TRANSFERRING -> "Transferring content..."
    MeshState.IDLE -> "Connected — ready to share content"
    MeshState.ERROR -> "Could not reach a nearby device"
}
