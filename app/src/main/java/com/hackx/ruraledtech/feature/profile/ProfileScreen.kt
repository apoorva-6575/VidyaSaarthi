package com.hackx.ruraledtech.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.SupportedLanguage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.IconButton
import com.hackx.ruraledtech.feature.common.UiStrings

@Composable
fun ProfileScreen(
    onSwitchedProfile: () -> Unit,
    onLogoutToRoleSelection: () -> Unit = {},
    onOpenAccessibility: () -> Unit,
    onOpenContentLibrary: () -> Unit,
    onOpenPassport: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val learner by viewModel.learner.collectAsState()
    val enrolledClasses by viewModel.enrolledClasses.collectAsState()
    val lang = learner?.preferredLanguage ?: "en"
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showJoinClassDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joinMessage by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(learner?.name ?: "", style = MaterialTheme.typography.headlineMedium)
            val currentLang = SupportedLanguage.fromTag(lang)
            Text(
                "Grade ${learner?.grade ?: ""} · ${currentLang.nativeName} (${currentLang.displayName})",
                style = MaterialTheme.typography.bodyMedium
            )

            if (enrolledClasses.isNotEmpty()) {
                Text(
                    "My Enrolled Classes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                enrolledClasses.forEach { c ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            if (c.grade != null || c.subject != null) {
                                Text(
                                    "${c.grade ?: ""} ${c.subject ?: ""}".trim(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        joinMessage = null
                        joinCode = ""
                        showJoinClassDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(UiStrings.joinClassCode(lang))
                }
                OutlinedButton(onClick = { showLanguageDialog = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(UiStrings.changeLanguage(lang))
                }
                OutlinedButton(onClick = onOpenContentLibrary, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Curriculum & Mesh Distribution")
                }
                OutlinedButton(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Accessibility settings")
                }
                OutlinedButton(onClick = onOpenPassport, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Learning Passport (move to another device)")
                }
                Button(
                    onClick = {
                        viewModel.switchProfile()
                        onSwitchedProfile()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(UiStrings.switchProfile(lang))
                }
                OutlinedButton(
                    onClick = {
                        viewModel.switchProfile()
                        onLogoutToRoleSelection()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(UiStrings.logoutTeacher(lang))
                }
            }
        }
    }

    if (showJoinClassDialog) {
        AlertDialog(
            onDismissRequest = { if (!isJoining) showJoinClassDialog = false },
            title = { Text("Join a class") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter the 6-character class code given by your teacher:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { if (it.length <= 8) joinCode = it.uppercase() },
                        label = { Text("Class Code (e.g. B7712C)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (joinMessage != null) {
                        Text(
                            joinMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (joinMessage!!.startsWith("Success") || joinMessage!!.startsWith("Joined")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isJoining = true
                        viewModel.joinClass(joinCode) { success, msg ->
                            isJoining = false
                            joinMessage = msg
                            if (success) {
                                joinCode = ""
                            }
                        }
                    },
                    enabled = joinCode.isNotBlank() && !isJoining
                ) {
                    Text(if (isJoining) "Joining..." else "Join")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showJoinClassDialog = false },
                    enabled = !isJoining
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showLanguageDialog) {
        val currentLanguage = SupportedLanguage.fromTag(lang)
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose your language") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SupportedLanguage.entries.forEach { language ->
                        val isSelected = language == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeLanguage(language)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.changeLanguage(language)
                                    showLanguageDialog = false
                                },
                            )
                            Text(
                                text = "${language.nativeName} (${language.displayName})",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                            IconButton(onClick = { viewModel.testSpeak(language) }) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "Test voice")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.openTtsSettings()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Icon(Icons.Filled.SettingsVoice, contentDescription = null)
                        Text(" ⚙️ Google TTS Voice Settings", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            },
        )
    }
}
