package com.hackx.ruraledtech.feature.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.EmptyState

@Composable
fun ContentLibraryScreen(viewModel: ContentLibraryViewModel = hiltViewModel()) {
    val packages by viewModel.packages.collectAsState()

    Scaffold { padding ->
        if (packages.isEmpty()) {
            EmptyState(
                "No content installed",
                "Content shared by your teacher or nearby devices will appear here.",
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(packages) { pkg ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("${pkg.subject} — Grade ${pkg.grade}", style = MaterialTheme.typography.titleMedium)
                                Text("v${pkg.version} · ${pkg.sizeBytes / 1_000_000} MB", style = MaterialTheme.typography.bodyMedium)
                            }
                            TextButton(onClick = { viewModel.remove(pkg.packageId) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}
