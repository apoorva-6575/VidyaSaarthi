package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel


/**
 * Real teacher-facing class list with offline caching (PS section 6.6/25): sync fetches
 * from the backend and writes through Room, so classes remain visible offline afterward.
 * Class creation and analytics require connectivity (accounts/classes only exist server
 * side); the class list itself does not.
 */
import androidx.compose.runtime.LaunchedEffect

@Composable
fun TeacherHomeScreen(
    onSwitchToStudent: () -> Unit,
    onOpenContentLibrary: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Teacher Dashboard", style = MaterialTheme.typography.headlineMedium)
                    if (uiState.teacherName.isNotBlank()) {
                        Text(uiState.teacherName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync Classes")
                    }
                    IconButton(onClick = { viewModel.logOut(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                }
            }

            uiState.dashboard?.let { dashboard ->
                DashboardSummaryCard(dashboard, modifier = Modifier.padding(bottom = 12.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onOpenContentLibrary() },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Curriculum & Mesh Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Manage offline packages & distribute to students via P2P mesh",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            OutlinedButton(
                onClick = { viewModel.switchToStudentRole(onSwitchToStudent) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Switch to student view")
            }
        }
    }
}

@Composable
private fun DashboardSummaryCard(dashboard: TeacherDashboardSummary, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            if (dashboard.cachedAt != null) {
                Text(
                    "Cached from ${formatCachedAt(dashboard.cachedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${dashboard.learnersCount}", style = MaterialTheme.typography.headlineSmall)
                    Text("Learners", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${dashboard.classesCount}", style = MaterialTheme.typography.headlineSmall)
                    Text("Classes", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(dashboard.averageMastery * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall)
                    Text("Avg mastery", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (dashboard.weakConcepts.isNotEmpty()) {
                Text(
                    "Needs attention: ${dashboard.weakConcepts.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

private fun formatCachedAt(timestampMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestampMillis))
}


