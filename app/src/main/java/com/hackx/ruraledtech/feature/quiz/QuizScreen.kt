package com.hackx.ruraledtech.feature.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.feature.common.LoadingState

@Composable
fun QuizScreen(
    onQuizComplete: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold { padding ->
        when (val s = state) {
            is QuizScreenState.Loading, is QuizScreenState.Submitting -> LoadingState(Modifier.padding(padding))
            is QuizScreenState.Error -> EmptyState("Quiz unavailable", s.message, Modifier.padding(padding))
            is QuizScreenState.Ready -> QuizQuestionView(
                state = s,
                onSelectOption = viewModel::selectOption,
                onSubmit = viewModel::submitCurrentAnswer,
                onReadAloud = { viewModel.readQuestionAloud(s.questions[s.currentIndex]) },
                modifier = Modifier.padding(padding),
            )
            is QuizScreenState.Completed -> QuizResultView(s, onQuizComplete, Modifier.padding(padding))
        }
    }
}

@Composable
private fun QuizQuestionView(
    state: QuizScreenState.Ready,
    onSelectOption: (String) -> Unit,
    onSubmit: () -> Unit,
    onReadAloud: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = state.questions[state.currentIndex]
    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1f) / state.questions.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Question ${state.currentIndex + 1} of ${state.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Row(verticalAlignment = Alignment.Top) {
            Text(question.prompt, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onReadAloud) {
                Icon(Icons.Filled.VolumeUp, contentDescription = "Read question aloud")
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEach { option ->
                val selected = state.selectedOptionId == option.optionId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, onClick = { onSelectOption(option.optionId) }),
                ) {
                    OptionRow(
                        modifier = Modifier.padding(16.dp),
                        selected = selected,
                        text = option.text,
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = state.selectedOptionId != null,
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp),
        ) {
            Text(if (state.currentIndex == state.questions.size - 1) "Finish" else "Next", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun OptionRow(modifier: Modifier, selected: Boolean, text: String) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun QuizResultView(state: QuizScreenState.Completed, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val latestMastery = state.outcomes.lastOrNull()?.learningResult?.updatedMastery
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Score: ${state.correctCount} / ${state.total}", style = MaterialTheme.typography.headlineMedium)

        latestMastery?.let { mastery ->
            Text(
                "${mastery.conceptName}: ${(mastery.score * 100).toInt()}% — ${masteryLabel(mastery.level)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        state.outcomes.lastOrNull()?.learningResult?.recommendation?.let { rec ->
            Text(
                "Next up: ${rec.recommendationType.name.replace('_', ' ')} on ${rec.conceptName}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(onClick = onDone, modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(56.dp)) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun masteryLabel(level: MasteryLevel): String = level.label
