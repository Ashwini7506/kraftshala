package ai.kraftshala.attendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * Used by the student app to scan for the instructor's BLE broadcast.
 * Emits a stream of (nonce, rssi, detected_at) tuples.
 */
data class BleDetection(
    val nonce: String,
    val rssi: Int,
    val detectedAtMs: Long
)

class BleScanner(context: Context) {
    private val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val scanner: BluetoothLeScanner? = btManager.adapter?.bluetoothLeScanner

    @SuppressLint("MissingPermission")
    fun scan(): Flow<BleDetection> = callbackFlow {
        val s = scanner ?: run { close(); return@callbackFlow }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleAdvertiser.KRAFTSHALA_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord ?: return
                val data = record.serviceData[ParcelUuid(BleAdvertiser.KRAFTSHALA_UUID)] ?: return
                val nonce = String(data, Charsets.UTF_8)
                trySend(BleDetection(nonce, result.rssi, System.currentTimeMillis()))
            }
            override fun onScanFailed(errorCode: Int) {
                Timber.e("BLE scan failed: $errorCode")
                close()
            }
        }

        s.startScan(listOf(filter), settings, callback)
        awaitClose { s.stopScan(callback) }
    }
}
