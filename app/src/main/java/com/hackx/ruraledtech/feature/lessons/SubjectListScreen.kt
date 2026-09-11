package com.hackx.ruraledtech.feature.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.feature.common.LoadingState
import com.hackx.ruraledtech.feature.common.UiState

@Composable
fun SubjectListScreen(
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold { padding ->
        when (val s = state) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Empty -> EmptyState(
                "No content installed yet",
                "Ask your teacher to share content, or connect nearby devices once P2P is available.",
                Modifier.padding(padding),
            )
            is UiState.Error -> EmptyState("Something went wrong", s.message, Modifier.padding(padding))
            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(s.data) { subject ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onSubjectClick(subject) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(subjectIcon(subject), contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                                Text(
                                    subject,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A student who can't read well can't tell subjects apart from text alone (PS section 25's
 * low-literacy/icon-first requirement) — a recognizable icon per subject, large and with a
 * colored background (same pattern as RoleSelectionScreen's icon tiles), gives a non-text
 * way to find the right one.
 */
private fun subjectIcon(subject: String): ImageVector = when (subject.trim().lowercase()) {
    "mathematics", "math" -> Icons.Filled.Calculate
    "science" -> Icons.Filled.Science
    else -> Icons.AutoMirrored.Filled.MenuBook
}
