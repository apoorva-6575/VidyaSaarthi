package com.hackx.ruraledtech.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import com.hackx.ruraledtech.domain.model.Mastery
import com.hackx.ruraledtech.domain.model.MasteryLevel
import com.hackx.ruraledtech.ui.theme.MasteredConcept
import com.hackx.ruraledtech.ui.theme.ProficientConcept
import com.hackx.ruraledtech.ui.theme.WeakConcept

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Subjects", style = MaterialTheme.typography.titleLarge) }
            items(state.subjectProgress) { subject ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(subject.subject, style = MaterialTheme.typography.titleMedium)
                            Text("${(subject.completionPercentage * 100).toInt()}%")
                        }
                        LinearProgressIndicator(progress = { subject.completionPercentage }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }

            item { Text("Concepts", style = MaterialTheme.typography.titleLarge) }
            items(state.mastery) { mastery -> ConceptMasteryRow(mastery) }
        }
    }
}

@Composable
private fun ConceptMasteryRow(mastery: Mastery) {
    val color = when (mastery.level) {
        MasteryLevel.BEGINNER, MasteryLevel.DEVELOPING -> WeakConcept
        MasteryLevel.PROFICIENT -> ProficientConcept
        MasteryLevel.MASTERED -> MasteredConcept
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(mastery.conceptName, style = MaterialTheme.typography.titleMedium)
                Text(mastery.level.label, color = color, style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { mastery.score },
                color = color,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
