package soc.athara.app.shared.payments.ble

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context








class BeaconAdvertiser(private val context: Context) {

    private var callback: AdvertiseCallback? = null

    
    @SuppressLint("MissingPermission")
    fun isSupported(): Boolean {
        val adapter = BlePermissions.adapter(context) ?: return false
        return try {
            adapter.isEnabled && adapter.isMultipleAdvertisementSupported && adapter.bluetoothLeAdvertiser != null
        } catch (e: SecurityException) {
            false
        }
    }

    
    @SuppressLint("MissingPermission")
    fun start(payload: ByteArray, onFailed: (Int) -> Unit = {}): Boolean {
        val advertiser = try {
            BlePermissions.adapter(context)?.takeIf { it.isEnabled }?.bluetoothLeAdvertiser
        } catch (e: SecurityException) {
            null
        } ?: return false
        stop()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(BeaconCodec.COMPANY_ID, payload)
            .build()
        val cb = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                if (callback === this) onFailed(errorCode)
            }
        }
        return try {
            advertiser.startAdvertising(settings, data, cb)
            callback = cb
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val cb = callback ?: return
        callback = null
        try {
            BlePermissions.adapter(context)?.bluetoothLeAdvertiser?.stopAdvertising(cb)
        } catch (e: SecurityException) {
            
        } catch (e: IllegalStateException) {
            
        }
    }
}
