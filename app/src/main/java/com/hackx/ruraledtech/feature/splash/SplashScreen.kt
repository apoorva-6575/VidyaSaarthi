package com.hackx.ruraledtech.feature.splash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(
    onDestinationReady: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.startDestination.collectAsState()

    LaunchedEffect(destination) {
        destination?.let(onDestinationReady)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text("Rural EdTech", style = MaterialTheme.typography.headlineLarge)
            Text("Learning that doesn't wait for the internet", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
