package com.hackx.ruraledtech.feature.contentlibrary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.core.permissions.P2PPermissions
import com.hackx.ruraledtech.feature.common.EmptyState
import com.hackx.ruraledtech.p2p.manifest.PackageDescriptor
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.mesh.TransferState
import com.hackx.ruraledtech.p2p.mesh.TransferTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentLibraryScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ContentLibraryViewModel = hiltViewModel(),
) {
    val packages by viewModel.packages.collectAsState()
    val meshState by viewModel.meshState.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()
    val availablePeerPackages by viewModel.availablePeerPackages.collectAsState()
    val teacherClasses by viewModel.teacherClasses.collectAsState()
    var showCreateMaterialDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        val missing = P2PPermissions.getMissingPermissions(context)
        if (missing.isEmpty()) {
            android.util.Log.d("ContentLibraryScreen", "[P2P][PERMISSION] All required P2P permissions granted.")
            if (!P2PPermissions.isBluetoothEnabled(context)) {
                android.widget.Toast.makeText(context, "Please turn on Bluetooth.", android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            if (!P2PPermissions.isWifiEnabled(context)) {
                android.widget.Toast.makeText(context, "Please turn on Wi-Fi.", android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            if (!P2PPermissions.isLocationEnabled(context)) {
                android.widget.Toast.makeText(context, "Please turn on Location for nearby device discovery.", android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            viewModel.findNearbyDevice()
        } else {
            android.util.Log.e("ContentLibraryScreen", "[P2P][PERMISSION_DENIED] Cannot start mesh. Missing permissions: $missing")
            android.widget.Toast.makeText(context, "Bluetooth & Nearby permissions are required for P2P mesh", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val onStartMesh = remember(context) {
        {
            if (!P2PPermissions.hasAllPermissions(context)) {
                permissionLauncher.launch(P2PPermissions.required)
            } else if (!P2PPermissions.isBluetoothEnabled(context)) {
                android.widget.Toast.makeText(
                    context,
                    "Please turn on Bluetooth.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else if (!P2PPermissions.isWifiEnabled(context)) {
                android.widget.Toast.makeText(
                    context,
                    "Please turn on Wi-Fi.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else if (!P2PPermissions.isLocationEnabled(context)) {
                android.widget.Toast.makeText(
                    context,
                    "Please turn on Location for nearby device discovery.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                viewModel.findNearbyDevice()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Curriculum & Distribution") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncFromCloud() }) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = "Sync from Server")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateMaterialDialog = true },
                icon = { Icon(Icons.Filled.CloudUpload, contentDescription = "Upload Material") },
                text = { Text("Upload Material") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LearningMeshCard(
                    meshState = meshState,
                    transfers = transfers,
                    connectedEndpoints = connectedEndpoints,
                    availablePeerPackages = availablePeerPackages,
                    onStartMesh = onStartMesh,
                    onStopMesh = { viewModel.stopMesh() },
                    onSyncFromCloud = { viewModel.syncFromCloud() },
                    onRequestPeerPackage = { viewModel.requestPackage(it) },
                )
            }

            item {
                Text(
                    "Installed Curriculum Packages (${packages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (packages.isEmpty()) {
                item {
                    EmptyState(
                        "No content installed",
                        "Tap 'Upload Material' or 'Start Mesh' to share & download content offline.",
                    )
                }
            } else {
                items(packages) { pkg ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${pkg.subject} — Grade ${pkg.grade}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Package: ${pkg.packageId} · v${pkg.version}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Ready for Offline P2P Mesh Seeding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                TextButton(onClick = { viewModel.remove(pkg.packageId) }) { Text("Remove") }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { viewModel.sharePackage(pkg.packageId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(" 📤 Share via P2P Mesh", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateMaterialDialog) {
        CreateMaterialDialog(
            viewModel = viewModel,
            teacherClasses = teacherClasses,
            onDismiss = { showCreateMaterialDialog = false },
            onUpload = { title, subject, grade, lang, concept, content, classId, onDone ->
                viewModel.createAndUploadMaterial(
                    title = title,
                    subject = subject,
                    grade = grade,
                    language = lang,
                    conceptName = concept,
                    content = content,
                    classId = classId,
                ) { success, msg ->
                    onDone(success, msg)
                    if (success) {
                        showCreateMaterialDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun LearningMeshCard(
    meshState: MeshState,
    transfers: List<TransferTask>,
    connectedEndpoints: List<String>,
    availablePeerPackages: List<PackageDescriptor>,
    onStartMesh: () -> Unit,
    onStopMesh: () -> Unit,
    onSyncFromCloud: () -> Unit,
    onRequestPeerPackage: (String) -> Unit,
) {
    val isMeshActive = meshState != MeshState.OFFLINE && meshState != MeshState.ERROR

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CellTower,
                        contentDescription = null,
                        tint = if (isMeshActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Village Learning Mesh (Offline P2P)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                meshStateLabel(meshState),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMeshActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (connectedEndpoints.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        " ${connectedEndpoints.size} device(s) connected nearby",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (availablePeerPackages.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                        .padding(12.dp)
                ) {
                    Text(
                        "📡 Available from Nearby Peers (${availablePeerPackages.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    availablePeerPackages.forEach { peerPkg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peerPkg.packageId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Version ${peerPkg.version} · ${peerPkg.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { onRequestPeerPackage(peerPkg.packageId) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" 📥 Request P2P", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            transfers.forEach { transfer ->
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(transfer.packageId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        val statusText = when (transfer.state) {
                            TransferState.QUEUED -> "Queued"
                            TransferState.CONNECTING -> "Connecting..."
                            TransferState.TRANSFERRING -> "Transferring ${transfer.progressPercent}%"
                            TransferState.VERIFYING -> "Verifying SHA-256..."
                            TransferState.COMPLETED -> "✅ Installed & Ready"
                            TransferState.FAILED -> "❌ Failed"
                        }
                        Text(statusText, style = MaterialTheme.typography.labelLarge, color = if (transfer.state == TransferState.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    LinearProgressIndicator(
                        progress = { transfer.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isMeshActive) {
                    Button(
                        onClick = onStopMesh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Stop Mesh", modifier = Modifier.padding(start = 4.dp))
                    }
                } else {
                    Button(
                        onClick = onStartMesh,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CellTower, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Start Mesh", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                OutlinedButton(
                    onClick = onSyncFromCloud,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sync Cloud")
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreateMaterialDialog(
    viewModel: ContentLibraryViewModel,
    teacherClasses: List<com.hackx.ruraledtech.data.local.entities.ClassGroupEntity> = emptyList(),
    onDismiss: () -> Unit,
    onUpload: (
        title: String,
        subject: String,
        grade: Int,
        language: String,
        concept: String,
        content: String,
        classId: String?,
        onDone: (Boolean, String) -> Unit
    ) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Science") }
    var gradeText by remember { mutableStateOf("5") }
    var language by remember { mutableStateOf("en") }
    var conceptName by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf<String?>(teacherClasses.firstOrNull()?.classId) }

    var isUploading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.processPickedFile(uri) { detectedTitle, detectedContent ->
                title = detectedTitle
                contentText = detectedContent
                if (conceptName.isBlank()) conceptName = detectedTitle
            }
        }
    }

    fun fillSample() {
        title = "Water Cycle & Evaporation"
        subject = "Science"
        gradeText = "5"
        language = "en"
        conceptName = "Evaporation & Condensation"
        contentText = "The water cycle describes how water evaporates from the Earth's surface into the atmosphere, cools and condenses into clouds, and falls back to the ground as rain or snow."
    }

    fun fillDemo4Fractions() {
        title = "math-fractions-v3"
        subject = "Mathematics"
        gradeText = "5"
        language = "en"
        conceptName = "Equivalent Fractions"
        contentText = "A fraction represents part of a whole. Two fractions are equivalent when they represent the same proportion of the whole, such as 1/2 and 2/4."
    }

    fun fillDemo6Remedial() {
        title = "fractions-remedial-v2"
        subject = "Mathematics"
        gradeText = "5"
        language = "en"
        conceptName = "Visual Fractions Remedial"
        contentText = "When an object is divided into 2 equal parts, each part is 1/2. When divided into 4 equal parts, each is 1/4. Two quarters (2/4) equal one half (1/2)."
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upload Learning Material", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" 📁 File", modifier = Modifier.padding(start = 2.dp))
                    }
                    OutlinedButton(
                        onClick = { fillDemo4Fractions() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔢 Demo 4")
                    }
                    OutlinedButton(
                        onClick = { fillDemo6Remedial() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📐 Demo 6")
                    }
                }

                if (teacherClasses.isNotEmpty()) {
                    Text("Target Class:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.material3.FilterChip(
                            selected = selectedClassId == null,
                            onClick = { selectedClassId = null },
                            label = { Text("General (All)") }
                        )
                        teacherClasses.forEach { cls ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedClassId == cls.classId,
                                onClick = {
                                    selectedClassId = cls.classId
                                    if (!cls.grade.isNullOrBlank()) gradeText = cls.grade
                                    if (!cls.subject.isNullOrBlank()) subject = cls.subject
                                },
                                label = { Text("${cls.name} (${cls.classId.take(6).uppercase()})") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Lesson Title *") },
                    placeholder = { Text("e.g. Water Cycle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject *") },
                    placeholder = { Text("e.g. Science, Mathematics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gradeText,
                        onValueChange = { gradeText = it },
                        label = { Text("Grade") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Language") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = conceptName,
                    onValueChange = { conceptName = it },
                    label = { Text("Concept Name") },
                    placeholder = { Text("e.g. Evaporation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Lesson Content / Explanation *") },
                    placeholder = { Text("Enter lesson text for learners...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                if (title.isBlank() || contentText.isBlank()) {
                    Text(
                        "* Title and Lesson Content are required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (statusMessage != null) {
                    Text(
                        statusMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grade = gradeText.toIntOrNull() ?: 5
                    isUploading = true
                    statusMessage = "Packaging material & creating offline bundle..."
                    onUpload(
                        title,
                        subject,
                        grade,
                        language,
                        conceptName,
                        contentText,
                        selectedClassId,
                    ) { success, msg ->
                        isUploading = false
                        isSuccess = success
                        statusMessage = msg
                    }
                },
                enabled = title.isNotBlank() && contentText.isNotBlank() && !isUploading
            ) {
                Text(if (isUploading) "Packaging..." else "Publish Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Cancel")
            }
        }
    )
}

private fun meshStateLabel(state: MeshState): String = when (state) {
    MeshState.OFFLINE -> "Mesh Offline — Tap 'Start Mesh' to share or discover peers"
    MeshState.DISCOVERING -> "📡 Searching & Advertising via Wi-Fi Direct P2P..."
    MeshState.PEERS_AVAILABLE -> "✨ Nearby Peer Device Discovered!"
    MeshState.CONNECTED -> "🔗 Connected to Nearby Device (P2P Active)"
    MeshState.TRANSFERRING -> "📦 Transferring Content Packages..."
    MeshState.IDLE -> "✅ Ready to Seed & Share Content"
    MeshState.ERROR -> "⚠️ Mesh Error — Try toggling Wi-Fi / Bluetooth"
}

