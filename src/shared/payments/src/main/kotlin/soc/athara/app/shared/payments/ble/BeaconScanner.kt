package soc.athara.app.shared.payments.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Listens for beacons of ONE kind ([BeaconCodec.TYPE_MERCHANT] for a payer, [BeaconCodec.TYPE_PAYER] for a merchant).
 * The filter runs in the Bluetooth chip, so other devices' adverts never wake the app. Results are delivered on the main thread.
 *
 * Start it once per payment and stop it when done: Android limits how often a scan may be restarted.
 */
class BeaconScanner(private val context: Context, private val type: Byte) {

    private val main = Handler(Looper.getMainLooper())
    private var callback: ScanCallback? = null

    /** [onHeard] gets each beacon with its signal strength (dBm). [onFailed] gets the ScanCallback error code. False when it could not start. */
    @SuppressLint("MissingPermission")
    fun start(onHeard: (BeaconCodec.Heard, Int) -> Unit, onFailed: (Int) -> Unit = {}): Boolean {
        val scanner = try {
            BlePermissions.adapter(context)?.takeIf { it.isEnabled }?.bluetoothLeScanner
        } catch (e: SecurityException) {
            null
        } ?: return false
        stop()
        val filter = ScanFilter.Builder()
            .setManufacturerData(BeaconCodec.COMPANY_ID, byteArrayOf(type), byteArrayOf(0xFF.toByte()))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .build()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val payload = result.scanRecord?.getManufacturerSpecificData(BeaconCodec.COMPANY_ID)
                val heard = BeaconCodec.parse(payload) ?: return
                val rssi = result.rssi
                main.post { if (callback === this) onHeard(heard, rssi) }
            }

            override fun onScanFailed(errorCode: Int) {
                main.post { if (callback === this) onFailed(errorCode) }
            }
        }
        return try {
            scanner.startScan(listOf(filter), settings, cb)
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
            BlePermissions.adapter(context)?.bluetoothLeScanner?.stopScan(cb)
        } catch (e: SecurityException) {
            // Permission taken away while running: nothing left to stop.
        } catch (e: IllegalStateException) {
            // Bluetooth turned off: the scan already stopped.
        }
    }
}
