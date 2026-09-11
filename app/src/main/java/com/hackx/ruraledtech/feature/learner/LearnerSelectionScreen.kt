package com.hackx.ruraledtech.feature.learner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.feature.common.LoadingState
import com.hackx.ruraledtech.feature.common.UiState

@Composable
fun LearnerSelectionScreen(
    onLearnerSelected: () -> Unit,
    onAddLearner: () -> Unit,
    viewModel: LearnerSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.learners.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Who is learning?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "This phone can be shared. Each learner keeps their own progress.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

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
    }
}

@Composable
private fun LearnerGrid(learners: List<Learner>, onLearnerClick: (Learner) -> Unit, onAddLearner: () -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(learners) { learner ->
            LearnerCard(name = learner.name, subtitle = "Grade ${learner.grade}", onClick = { onLearnerClick(learner) })
        }
        item {
            LearnerCard(name = "Add learner", subtitle = null, icon = Icons.Filled.Add, onClick = onAddLearner)
        }
    }
}

@Composable
private fun LearnerCard(
    name: String,
    subtitle: String?,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Person,
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Text(name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
