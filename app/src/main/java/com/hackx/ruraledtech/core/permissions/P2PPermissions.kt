package com.hackx.ruraledtech.core.permissions

import android.Manifest
import android.os.Build

/**
 * Permissions Nearby Connections needs before [com.hackx.ruraledtech.p2p.mesh.LearningMesh
 * .startMesh] is called (Android 12/API 31 replaced the old BLUETOOTH permissions with
 * runtime-granted ones). Request these — via [androidx.activity.result.contract
 * .ActivityResultContracts.RequestMultiplePermissions] in the composable that triggers the
 * mesh — before calling startMesh(), not after; Nearby Connections fails silently rather
 * than throwing if scanning/advertising permissions are missing.
 */
object P2PPermissions {
    val required: Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()
}
