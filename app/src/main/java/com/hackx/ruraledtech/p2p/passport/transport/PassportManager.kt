package com.hackx.ruraledtech.p2p.passport.transport

import android.util.Log
import com.hackx.ruraledtech.domain.integration.LearnerDataExporter
import com.hackx.ruraledtech.domain.integration.LearnerDataImporter
import com.hackx.ruraledtech.domain.model.ImportResult
import com.hackx.ruraledtech.domain.model.LearnerExportData
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.passport.crypto.PassportCrypto
import com.hackx.ruraledtech.p2p.passport.model.LearningPassport
import com.hackx.ruraledtech.p2p.protocol.P2PMessage
import com.hackx.ruraledtech.p2p.protocol.ProtocolSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

sealed class PassportImportResult {
    data class Success(val learnerId: String, val importResult: ImportResult) : PassportImportResult()
    object InvalidPin : PassportImportResult()
    object MalformedPassport : PassportImportResult()
    data class UnsupportedVersion(val passportVersion: Int, val supportedVersion: Int) : PassportImportResult()
    data class Error(val message: String) : PassportImportResult()
}

class PassportManager(
    private val exporter: LearnerDataExporter,
    private val importer: LearnerDataImporter,
    private val connectionManager: P2PConnectionManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "PassportManager"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        /**
         * The `version` field on [LearningPassport] was previously write-only — set to 1 on
         * export and never read back anywhere, so two devices on incompatible passport
         * formats would silently attempt decryption/import instead of failing clearly. Bump
         * this whenever the encrypted payload's inner schema (LearnerExportData) changes in a
         * way older devices can't parse.
         */
        const val CURRENT_PASSPORT_VERSION = 1
    }

    private val _pendingPassport = MutableStateFlow<LearningPassport?>(null)
    val pendingPassport: StateFlow<LearningPassport?> = _pendingPassport.asStateFlow()

    /**
     * Called by MeshController when P2PMessage.PassportTransfer is received over P2P transport.
     * Only stores the encrypted passport in pending state for UI PIN entry. Never attempts automatic PIN decryption.
     */
    fun onPassportReceived(passport: LearningPassport) {
        Log.d(TAG, "Received encrypted passport ${passport.passportId} for learner ${passport.learnerId}")
        _pendingPassport.value = passport
    }

    fun clearPendingPassport() {
        _pendingPassport.value = null
    }

    /**
     * Triggered by UI when exporting learner data to send to a peer device over P2P mesh.
     */
    fun exportAndSendPassport(learnerId: String, pin: String, endpointId: String, deviceId: String) {
        coroutineScope.launch {
            Log.d(TAG, "Exporting learner data for $learnerId...")
            val exportData = exporter.exportLearnerData(learnerId)
            val rawJson = json.encodeToString(LearnerExportData.serializer(), exportData)

            Log.d(TAG, "Encrypting with user-provided PIN...")
            val (encryptedPayload, iv, salt) = PassportCrypto.encrypt(rawJson, pin)

            val passport = LearningPassport(
                passportId = UUID.randomUUID().toString(),
                learnerId = learnerId,
                version = CURRENT_PASSPORT_VERSION,
                timestamp = System.currentTimeMillis(),
                encryptedPayload = encryptedPayload,
                iv = iv,
                salt = salt
            )

            Log.d(TAG, "Sending encrypted passport over mesh to $endpointId...")
            val msg = P2PMessage.PassportTransfer(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                passport = passport
            )
            connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(msg))
        }
    }

    /**
     * Decrypts and imports passport using PIN entered by user via UI.
     */
    suspend fun importPassport(passport: LearningPassport, pin: String): PassportImportResult {
        if (passport.version > CURRENT_PASSPORT_VERSION) {
            Log.e(TAG, "Passport ${passport.passportId} is version ${passport.version}, this device only supports up to $CURRENT_PASSPORT_VERSION.")
            return PassportImportResult.UnsupportedVersion(passport.version, CURRENT_PASSPORT_VERSION)
        }

        Log.d(TAG, "Attempting decryption of passport ${passport.passportId}...")
        val decryptedJson = PassportCrypto.decrypt(passport.encryptedPayload, passport.iv, passport.salt, pin)
            ?: run {
                Log.e(TAG, "Decryption failed. Invalid PIN entered.")
                return PassportImportResult.InvalidPin
            }

        val exportData = try {
            json.decodeFromString(LearnerExportData.serializer(), decryptedJson)
        } catch (e: Exception) {
            Log.e(TAG, "Malformed passport payload in ${passport.passportId}", e)
            return PassportImportResult.MalformedPassport
        }

        return try {
            val importResult = importer.importLearnerData(exportData)
            if (_pendingPassport.value?.passportId == passport.passportId) {
                _pendingPassport.value = null
            }
            Log.d(TAG, "Passport ${passport.passportId} imported successfully for learner ${exportData.learnerId}")
            PassportImportResult.Success(exportData.learnerId, importResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error merging learner data into DB", e)
            PassportImportResult.Error(e.message ?: "Failed to import learner data")
        }
    }
}
