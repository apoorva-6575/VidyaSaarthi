package com.hackx.ruraledtech.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Inbox, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        action?.let {
            Column(modifier = Modifier.padding(top = 16.dp)) { it() }
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun OfflineNotice(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CloudOff, contentDescription = "Offline")
    }
}
