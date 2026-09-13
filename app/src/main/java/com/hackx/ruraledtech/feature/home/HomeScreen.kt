package com.hackx.ruraledtech.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.domain.model.SubjectProgress

import androidx.compose.material.icons.filled.CellTower

import com.hackx.ruraledtech.feature.common.UiStrings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.DisposableEffect

@Composable
fun HomeScreen(
    onOpenSubjects: () -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenContentLibrary: () -> Unit = {},
    onAddLearner: () -> Unit = {},
    onOpenLesson: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val classLessons by viewModel.classLessons.collectAsState()
    val generalLessons by viewModel.generalLessons.collectAsState()
    val enrolledClasses by viewModel.enrolledClasses.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val lang = state.learner?.preferredLanguage ?: "en"

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSpeech() }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Top Header: Greeting, Offline Status & Audio Guide Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(UiStrings.hello(state.learner?.name ?: "", lang), style = MaterialTheme.typography.headlineMedium)
                        ConnectivityBadge(state.connectivity, state.pendingSyncCount)
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) viewModel.stopSpeech() else viewModel.playAudioGuide()
                            }
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                contentDescription = "Audio Guide",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenProfile) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile")
                        }
                    }
                }
            }

            // Shared Device: Household Learners Quick Switcher
            if (state.allLearners.isNotEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    UiStrings.householdLearners(lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Grade ${state.learner?.grade ?: 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                state.allLearners.forEach { l ->
                                    val isCurrent = l.learnerId == state.learner?.learnerId
                                    FilterChip(
                                        selected = isCurrent,
                                        onClick = { viewModel.switchLearner(l.learnerId) },
                                        label = {
                                            Text("${if (isCurrent) "✓ " else ""}${l.name} (Gr ${l.grade})")
                                        },
                                        colors = FilterChipDefaults.filterChipColors()
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onAddLearner,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" Add Child", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Spoken Audio Guide Status Pill (if actively speaking)
            if (isSpeaking) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("🔊 Audio Guide Playing...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            IconButton(onClick = { viewModel.stopSpeech() }) {
                                Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            state.recommendation?.let { recommendation ->
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(UiStrings.recommended(lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "${recommendation.recommendationType.name.replace('_', ' ')} — ${recommendation.conceptName}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            if (enrolledClasses.isNotEmpty()) {
                item {
                    Text(
                        "🏫 My Enrolled Classes (${enrolledClasses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(enrolledClasses) { cls ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cls.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${cls.subject ?: "General"} · Grade ${cls.grade ?: "Any"} · Code: ${cls.joinCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.SuggestionChip(
                                onClick = onOpenProfile,
                                label = { Text("● Enrolled") }
                            )
                        }
                    }
                }
            }

            // Class-grouped lessons: each enrolled class shows its assigned materials
            classLessons.forEach { (cls, lessons) ->
                item {
                    Text(
                        "🏫 ${cls.name}${if (!cls.subject.isNullOrBlank()) " • ${cls.subject}" else ""}${if (!cls.grade.isNullOrBlank()) " (Grade ${cls.grade})" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (lessons.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "No materials shared yet. Your teacher will share lessons here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(lessons) { lesson ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLesson(lesson.lessonId) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${lesson.subject} · Grade ${lesson.grade}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    androidx.compose.material3.Button(
                                        onClick = { onOpenLesson(lesson.lessonId) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("📚 Read & Quiz", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // General lessons (not tied to any class)
            if (generalLessons.isNotEmpty()) {
                item {
                    Text(
                        "📚 General Lessons (${generalLessons.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(generalLessons) { lesson ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenLesson(lesson.lessonId) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${lesson.subject} · Grade ${lesson.grade}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.Button(
                                    onClick = { onOpenLesson(lesson.lessonId) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("📚 Read & Quiz", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }


            item {
                Text(UiStrings.yourProgress(lang), style = MaterialTheme.typography.titleMedium)
            }

            items(state.subjectProgress) { subject ->
                SubjectProgressRow(subject, lang = lang, onClick = { onOpenSubject(subject.subject) })
            }

            item {
                NavRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = UiStrings.browseSubjects(lang),
                    subtitle = "Interactive audio-visual lessons & practice",
                    onClick = onOpenSubjects
                )
            }

            item {
                NavRow(
                    icon = Icons.Filled.CellTower,
                    label = UiStrings.discoverPeers(lang),
                    subtitle = "Village Learning Mesh — share & receive lessons offline",
                    onClick = onOpenContentLibrary,
                    highlight = true
                )
            }

            item {
                NavRow(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = UiStrings.viewFullProgress(lang),
                    subtitle = "Badges, mastery levels & learning streaks",
                    onClick = onOpenProgress
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun SubjectProgressRow(subject: SubjectProgress, lang: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickableRow(onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(UiStrings.subjectName(subject.subject, lang), style = MaterialTheme.typography.titleMedium)
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
private fun NavRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickableRow(onClick),
        colors = if (highlight) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) else androidx.compose.material3.CardDefaults.cardColors()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    )
                    .padding(12.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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

