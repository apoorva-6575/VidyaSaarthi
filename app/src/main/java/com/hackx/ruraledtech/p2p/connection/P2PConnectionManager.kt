package com.hackx.ruraledtech.p2p.connection

import android.net.Uri

interface P2PConnectionManager {
    fun setListener(listener: ConnectionListener)
    fun startAdvertising(deviceName: String)
    fun stopAdvertising()
    
    fun startDiscovery()
    fun stopDiscovery()
    
    fun requestConnection(endpointId: String, endpointName: String = "RuralEdTech-Node")
    fun acceptConnection(endpointId: String)
    fun rejectConnection(endpointId: String)
    fun disconnect(endpointId: String)
    
    fun sendBytes(endpointId: String, bytes: ByteArray)
    fun sendFile(endpointId: String, fileUri: Uri): Long // Returns payloadId
}
