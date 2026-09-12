package com.hackx.ruraledtech.core.permissions

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
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

    fun hasAllPermissions(context: Context): Boolean {
        return required.all { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMissingPermissions(context: Context): List<String> {
        return required.filter { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        return bluetoothManager.adapter?.isEnabled == true
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        return wifiManager.isWifiEnabled
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun isDeviceReadyForNearby(context: Context): Boolean {
        return hasAllPermissions(context) &&
            isBluetoothEnabled(context) &&
            isWifiEnabled(context) &&
            isLocationEnabled(context)
    }
}
