package soc.athara.app.shared.payments.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat


enum class BleState {
    
    UNSUPPORTED,
    
    OFF,
    
    NEEDS_PERMISSION,
    
    LOCATION_OFF,
    READY,
}

object BlePermissions {
    
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasAll(context: Context): Boolean =
        required().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun state(context: Context): BleState {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return BleState.UNSUPPORTED
        val adapter = adapter(context) ?: return BleState.UNSUPPORTED
        if (!hasAll(context)) return BleState.NEEDS_PERMISSION
        if (!adapter.isEnabled) return BleState.OFF
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !locationOn(context)) return BleState.LOCATION_OFF
        return BleState.READY
    }

    internal fun adapter(context: Context): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private fun locationOn(context: Context): Boolean =
        context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true
}
