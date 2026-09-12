package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity

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
    onOpenClassAnalytics: (String) -> Unit,
    onOpenContentLibrary: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create class")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("My Classes", style = MaterialTheme.typography.headlineMedium)
                    if (uiState.teacherName.isNotBlank()) {
                        Text(uiState.teacherName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !uiState.isLoading && !uiState.isOffline
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync Classes")
                    }
                    IconButton(onClick = { viewModel.logOut(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                }
            }

            if (uiState.isOffline) {
                Text(
                    "You are currently offline. Showing cached classes.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
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
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                    )
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
            } else if (uiState.classes.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
                    Text("No classes found", style = MaterialTheme.typography.titleMedium)
                    Text("Tap sync to fetch your classes, or the + button to create one", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.classes.size) { index ->
                        val classGroup = uiState.classes[index]
                        val learnerCount = uiState.learnerCounts[classGroup.classId] ?: 0
                        ClassRow(
                            classGroup = classGroup,
                            learnerCount = learnerCount,
                            onClick = { onOpenClassAnalytics(classGroup.classId) }
                        )
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

    if (showCreateDialog) {
        CreateClassDialog(
            creating = uiState.creatingClass,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, grade, subject ->
                viewModel.createClass(name, grade, subject) { success ->
                    if (success) showCreateDialog = false
                }
            },
        )
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

@Composable
private fun ClassRow(classGroup: ClassGroupEntity, learnerCount: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(classGroup.name, style = MaterialTheme.typography.titleMedium)
                if (classGroup.grade != null || classGroup.subject != null) {
                    Text(
                        "${classGroup.grade ?: ""} ${classGroup.subject ?: ""}".trim(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Code: ${classGroup.classId.take(6).uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (learnerCount > 0) "• $learnerCount student${if (learnerCount > 1) "s" else ""}" else "• No students yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun CreateClassDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, grade: String?, subject: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Class name") }, singleLine = true)
                OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Grade (optional)") }, singleLine = true)
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, grade.ifBlank { null }, subject.ifBlank { null }) },
                enabled = name.isNotBlank() && !creating,
            ) {
                Text(if (creating) "Creating..." else "Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
