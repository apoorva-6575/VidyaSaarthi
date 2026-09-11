package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.ui.theme.ErrorRed
import com.hackx.ruraledtech.ui.theme.MasteredConcept
import com.hackx.ruraledtech.ui.theme.ProficientConcept
import com.hackx.ruraledtech.ui.theme.WeakConcept

@Composable
fun ClassAnalyticsScreen(viewModel: ClassAnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        when {
            state.isOffline -> EmptyState(
                "Analytics need an internet connection",
                "Class-wide analytics are computed on the server. Connect and try again.",
                Modifier.padding(padding),
            )
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            state.error != null -> EmptyState("Couldn't load analytics", state.error!!, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Text("Class averages", style = MaterialTheme.typography.titleLarge) }
                items(state.classAverages.entries.toList()) { (concept, average) ->
                    ConceptAverageRow(concept, average)
                }

                item { Text("Students", style = MaterialTheme.typography.titleLarge) }
                items(state.learnerRows) { row -> LearnerAnalyticsCard(row) }
            }
        }
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
private fun LearnerAnalyticsCard(row: LearnerAnalyticsRow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.name, style = MaterialTheme.typography.titleMedium)
                if (row.metrics?.needs_attention == true) {
                    Icon(Icons.Filled.Warning, contentDescription = "Needs attention", tint = ErrorRed)
                }
            }
            val metrics = row.metrics
            if (metrics == null) {
                Text("No data yet", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            } else {
                Text(
                    "${metrics.completed_lessons} lessons completed · ${(metrics.average_score * 100).toInt()}% avg score",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
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
