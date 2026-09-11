package com.hackx.ruraledtech.p2p.passport.transport

import android.util.Log
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.integration.LearnerDataExporter
import com.hackx.ruraledtech.p2p.integration.LearnerDataImporter
import com.hackx.ruraledtech.p2p.passport.crypto.PassportCrypto
import com.hackx.ruraledtech.p2p.passport.model.LearningPassport
import com.hackx.ruraledtech.p2p.protocol.P2PMessage
import com.hackx.ruraledtech.p2p.protocol.ProtocolSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class PassportManager(
    private val exporter: LearnerDataExporter,
    private val importer: LearnerDataImporter,
    private val connectionManager: P2PConnectionManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "PassportManager"

    /**
     * Triggered by UI when Learner A wants to move to a new device.
     */
    fun exportAndSendPassport(learnerId: String, pin: String, endpointId: String, deviceId: String) {
        coroutineScope.launch {
            Log.d(TAG, "Exporting learner data for $learnerId...")
            val rawJson = exporter.exportLearnerData(learnerId)
            
            Log.d(TAG, "Encrypting with provided PIN...")
            val (encryptedPayload, iv, salt) = PassportCrypto.encrypt(rawJson, pin)

            val passport = LearningPassport(
                passportId = UUID.randomUUID().toString(),
                learnerId = learnerId,
                version = 1,
                timestamp = System.currentTimeMillis(),
                encryptedPayload = encryptedPayload,
                iv = iv,
                salt = salt
            )

            Log.d(TAG, "Sending encrypted passport over the mesh...")
            val message = P2PMessage.PassportTransfer(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                passport = passport
            )
            val payload = ProtocolSerializer.serialize(message)
            connectionManager.sendBytes(endpointId, payload)
        }
    }

    /**
     * Triggered when a passport is received and the user enters a PIN.
     */
    fun receiveAndImportPassport(passport: LearningPassport, pin: String) {
        coroutineScope.launch {
            Log.d(TAG, "Attempting to decrypt passport ${passport.passportId}...")
            val decryptedJson = PassportCrypto.decrypt(passport.encryptedPayload, passport.iv, passport.salt, pin)

            if (decryptedJson != null) {
                Log.d(TAG, "Decryption SUCCESS! Handing off to Group 1 to merge.")
                val success = importer.importLearnerData(decryptedJson)
                if (success) {
                    Log.d(TAG, "Passport merged successfully. Learner identity restored!")
                } else {
                    Log.e(TAG, "Group 1 failed to merge passport data.")
                }
            } else {
                Log.e(TAG, "Decryption FAILED. Incorrect PIN.")
            }
        }
    }
}
