package com.hackx.ruraledtech.feature.contentlibrary

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.core.work.SyncScheduler
import com.hackx.ruraledtech.data.contentpackage.ContentPackageReader
import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.local.entities.LessonEntity
import com.hackx.ruraledtech.data.local.entities.QuestionEntity
import com.hackx.ruraledtech.data.mapper.ContentBlockDto
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.ContentPackageState
import com.hackx.ruraledtech.domain.model.ContentRequirement
import com.hackx.ruraledtech.domain.usecase.content.ObserveInstalledPackagesUseCase
import com.hackx.ruraledtech.domain.usecase.content.RemoveContentPackageUseCase
import com.hackx.ruraledtech.p2p.integration.ContentInstaller
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.mesh.TransferTask
import com.hackx.ruraledtech.p2p.storage.PackageStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

private val json = Json { ignoreUnknownKeys = true }

@HiltViewModel
class ContentLibraryViewModel @Inject constructor(
    observeInstalledPackagesUseCase: ObserveInstalledPackagesUseCase,
    private val removeContentPackageUseCase: RemoveContentPackageUseCase,
    private val learningMesh: LearningMesh,
    private val syncScheduler: SyncScheduler,
    private val api: RuralEdTechApi,
    private val contentInstaller: ContentInstaller,
    private val packageStorageManager: PackageStorageManager,
    private val reader: ContentPackageReader,
    private val lessonDao: LessonDao,
    private val questionDao: QuestionDao,
    private val contentPackageDao: ContentPackageDao,
    private val classroomMeshCoordinator: com.hackx.ruraledtech.domain.usecase.mesh.ClassroomMeshCoordinator,
    private val materialRequestDao: com.hackx.ruraledtech.data.local.dao.MaterialRequestDao,
    private val currentLearnerManager: CurrentLearnerManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * Scoped to the active learner profile — previously this showed every installed package on
     * the device to every profile, so a sibling using a different profile on the same device
     * could see material another sibling had privately received via P2P. Falls back to an
     * empty-string "no learner" key in teacher mode, which still shows shared/global content
     * (receivedByLearnerId IS NULL) — see ContentPackageDao.observeInstalledForLearner.
     */
    val packages: StateFlow<List<ContentPackage>> = currentLearnerManager.currentLearnerId
        .flatMapLatest { learnerId -> observeInstalledPackagesUseCase(learnerId ?: "") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    val meshState: StateFlow<MeshState> = learningMesh.observeMeshState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeshState.OFFLINE)

    val transfers: StateFlow<List<TransferTask>> = learningMesh.observeTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectedEndpoints: StateFlow<List<String>> = learningMesh.observeConnectedEndpoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePeerPackages: StateFlow<List<com.hackx.ruraledtech.p2p.manifest.PackageDescriptor>> =
        learningMesh.observeAvailablePeerPackages()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingOffers: StateFlow<List<com.hackx.ruraledtech.p2p.protocol.P2PMessage.ClassMaterialOffer>> = classroomMeshCoordinator.incomingOffers
        .scan(emptyList<com.hackx.ruraledtech.p2p.protocol.P2PMessage.ClassMaterialOffer>()) { acc, offer ->
            acc + offer
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val materialRequests: StateFlow<List<com.hackx.ruraledtech.data.local.entities.MaterialRequestEntity>> = materialRequestDao.observeIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun acceptOffer(offer: com.hackx.ruraledtech.p2p.protocol.P2PMessage.ClassMaterialOffer) {
        viewModelScope.launch {
            classroomMeshCoordinator.acceptOffer(offer)
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            classroomMeshCoordinator.approveRequest(requestId)
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            classroomMeshCoordinator.declineRequest(requestId)
        }
    }

    fun remove(packageId: String) {
        viewModelScope.launch { removeContentPackageUseCase(packageId) }
    }

    private val _shareErrors = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val shareErrors: kotlinx.coroutines.flow.SharedFlow<String> = _shareErrors.asSharedFlow()

    fun sharePackage(packageId: String) {
        viewModelScope.launch {
            val zipFile = packageStorageManager.getPackageZipFile(packageId)
            if (zipFile == null || !zipFile.exists()) {
                val errorMsg = "Material isn't available locally on this device."
                android.util.Log.e("ContentLibraryViewModel", errorMsg)
                _shareErrors.emit(errorMsg)
                return@launch
            }
            android.util.Log.d("ContentLibraryViewModel", "Verified ZIP exists for $packageId: ${zipFile.absolutePath} (size=${zipFile.length()} sha256=${packageStorageManager.computeSha256(zipFile)})")
            // Real byte transfer to already-connected peers (existing, working pipeline).
            learningMesh.broadcastPackage(packageId)
            // Classroom-aware notification so peers see "Shared by Teacher" — this alone sends
            // no bytes; it was previously never called at all, so that UI section was always
            // empty even though the transfer above already worked.
            val pkg = contentPackageDao.getLatest(packageId)
            classroomMeshCoordinator.broadcastOffer(
                packageId = packageId,
                version = pkg?.version ?: 1,
                subject = pkg?.subject ?: "General",
                sizeBytes = zipFile.length(),
            )
        }
    }

    fun requestPackage(packageId: String) {
        viewModelScope.launch {
            learningMesh.requestContent(com.hackx.ruraledtech.domain.model.ContentRequirement(packageId, null, 0.5f, "Manual Peer Request"))
        }
    }

    fun findNearbyDevice() {
        learningMesh.startMesh()
    }

    fun stopMesh() {
        learningMesh.stopMesh()
    }

    fun syncFromCloud() {
        syncScheduler.scheduleContentUpdateCheck()
    }

    /**
     * Remembered so [createAndUploadMaterial] can also copy the ORIGINAL file bytes into the
     * package (not just extracted text) — previously the picked PPT/PDF's Uri was discarded
     * right after text extraction, so a student could never actually open the source document,
     * only read a plain-text (or placeholder) rendering of it.
     */
    private var lastPickedFileUri: Uri? = null
    private var lastPickedFileName: String? = null

    fun processPickedFile(uri: Uri, onParsed: (title: String, content: String) -> Unit) {
        lastPickedFileUri = uri
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "Uploaded Document"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                lastPickedFileName = fileName
                val cleanTitle = fileName.substringBeforeLast('.')
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .trim()

                val textContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    try {
                        String(bytes, Charsets.UTF_8).take(8000)
                    } catch (_: Exception) {
                        "Attached File: $fileName (${bytes.size / 1024} KB)"
                    }
                } ?: "Document: $fileName"

                val finalContent = if (textContent.isBlank() || textContent.any { it.code == 0 }) {
                    "Attached Learning Resource: $fileName (${fileName.substringAfterLast('.').uppercase()} format for classroom distribution)"
                } else textContent

                withContext(Dispatchers.Main) {
                    onParsed(cleanTitle, finalContent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onParsed("Class Document", "Attached file from device storage.")
                }
            }
        }
    }

    fun createAndUploadMaterial(
        title: String,
        subject: String,
        grade: Int,
        language: String,
        conceptName: String,
        content: String,
        questionPrompt: String? = null,
        optionA: String? = null,
        optionB: String? = null,
        correctOption: String = "a",
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanSubject = subject.trim()
                val cleanTitle = title.trim()
                val cleanConcept = if (conceptName.isNotBlank()) conceptName.trim() else cleanTitle
                val slug = cleanTitle.lowercase().replace("[^a-z0-9]+".toRegex(), "-").trim('-')
                val packageId = if (cleanTitle.contains("-v") || cleanTitle.startsWith("math-") || cleanTitle.startsWith("fractions-")) {
                    cleanTitle.lowercase().replace("[^a-z0-9-]+".toRegex(), "").trim('-')
                } else {
                    "${cleanSubject.lowercase()}-grade${grade}-${slug}"
                }
                val version = if (packageId.contains("-v")) {
                    packageId.substringAfterLast("-v").toIntOrNull() ?: 1
                } else 1

                val buildDir = File(context.cacheDir, "pkg_build_${System.currentTimeMillis()}")
                val lessonsDir = File(buildDir, "lessons")
                val questionsDir = File(buildDir, "questions")
                lessonsDir.mkdirs()
                questionsDir.mkdirs()

                val lessonId = "lesson_${slug}_1"
                val conceptId = "concept_${slug}"

                // Copy the ORIGINAL picked file (PPT/PDF/etc.) into the package so the student
                // can actually open the source document, not just read extracted plain text.
                // Previously the Uri was discarded right after text extraction.
                var documentBlockJson = ""
                val pickedUri = lastPickedFileUri
                if (pickedUri != null) {
                    val originalName = lastPickedFileName ?: "document"
                    val ext = originalName.substringAfterLast('.', "").ifBlank { "bin" }
                    val assetsDir = File(buildDir, "assets")
                    assetsDir.mkdirs()
                    val destFile = File(assetsDir, "document.$ext")
                    try {
                        context.contentResolver.openInputStream(pickedUri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (destFile.exists() && destFile.length() > 0) {
                            documentBlockJson = """
                                ,
                                {
                                    "type": "DOCUMENT",
                                    "block_id": "b2",
                                    "asset_path": "assets/document.$ext",
                                    "alt_text": "${originalName.replace("\"", "\\\"")}"
                                }
                            """.trimIndent()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ContentLibraryViewModel", "Could not copy original document into package", e)
                    }
                }
                lastPickedFileUri = null
                lastPickedFileName = null

                val lessonJson = """
                {
                    "lesson_id": "$lessonId",
                    "package_id": "$packageId",
                    "subject": "$cleanSubject",
                    "grade": $grade,
                    "concept_id": "$conceptId",
                    "language": "$language",
                    "title": "$cleanTitle",
                    "order_index": 0,
                    "blocks": [
                        {
                            "type": "TEXT",
                            "block_id": "b1",
                            "body": "${content.replace("\"", "\\\"").replace("\n", "\\n")}"
                        }$documentBlockJson
                    ]
                }
                """.trimIndent()

                val qPrompt = if (!questionPrompt.isNullOrBlank()) questionPrompt.trim() else "What is the primary topic of $cleanTitle?"
                val optA = if (!optionA.isNullOrBlank()) optionA.trim() else cleanConcept
                val optB = if (!optionB.isNullOrBlank()) optionB.trim() else "Alternative concept"

                val questionJson = """
                {
                    "question_id": "q_${slug}_1",
                    "lesson_id": "$lessonId",
                    "concept_id": "$conceptId",
                    "question_type": "SINGLE_CHOICE",
                    "language": "$language",
                    "prompt": "${qPrompt.replace("\"", "\\\"")}",
                    "options": [
                        {"option_id": "a", "text": "${optA.replace("\"", "\\\"")}"},
                        {"option_id": "b", "text": "${optB.replace("\"", "\\\"")}"}
                    ],
                    "correct_option_id": "$correctOption",
                    "difficulty": 0.3,
                    "explanation": "This lesson covers $cleanConcept."
                }
                """.trimIndent()

                File(lessonsDir, "lesson_1.json").writeText(lessonJson)
                File(questionsDir, "question_1.json").writeText(questionJson)

                fun sha256(bytes: ByteArray): String {
                    val digest = MessageDigest.getInstance("SHA-256")
                    return digest.digest(bytes).joinToString("") { "%02x".format(it) }
                }

                val lessonBytes = File(lessonsDir, "lesson_1.json").readBytes()
                val questionBytes = File(questionsDir, "question_1.json").readBytes()

                val checksumsJson = """
                {
                    "lessons/lesson_1.json": "${sha256(lessonBytes)}",
                    "questions/question_1.json": "${sha256(questionBytes)}"
                }
                """.trimIndent()
                File(buildDir, "checksums.json").writeText(checksumsJson)

                val packageChecksum = reader.computeContentChecksum(buildDir)

                val manifestJson = """
                {
                    "package_id": "$packageId",
                    "version": $version,
                    "subject": "$cleanSubject",
                    "grade": $grade,
                    "languages": ["$language"],
                    "size": 4096,
                    "dependencies": [],
                    "checksum": "$packageChecksum",
                    "created_at": "2026-09-12T00:00:00Z",
                    "priority": "normal"
                }
                """.trimIndent()
                File(buildDir, "manifest.json").writeText(manifestJson)

                val zipFile = File(context.cacheDir, "upload_${packageId}.zip")
                if (zipFile.exists()) zipFile.delete()

                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    buildDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relPath = file.relativeTo(buildDir).path.replace('\\', '/')
                            zos.putNextEntry(ZipEntry(relPath))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                val zipChecksum = sha256(zipFile.readBytes())

                var uploadSuccess = false
                var uploadMsg = ""
                try {
                    val plainType = "text/plain".toMediaTypeOrNull()
                    val zipType = "application/zip".toMediaTypeOrNull()
                    val reqPkgId = packageId.toRequestBody(plainType)
                    val reqVersion = version.toString().toRequestBody(plainType)
                    val reqSubject = cleanSubject.toRequestBody(plainType)
                    val reqGrade = grade.toString().toRequestBody(plainType)
                    val reqLang = language.toRequestBody(plainType)
                    val reqChecksum = zipChecksum.toRequestBody(plainType)
                    val reqManifest = manifestJson.toRequestBody(plainType)
                    val reqFileBody = zipFile.asRequestBody(zipType)
                    val reqFilePart = MultipartBody.Part.createFormData("file", "content.zip", reqFileBody)

                    val resp = api.uploadContent(
                        packageId = reqPkgId,
                        version = reqVersion,
                        subject = reqSubject,
                        grade = reqGrade,
                        language = reqLang,
                        checksum = reqChecksum,
                        manifest = reqManifest,
                        file = reqFilePart,
                    )
                    if (resp.isSuccessful) {
                        uploadSuccess = true
                        uploadMsg = "Material uploaded to cloud & ready for offline P2P mesh!"
                    } else {
                        uploadMsg = "Material installed locally (ready for offline P2P mesh)"
                    }
                } catch (_: Exception) {
                    uploadMsg = "Material saved locally for offline P2P mesh"
                }

                packageStorageManager.savePackageZip(packageId, zipFile)

                val lessonBlockJson = json.encodeToString(
                    ListSerializer(ContentBlockDto.serializer()),
                    listOf(ContentBlockDto(blockId = "b1", type = "text", body = content))
                )
                lessonDao.insertAll(
                    listOf(
                        LessonEntity(
                            lessonId = lessonId,
                            packageId = packageId,
                            subject = cleanSubject,
                            grade = grade,
                            conceptId = conceptId,
                            language = language,
                            title = cleanTitle,
                            orderIndex = 0,
                            blocksJson = lessonBlockJson
                        )
                    )
                )

                val optList = listOf(
                    com.hackx.ruraledtech.data.contentpackage.dto.QuestionOptionFileDto("a", optA),
                    com.hackx.ruraledtech.data.contentpackage.dto.QuestionOptionFileDto("b", optB)
                )
                val optionsJson = json.encodeToString(
                    ListSerializer(com.hackx.ruraledtech.data.contentpackage.dto.QuestionOptionFileDto.serializer()),
                    optList
                )
                questionDao.insertAll(
                    listOf(
                        QuestionEntity(
                            questionId = "q_${slug}_1",
                            lessonId = lessonId,
                            conceptId = conceptId,
                            questionType = "single_choice",
                            language = language,
                            prompt = qPrompt,
                            optionsJson = optionsJson,
                            correctOptionId = correctOption,
                            difficulty = 0.3f,
                            explanation = "This lesson covers $cleanConcept."
                        )
                    )
                )

                contentPackageDao.upsert(
                    ContentPackage(
                        packageId = packageId,
                        version = version,
                        subject = cleanSubject,
                        grade = grade,
                        languages = listOf(language),
                        sizeBytes = zipFile.length(),
                        checksum = packageChecksum,
                        installedAt = System.currentTimeMillis(),
                        state = ContentPackageState.INSTALLED,
                        priority = "normal",
                    ).toEntity()
                )

                try {
                    contentInstaller.install(buildDir.absolutePath)
                } catch (_: Exception) {}

                buildDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    onResult(true, if (uploadSuccess) uploadMsg else "Material ready for offline P2P mesh distribution!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Could not publish material: ${e.message}")
                }
            }
        }
    }

    /** Demo hook for PS section 36's "Find nearby learning device" empty-state action. */
    fun requestMissingContent(packageId: String, conceptId: String) {
        viewModelScope.launch {
            learningMesh.requestContent(
                ContentRequirement(packageId = packageId, conceptId = conceptId, priority = 1f, reason = "learner_requested"),
            )
        }
    }
}
