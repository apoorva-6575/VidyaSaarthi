package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Placeholder for Group 4's teacher platform (PS section 6.6/25). This exists so the
 * role split asked for at onboarding is real and demoable today, without Group 1
 * building out class management, analytics, or content distribution — that's Group 4's
 * scope, consuming the same LearnerRepository/ProgressRepository/SyncRepository interfaces
 * Group 1 already exposes.
 */
@Composable
fun TeacherHomeScreen(
    onSwitchToStudent: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Classes", style = MaterialTheme.typography.headlineMedium)
                androidx.compose.material3.IconButton(
                    onClick = { viewModel.refreshData() },
                    enabled = !uiState.isLoading && !uiState.isOffline
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Refresh, contentDescription = "Sync Classes")
                }
            }
            
            if (uiState.isOffline) {
                Text(
                    "You are currently offline. Showing cached classes.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (uiState.error != null) {
                Text(
                    "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (uiState.isLoading) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.classes.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
                    Text("No classes found", style = MaterialTheme.typography.titleMedium)
                    Text("Tap sync to fetch your classes from the server", textAlign = TextAlign.Center)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.classes.size) { index ->
                        val classGroup = uiState.classes[index]
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(classGroup.name, style = MaterialTheme.typography.titleMedium)
                                if (classGroup.grade != null || classGroup.subject != null) {
                                    Text(
                                        "${classGroup.grade ?: ""} ${classGroup.subject ?: ""}".trim(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
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
