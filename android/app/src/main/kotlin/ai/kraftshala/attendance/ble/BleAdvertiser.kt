package ai.kraftshala.attendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import timber.log.Timber
import java.util.UUID

/**
 * Used by the instructor app to broadcast a BLE advertisement carrying the day's session nonce.
 * Students within range passively detect this.
 */
class BleAdvertiser(context: Context) {
    private val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val advertiser: BluetoothLeAdvertiser? = btManager.adapter?.bluetoothLeAdvertiser
    private var currentCallback: AdvertiseCallback? = null

    @SuppressLint("MissingPermission")
    fun start(sessionNonce: String) {
        val adv = advertiser ?: run { Timber.w("BLE advertising not supported"); return }
        stop()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        // Carry the nonce in the service data payload
        val nonceBytes = sessionNonce.take(MAX_PAYLOAD).toByteArray(Charsets.UTF_8)
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(KRAFTSHALA_UUID))
            .addServiceData(ParcelUuid(KRAFTSHALA_UUID), nonceBytes)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Timber.i("BLE advertising started")
            }
            override fun onStartFailure(errorCode: Int) {
                Timber.e("BLE advertising failed: $errorCode")
            }
        }
        currentCallback = callback
        adv.startAdvertising(settings, data, callback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        currentCallback?.let { advertiser?.stopAdvertising(it) }
        currentCallback = null
    }

    companion object {
        val KRAFTSHALA_UUID: UUID = UUID.fromString("00001815-0000-1000-8000-00805F9B34FB")
        private const val MAX_PAYLOAD = 20
    }
}
