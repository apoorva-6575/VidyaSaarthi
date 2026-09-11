package com.hackx.ruraledtech.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Placeholder for Group 4's teacher platform (PS section 6.6/25). This exists so the
 * role split asked for at onboarding is real and demoable today, without Group 1
 * building out class management, analytics, or content distribution — that's Group 4's
 * scope, consuming the same LearnerRepository/ProgressRepository/SyncRepository interfaces
 * Group 1 already exposes.
 */
@Composable
fun TeacherHomeScreen(
    onSwitchToStudent: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel(),
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
            Text("Teacher Portal", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                "Class management, content distribution, and learner analytics are on the way.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
            )
            OutlinedButton(onClick = { viewModel.switchToStudentRole(onSwitchToStudent) }, modifier = Modifier.fillMaxWidth()) {
                Text("Switch to student view")
            }
        }
    }
}
