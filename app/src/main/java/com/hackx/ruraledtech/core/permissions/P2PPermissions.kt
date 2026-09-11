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
    val required: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
