package com.hackx.ruraledtech.p2p.connection

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class NearbyConnectionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : P2PConnectionManager {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "com.hackx.ruraledtech.mesh"
    
    private val TAG = "NearbyTransport"

    private val _localEndpointName: String by lazy {
        val prefs = context.getSharedPreferences(
            "p2p_mesh_prefs",
            Context.MODE_PRIVATE
        )

        val storedId = prefs.getString("node_id", null)

        val nodeId = storedId ?: UUID.randomUUID().toString().replace("-", "")
            .take(6)
            .uppercase()
            .also {
                prefs.edit()
                    .putString("node_id", it)
                    .apply()
            }

        "RuralEdTech-Node-$nodeId"
    }

    override fun getLocalEndpointName(): String {
        return _localEndpointName
    }

    private val incomingFilePayloads =
        java.util.concurrent.ConcurrentHashMap<Long, Payload>()

    private val completedBeforeReceived =
        java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    private var listener: ConnectionListener? = null

    override fun setListener(listener: ConnectionListener) {
        this.listener = listener
    }

    // 1. DISCOVERY CALLBACKS
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Peer discovered: $endpointId (${info.endpointName})")
            listener?.onPeerDiscovered(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Peer lost: $endpointId")
            listener?.onPeerLost(endpointId)
        }
    }

    // 2. CONNECTION LIFECYCLE CALLBACKS
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "[P2P][INITIATED] Connection initiated with endpoint=$endpointId name=${info.endpointName} token=${info.authenticationToken}")
            listener?.onConnectionInitiated(endpointId, info.endpointName, info.authenticationToken)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val statusCode = result.status.statusCode
            val statusStr = ConnectionsStatusCodes.getStatusCodeString(statusCode)
            Log.d(TAG, "[P2P] Connection result for endpoint=$endpointId statusCode=$statusCode status=$statusStr")
            when (statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "[P2P][CONNECTED] endpoint=$endpointId status=STATUS_OK")
                    listener?.onConnectionAccepted(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "[P2P][CONNECTION_REJECTED] endpoint=$endpointId status=STATUS_CONNECTION_REJECTED")
                    listener?.onConnectionRejected(endpointId)
                }
                else -> {
                    Log.e(TAG, "[P2P][CONNECTION_FAILED] endpoint=$endpointId statusCode=$statusCode status=$statusStr")
                    listener?.onDisconnected(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "[P2P][DISCONNECTED] Disconnected from endpoint=$endpointId")
            listener?.onDisconnected(endpointId)
        }
    }

    // 3. PAYLOAD CALLBACKS (Data Transfer)
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(
            endpointId: String,
            payload: Payload
        ) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let {
                        Log.d(
                            TAG,
                            "[P2P][BYTES_RECEIVED] from=$endpointId size=${it.size}"
                        )
                        listener?.onBytesReceived(endpointId, it)
                    }
                }
                Payload.Type.FILE -> {
                    Log.d(
                        TAG,
                        "[P2P][FILE_RECEIVED] " +
                            "payloadId=${payload.id} from=$endpointId"
                    )

                    incomingFilePayloads[payload.id] = payload

                    listener?.onFilePayloadReceived(
                        endpointId,
                        payload.id
                    )

                    if (completedBeforeReceived.remove(payload.id)) {
                        processCompletedFilePayload(
                            endpointId,
                            payload.id,
                            payload
                        )
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) {
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress =
                        if (update.totalBytes > 0) {
                            (
                                update.bytesTransferred.toFloat() /
                                    update.totalBytes
                                    * 100
                            ).toInt()
                        } else {
                            0
                        }

                    listener?.onFileTransferProgress(
                        endpointId,
                        update.payloadId,
                        progress
                    )
                }

                PayloadTransferUpdate.Status.SUCCESS -> {
                    val payload =
                        incomingFilePayloads.remove(update.payloadId)

                    if (payload == null) {
                        Log.d(
                            TAG,
                            "[P2P][FILE_SEND_SUCCESS] " +
                                "Outgoing payload ${update.payloadId} " +
                                "successfully delivered to $endpointId"
                        )

                        listener?.onOutgoingFileTransferComplete(
                            endpointId,
                            update.payloadId
                        )

                        return
                    }

                    processCompletedFilePayload(
                        endpointId,
                        update.payloadId,
                        payload
                    )
                }

                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED -> {
                    Log.e(
                        TAG,
                        "[P2P][FILE_TRANSFER_FAILED] " +
                            "payloadId=${update.payloadId} " +
                            "endpoint=$endpointId " +
                            "status=${update.status}"
                    )

                    incomingFilePayloads.remove(update.payloadId)
                    completedBeforeReceived.remove(update.payloadId)

                    listener?.onFileTransferFailed(
                        endpointId,
                        update.payloadId
                    )
                }
            }
        }
    }

    private fun processCompletedFilePayload(
        endpointId: String,
        payloadId: Long,
        payload: Payload
    ) {
        Log.d(
            TAG,
            "[P2P][FILE_RECEIVE_SUCCESS] " +
                "payloadId=$payloadId from=$endpointId"
        )

        val destinationFile = try {
            val payloadUri = payload.asFile()?.asUri()

            val incomingDir =
                java.io.File(
                    context.cacheDir,
                    "incoming"
                ).apply {
                    if (!exists()) {
                        mkdirs()
                    }
                }

            val tempFile =
                java.io.File(
                    incomingDir,
                    "payload_${payloadId}.zip"
                )

            if (payloadUri != null) {
                context.contentResolver
                    .openInputStream(payloadUri)
                    ?.use { input ->
                        java.io.FileOutputStream(tempFile)
                            .use { output ->
                                input.copyTo(output)
                            }
                    }
            }

            tempFile

        } catch (e: Exception) {
            Log.e(
                TAG,
                "[P2P][FILE_RECEIVE_ERROR] " +
                    "Could not materialize payload $payloadId",
                e
            )

            null
        }

        if (
            destinationFile != null &&
            destinationFile.exists() &&
            destinationFile.length() > 0
        ) {
            Log.d(
                TAG,
                "[P2P][FILE_TRANSFER_COMPLETE] " +
                    "payloadId=$payloadId " +
                    "file=${destinationFile.absolutePath} " +
                    "size=${destinationFile.length()}"
            )

            listener?.onFileTransferComplete(
                endpointId,
                payloadId,
                destinationFile
            )

        } else {
            Log.e(
                TAG,
                "[P2P][FILE_TRANSFER_FAILED] " +
                    "payloadId=$payloadId completed but " +
                    "received file is invalid"
            )

            listener?.onFileTransferFailed(
                endpointId,
                payloadId
            )
        }
    }

    // --- COMMAND IMPLEMENTATIONS ---

    override fun startAdvertising(deviceName: String) {
        val actualDeviceName = _localEndpointName

        val options = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        Log.d(
            TAG,
            "[P2P][ADVERTISE] Starting advertising " +
                "name=$actualDeviceName " +
                "serviceId=$serviceId " +
                "strategy=P2P_CLUSTER"
        )

        connectionsClient
            .startAdvertising(
                actualDeviceName,
                serviceId,
                connectionLifecycleCallback,
                options
            )
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "[P2P][ADVERTISE][SUCCESS] name=$actualDeviceName"
                )
            }
            .addOnFailureListener { e ->
                val statusCode =
                    (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1

                val statusStr =
                    ConnectionsStatusCodes.getStatusCodeString(statusCode)

                Log.e(
                    TAG,
                    "[P2P][ADVERTISE][FAILED] " +
                        "statusCode=$statusCode " +
                        "status=$statusStr " +
                        "error=${e.message}",
                    e
                )

                listener?.onTransportError("Advertising failed: $statusStr")
            }
    }

    override fun stopAdvertising() {
        try {
            Log.d(TAG, "[P2P][ADVERTISE] Stopping advertising")
            connectionsClient.stopAdvertising()
        } catch (e: Exception) {
            Log.w(TAG, "[P2P][ADVERTISE] stopAdvertising warning", e)
        }
    }

    override fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        Log.d(TAG, "[P2P][DISCOVERY] Starting discovery serviceId=$serviceId strategy=P2P_CLUSTER")
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d(TAG, "[P2P][DISCOVERY] Discovery started successfully for $serviceId") }
            .addOnFailureListener { e ->
                val statusCode = (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                val statusStr = ConnectionsStatusCodes.getStatusCodeString(statusCode)
                Log.e(TAG, "[P2P][DISCOVERY][FAILED] statusCode=$statusCode status=$statusStr error=${e.message}", e)
                listener?.onTransportError("Discovery failed: $statusStr")
            }
    }

    override fun stopDiscovery() {
        try {
            Log.d(TAG, "[P2P][DISCOVERY] Stopping discovery")
            connectionsClient.stopDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "[P2P][DISCOVERY] stopDiscovery warning", e)
        }
    }

    override fun requestConnection(
        endpointId: String,
        endpointName: String
    ) {
        Log.d(
            TAG,
            "[P2P][REQUEST] Requesting connection " +
                "localName=$_localEndpointName " +
                "remoteEndpoint=$endpointId " +
                "remoteName=$endpointName"
        )

        connectionsClient
            .requestConnection(
                _localEndpointName,
                endpointId,
                connectionLifecycleCallback
            )
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "[P2P][REQUEST][SUCCESS] " +
                        "remoteEndpoint=$endpointId"
                )
            }
            .addOnFailureListener { e ->
                val statusCode =
                    (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1

                val statusStr =
                    ConnectionsStatusCodes.getStatusCodeString(statusCode)

                Log.e(
                    TAG,
                    "[P2P][REQUEST][FAILED] " +
                        "endpoint=$endpointId " +
                        "statusCode=$statusCode " +
                        "status=$statusStr " +
                        "error=${e.message}",
                    e
                )

                listener?.onConnectionRequestFailed(endpointId)
            }
    }

    override fun acceptConnection(endpointId: String) {
        Log.d(TAG, "[P2P][ACCEPT] Accepting connection from endpoint=$endpointId")
        connectionsClient.acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener { Log.d(TAG, "[P2P][ACCEPT] Connection accepted for endpoint=$endpointId") }
            .addOnFailureListener { e ->
                val statusCode = (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                val statusStr = ConnectionsStatusCodes.getStatusCodeString(statusCode)
                Log.e(TAG, "[P2P][ACCEPT_FAILED] Failed to accept connection from $endpointId: statusCode=$statusCode status=$statusStr", e)
            }
    }

    override fun rejectConnection(endpointId: String) {
        Log.d(TAG, "[P2P][REJECT] Rejecting connection from endpoint=$endpointId")
        connectionsClient.rejectConnection(endpointId)
    }

    override fun disconnect(endpointId: String) {
        Log.d(TAG, "[P2P][DISCONNECT] Disconnecting from endpoint=$endpointId")
        connectionsClient.disconnectFromEndpoint(endpointId)
    }

    override fun stopAllEndpoints() {
        try {
            Log.d(TAG, "[P2P][RESET] Stopping all endpoints")
            connectionsClient.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "[P2P][RESET] stopAllEndpoints warning", e)
        }
    }

    override fun sendBytes(endpointId: String, bytes: ByteArray) {
        Log.d(TAG, "[P2P][BYTES_SEND] Sending ${bytes.size} bytes to endpoint=$endpointId")
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { e ->
                val statusCode = (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                val statusStr = ConnectionsStatusCodes.getStatusCodeString(statusCode)
                Log.e(TAG, "[P2P][BYTES_SEND_FAILED] endpoint=$endpointId statusCode=$statusCode status=$statusStr error=${e.message}", e)
            }
    }

    override fun sendFile(endpointId: String, file: java.io.File): Long {
        if (!file.exists() || !file.isFile || file.length() == 0L) {
            Log.e(TAG, "[P2P][FILE_SEND_ERROR] Target file does not exist or is empty: ${file.absolutePath}")
            throw java.io.FileNotFoundException("File does not exist or is empty: ${file.absolutePath}")
        }
        Log.d(TAG, "[P2P][FILE_SEND_START] endpoint=$endpointId filename=${file.name} absolutePath=${file.absolutePath} size=${file.length()} bytes")
        val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        val payload = Payload.fromFile(pfd)
        Log.d(TAG, "[P2P][FILE_PAYLOAD_CREATED] payloadId=${payload.id} filename=${file.name} size=${file.length()} bytes")
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener { Log.d(TAG, "[P2P][FILE_PAYLOAD_SENT] payloadId=${payload.id} sent to $endpointId") }
            .addOnFailureListener { e ->
                val statusCode = (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                val statusStr = ConnectionsStatusCodes.getStatusCodeString(statusCode)
                Log.e(TAG, "[P2P][FILE_PAYLOAD_FAILED] payloadId=${payload.id} endpoint=$endpointId statusCode=$statusCode status=$statusStr error=${e.message}", e)
            }
        return payload.id
    }

    override fun sendFile(endpointId: String, fileUri: Uri): Long {
        val filePath = fileUri.path
        if (filePath != null) {
            val file = java.io.File(filePath)
            if (file.exists()) {
                return sendFile(endpointId, file)
            }
        }
        Log.d(TAG, "[P2P][FILE_SEND_START] endpoint=$endpointId uri=$fileUri")
        val pfd = context.contentResolver.openFileDescriptor(fileUri, "r")
            ?: throw java.io.FileNotFoundException("Could not open FileDescriptor for $fileUri")
        val payload = Payload.fromFile(pfd)
        Log.d(TAG, "[P2P][FILE_PAYLOAD_CREATED] payloadId=${payload.id} from uri=$fileUri")
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener { Log.d(TAG, "[P2P][FILE_PAYLOAD_SENT] payloadId=${payload.id} sent to $endpointId") }
            .addOnFailureListener { e ->
                val statusCode = (e as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                val statusStr = ConnectionsStatusCodes.getStatusCodeString(statusCode)
                Log.e(TAG, "[P2P][FILE_PAYLOAD_FAILED] payloadId=${payload.id} endpoint=$endpointId statusCode=$statusCode status=$statusStr error=${e.message}", e)
            }
        return payload.id
    }
}
