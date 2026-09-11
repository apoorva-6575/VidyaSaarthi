# Rural EdTech: Group 3 Integration Guide

This document defines the contracts and interfaces that Groups 1, 2, and 4 should use to interact with the Group 3 Learning Mesh.

## 1. Requesting Content (Demand-Driven)
If Group 2 identifies that a specific package is needed (e.g., the student clicked a lesson they don't have), Group 1 should call `requestContent` on the `LearningMesh` interface.

```kotlin
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.integration.ContentRequirement
import javax.inject.Inject

class YourViewModel @Inject constructor(
    private val learningMesh: LearningMesh
) {
    fun downloadLesson(packageId: String) {
        val requirement = ContentRequirement(
            packageId = packageId,
            version = 1,
            conceptId = null,
            priority = 1.0f,
            reason = "User requested"
        )
        val requestSent = learningMesh.requestContent(requirement)
        if (!requestSent) {
            // Mesh is offline or no peers available
        }
    }
}
```

## 2. Observing Mesh State and Transfers
Group 1's UI should observe the StateFlows exposed by `LearningMesh` to update the UI (e.g., showing a progress bar or connection status).

```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun MeshStatusScreen(learningMesh: LearningMesh) {
    val meshState by learningMesh.observeMeshState().collectAsState()
    val activeTransfers by learningMesh.observeTransfers().collectAsState()

    Text("Current Mesh State: ${meshState.name}")

    if (activeTransfers.isNotEmpty()) {
        activeTransfers.forEach { transfer ->
            Text("Downloading ${transfer.packageId}: ${transfer.progressPercent}%")
        }
    }
}
```

## 3. Exporting and Sending the Learning Passport
To move a student's progress to another device, Group 1 must gather a PIN from the user and call `PassportManager`.

```kotlin
import com.hackx.ruraledtech.p2p.passport.transport.PassportManager
import javax.inject.Inject

class PassportViewModel @Inject constructor(
    private val passportManager: PassportManager
) {
    fun transferPassport(learnerId: String, pin: String, targetEndpointId: String) {
        // This will securely encrypt the data and send it to the target device
        passportManager.exportAndSendPassport(learnerId, pin, targetEndpointId)
    }
}
```
