package com.hackx.ruraledtech.p2p.connection

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyConnectionManagerImpl(
    private val context: Context,
    private val listener: ConnectionListener
) : P2PConnectionManager {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "com.hackx.ruraledtech.mesh"
    
    private val TAG = "NearbyTransport"

    // 1. DISCOVERY CALLBACKS
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Peer discovered: $endpointId (${info.endpointName})")
            listener.onPeerDiscovered(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Peer lost: $endpointId")
            listener.onPeerLost(endpointId)
        }
    }

    // 2. CONNECTION LIFECYCLE CALLBACKS
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: $endpointId. Auth token: ${info.authenticationToken}")
            listener.onConnectionInitiated(endpointId, info.endpointName, info.authenticationToken)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> listener.onConnectionAccepted(endpointId)
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> listener.onConnectionRejected(endpointId)
                else -> listener.onDisconnected(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")
            listener.onDisconnected(endpointId)
        }
    }

    private val incomingFilePayloads = java.util.concurrent.ConcurrentHashMap<Long, Payload>()

    // 3. PAYLOAD CALLBACKS (Data Transfer)
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let { listener.onBytesReceived(endpointId, it) }
                }
                Payload.Type.FILE -> {
                    Log.d(TAG, "Incoming file payload detected: ${payload.id}")
                    incomingFilePayloads[payload.id] = payload
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress = if (update.totalBytes > 0) {
                        ((update.bytesTransferred.toFloat() / update.totalBytes) * 100).toInt()
                    } else 0
                    listener.onFileTransferProgress(endpointId, update.payloadId, progress)
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    val payload = incomingFilePayloads.remove(update.payloadId)
                    val file = payload?.asFile()?.asJavaFile() ?: java.io.File(context.cacheDir, "payload_${update.payloadId}.pkg")
                    listener.onFileTransferComplete(endpointId, update.payloadId, file)
                }
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED -> {
                    incomingFilePayloads.remove(update.payloadId)
                    listener.onFileTransferFailed(endpointId, update.payloadId)
                }
            }
        }
    }

    // --- COMMAND IMPLEMENTATIONS ---

    override fun startAdvertising(deviceName: String) {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(deviceName, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener { Log.d(TAG, "Advertising started") }
            .addOnFailureListener { Log.e(TAG, "Advertising failed", it) }
    }

    override fun stopAdvertising() {
        connectionsClient.stopAdvertising()
    }

    override fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d(TAG, "Discovery started") }
            .addOnFailureListener { Log.e(TAG, "Discovery failed", it) }
    }

    override fun stopDiscovery() {
        connectionsClient.stopDiscovery()
    }

    override fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    override fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
    }

    override fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
    }

    override fun sendBytes(endpointId: String, bytes: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun sendFile(endpointId: String, fileUri: Uri): Long {
        val payload = Payload.fromFile(context.contentResolver.openFileDescriptor(fileUri, "r")!!)
        connectionsClient.sendPayload(endpointId, payload)
        return payload.id
    }
}
