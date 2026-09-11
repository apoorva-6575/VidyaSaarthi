package com.hackx.ruraledtech.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.domain.model.SubjectProgress

@Composable
fun HomeScreen(
    onOpenSubjects: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Hello, ${state.learner?.name ?: ""}", style = MaterialTheme.typography.headlineMedium)
                        ConnectivityBadge(state.connectivity, state.pendingSyncCount)
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                }
            }

            state.recommendation?.let { recommendation ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Recommended for you", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${recommendation.recommendationType.name.replace('_', ' ')} — ${recommendation.conceptName}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            item {
                Text("Your Progress", style = MaterialTheme.typography.titleMedium)
            }

            items(state.subjectProgress) { subject ->
                SubjectProgressRow(subject, onClick = { onOpenSubject(subject.subject) })
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().clickableRow(onOpenSubjects)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Text("Browse all subjects", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().clickableRow(onOpenProgress)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
                        Text("View full progress", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun SubjectProgressRow(subject: SubjectProgress, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickableRow(onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(subject.subject, style = MaterialTheme.typography.titleMedium)
                Text("${(subject.completionPercentage * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { subject.completionPercentage },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ConnectivityBadge(state: ConnectivityState, pendingCount: Int) {
    val (icon, label) = when (state) {
        ConnectivityState.OFFLINE -> Icons.Filled.CloudOff to if (pendingCount > 0) "Offline — $pendingCount waiting to sync" else "Offline — saved on this device"
        ConnectivityState.LIMITED -> Icons.Filled.CloudSync to "Weak connection — syncing essentials"
        ConnectivityState.ONLINE -> Icons.Filled.CloudDone to if (pendingCount > 0) "Online — syncing..." else "All progress synced"
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.height(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
    }
}
