package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.ui.theme.ErrorRed
import com.hackx.ruraledtech.ui.theme.MasteredConcept
import com.hackx.ruraledtech.ui.theme.ProficientConcept
import com.hackx.ruraledtech.ui.theme.WeakConcept

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.hackx.ruraledtech.core.permissions.P2PPermissions
import com.hackx.ruraledtech.p2p.mesh.MeshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassAnalyticsScreen(
    onBack: (() -> Unit)? = null,
    onOpenContentLibrary: (() -> Unit)? = null,
    viewModel: ClassAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val meshState by viewModel.meshState.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()
    val shortCode = viewModel.classId.take(6).uppercase()
    val clipboardManager = LocalClipboardManager.current
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var lastSharedPackage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) {
            viewModel.startMesh()
        }
    }

    val isMeshActive = meshState != MeshState.OFFLINE && meshState != MeshState.ERROR

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.classGroup?.name ?: "Classroom Details") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val report = viewModel.exportClassReport()
                        reportContent = report
                        showReportDialog = true
                    }) {
                        Icon(Icons.Filled.Assignment, contentDescription = "Export Report")
                    }
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            state.error != null && state.classAverages.isEmpty() && state.learnerRows.isEmpty() ->
                EmptyState("Couldn't load classroom data", state.error!!, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Class Join Code Banner
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state.classGroup?.name ?: "Classroom",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.classGroup?.grade != null || state.classGroup?.subject != null) {
                                Text(
                                    text = "${state.classGroup?.grade ?: ""} ${state.classGroup?.subject ?: ""}".trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "STUDENT JOIN CODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        shortCode,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Students enter this\ncode to join offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Class Learning Materials & P2P Sharing Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CellTower,
                                        contentDescription = null,
                                        tint = if (isMeshActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "Class Materials & P2P Share",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                if (connectedEndpoints.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(" ${connectedEndpoints.size} peer(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            if (lastSharedPackage != null) {
                                Text(
                                    "📡 Broadcasting $lastSharedPackage to connected students...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            if (packages.isEmpty()) {
                                Text(
                                    "No learning packages uploaded yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    packages.forEach { pkg ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "${pkg.subject} — Grade ${pkg.grade}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    pkg.packageId,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    permissionLauncher.launch(P2PPermissions.required)
                                                    viewModel.startMesh()
                                                    viewModel.sharePackageToClass(pkg.packageId)
                                                    lastSharedPackage = pkg.packageId
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text(" 📤 Share (P2P)", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onOpenContentLibrary != null) {
                                    OutlinedButton(
                                        onClick = onOpenContentLibrary,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" Manage / Upload", modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Enrolled Students Header with Attendance Counter
                item {
                    val presentCount = state.learnerRows.count { it.isPresent }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Student Cohort (${state.learnerRows.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Attendance: $presentCount / ${state.learnerRows.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (state.learnerRows.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "No students have joined yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                                Text(
                                    "Give your students the code $shortCode. When they enter it in their profile, they will appear here instantly.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(state.learnerRows) { row ->
                        LearnerAnalyticsCard(
                            row = row,
                            onToggleAttendance = { viewModel.toggleAttendance(row.learnerId) }
                        )
                    }
                }

                if (state.classAverages.isNotEmpty()) {
                    item {
                        Text("Class Concept Mastery", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(state.classAverages.entries.toList()) { (concept, average) ->
                        ConceptAverageRow(concept, average)
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("📋 Facilitator Class Report") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        reportContent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(reportContent))
                        showReportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ConceptAverageRow(concept: String, average: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(concept, style = MaterialTheme.typography.titleMedium)
                Text("${(average * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { average },
                color = masteryColor(average),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun LearnerAnalyticsCard(
    row: LearnerAnalyticsRow,
    onToggleAttendance: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(row.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("ID: ${row.learnerId.take(8)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (row.metrics?.needs_attention == true) {
                        Icon(Icons.Filled.Warning, contentDescription = "Needs attention", tint = ErrorRed, modifier = Modifier.padding(end = 8.dp))
                    }
                    FilterChip(
                        selected = row.isPresent,
                        onClick = onToggleAttendance,
                        label = { Text(if (row.isPresent) "✓ Present" else "✗ Absent") },
                        colors = if (row.isPresent) FilterChipDefaults.filterChipColors() else FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    )
                }
            }
            val metrics = row.metrics
            if (metrics == null || (metrics.completed_lessons == 0 && metrics.mastery_levels.isEmpty())) {
                Text("Enrolled in class · Ready to learn", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    "${metrics.completed_lessons} lessons completed · ${(metrics.average_score * 100).toInt()}% avg score",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                metrics.mastery_levels.forEach { (concept, score) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(concept, style = MaterialTheme.typography.bodySmall)
                        Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = masteryColor(score))
                    }
                }
            }
        }
    }
}

private fun masteryColor(score: Float): Color = when {
    score >= 0.85f -> MasteredConcept
    score >= 0.70f -> ProficientConcept
    else -> WeakConcept
}

private fun formatCachedAt(timestampMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestampMillis))
}

