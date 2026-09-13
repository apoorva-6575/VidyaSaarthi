package com.hackx.ruraledtech.feature.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.domain.model.CalloutTone
import com.hackx.ruraledtech.domain.model.ContentBlock
import com.hackx.ruraledtech.domain.model.ContentLookupResult
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.feature.common.LoadingState
import com.hackx.ruraledtech.feature.common.UiState

import androidx.compose.runtime.DisposableEffect

import androidx.compose.material.icons.filled.Stop
import com.hackx.ruraledtech.feature.common.SupportedLanguage
import com.hackx.ruraledtech.feature.common.UiStrings

@Composable
fun LessonViewerScreen(
    onTakeQuiz: () -> Unit,
    viewModel: LessonViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val speakingLang by viewModel.speakingState.collectAsState()
    val learnerLang by viewModel.learnerLanguage.collectAsState()

    LaunchedEffect(Unit) { viewModel.markStarted() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeech()
            viewModel.audioManager.stop()
        }
    }

    Scaffold { padding ->
        when (val s = state) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Empty -> EmptyState(
                "This lesson isn't downloaded yet",
                "Try connecting to a nearby device running the app, or check back once your teacher shares this content.",
                Modifier.padding(padding),
            )
            is UiState.Error -> EmptyState("Something went wrong", s.message, Modifier.padding(padding))
            is UiState.Success -> when (val result = s.data) {
                is ContentLookupResult.Available -> LessonContent(
                    lesson = result.lesson,
                    lang = learnerLang,
                    speakingLang = speakingLang,
                    onPlayAudio = {
                        viewModel.stopSpeech()
                        viewModel.audioManager.play(it)
                    },
                    onSpeak = { viewModel.speakText(it) },
                    onStopSpeech = { viewModel.stopSpeech() },
                    onFinish = {
                        viewModel.markCompleted()
                        onTakeQuiz()
                    },
                    modifier = Modifier.padding(padding),
                )
                else -> EmptyState("Content unavailable", "This content is pending transfer.", Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun LessonContent(
    lesson: com.hackx.ruraledtech.domain.model.Lesson,
    lang: String,
    speakingLang: String?,
    onPlayAudio: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)

        if (speakingLang != null) {
            val spokenName = SupportedLanguage.fromTag(speakingLang).nativeName
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔊 Speaking in $spokenName...", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    IconButton(onClick = onStopSpeech) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(lesson.blocks) { block -> ContentBlockView(block, onPlayAudio, onSpeak) }
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(UiStrings.takeQuiz(lang), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ContentBlockView(block: ContentBlock, onPlayAudio: (String) -> Unit, onSpeak: (String) -> Unit) {
    when (block) {
        is ContentBlock.Text -> Row(verticalAlignment = Alignment.Top) {
            Text(block.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onSpeak(block.body) }) {
                Icon(Icons.Filled.VolumeUp, contentDescription = "Listen")
            }
        }
        is ContentBlock.Audio -> Card {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                IconButton(onClick = { onPlayAudio(block.assetPath) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play audio")
                }
                Text(block.transcript ?: "Listen to this section", style = MaterialTheme.typography.bodyMedium)
            }
        }
        is ContentBlock.Image -> Card { Text(block.altText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
        is ContentBlock.Example -> Card {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(block.prompt, style = MaterialTheme.typography.titleSmall)
                    Text(block.explanation, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                IconButton(onClick = { onSpeak("${block.prompt}. ${block.explanation}") }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Listen")
                }
            }
        }
        is ContentBlock.Callout -> Card {
            Text(
                block.message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (block.tone == CalloutTone.WARNING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        is ContentBlock.Video -> Card { Text("Video: ${block.assetPath}", modifier = Modifier.padding(12.dp)) }
        is ContentBlock.Document -> {
            val context = LocalContext.current
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                    Text(block.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Button(onClick = { openDocument(context, block.assetPath) }) {
                        Text("Open")
                    }
                }
            }
        }
        is ContentBlock.Unsupported -> Unit
    }
}

/** Opens a persisted document asset (see ContentInstallerImpl.withPersistedAsset) via whatever app the device already has for its type — no bundled renderer needed. */
private fun openDocument(context: android.content.Context, absolutePath: String) {
    try {
        val file = java.io.File(absolutePath)
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "This document isn't available on this device.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "*/*"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No app installed that can open this file type.", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not open document: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
