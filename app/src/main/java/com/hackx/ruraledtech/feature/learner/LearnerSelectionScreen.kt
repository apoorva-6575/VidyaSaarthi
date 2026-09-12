package com.hackx.ruraledtech.feature.learner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.feature.common.LoadingState
import com.hackx.ruraledtech.feature.common.UiState

import androidx.compose.material.icons.filled.SettingsVoice
import com.hackx.ruraledtech.feature.common.SupportedLanguage

import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnerSelectionScreen(
    onLearnerSelected: () -> Unit,
    onAddLearner: () -> Unit,
    onSwitchRole: () -> Unit = {},
    viewModel: LearnerSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.learners.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSpeech() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learner Profiles") },
                navigationIcon = {
                    IconButton(onClick = onSwitchRole) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Switch Role")
                    }
                },
                actions = {
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
                }
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Who is learning?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Select your profile below or add a new learner.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            if (isSpeaking) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔊 Speaking instructions...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = { viewModel.stopSpeech() }) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val s = state) {
                    is UiState.Loading -> LoadingState()
                    is UiState.Empty -> LearnerGrid(emptyList(), { }, onAddLearner)
                    is UiState.Success -> LearnerGrid(
                        learners = s.data,
                        onLearnerClick = { viewModel.selectLearner(it.learnerId, onLearnerSelected) },
                        onAddLearner = onAddLearner,
                    )
                    is UiState.Error -> EmptyState("Something went wrong", s.message)
                }
            }

            OutlinedButton(
                onClick = { viewModel.openTtsSettings() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.SettingsVoice, contentDescription = null)
                Text(" ⚙️ Google TTS Voice Settings", modifier = Modifier.padding(start = 6.dp))
            }

            OutlinedButton(
                onClick = onSwitchRole,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Switch to Teacher / Role Selection")
            }
        }
    }
}

private val avatarList = listOf("🦁", "🚀", "🌟", "🦉", "🎨", "⚽", "🌸", "🐯")

@Composable
private fun LearnerGrid(learners: List<Learner>, onLearnerClick: (Learner) -> Unit, onAddLearner: () -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(learners) { learner ->
            val lang = SupportedLanguage.fromTag(learner.preferredLanguage)
            val avatarEmoji = avatarList[kotlin.math.abs(learner.learnerId.hashCode()) % avatarList.size]
            LearnerCard(
                name = learner.name,
                subtitle = "Grade ${learner.grade} · ${lang.nativeName}",
                avatarEmoji = avatarEmoji,
                onClick = { onLearnerClick(learner) }
            )
        }
        item {
            LearnerCard(name = "+ Add Learner", subtitle = "New Profile", icon = Icons.Filled.Add, onClick = onAddLearner)
        }
    }
}

@Composable
private fun LearnerCard(
    name: String,
    subtitle: String?,
    onClick: () -> Unit,
    avatarEmoji: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Person,
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarEmoji != null) {
                    Text(avatarEmoji, style = MaterialTheme.typography.headlineMedium)
                } else {
                    Icon(icon, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
