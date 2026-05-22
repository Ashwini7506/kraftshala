package ai.kraftshala.attendance.ble

import ai.kraftshala.attendance.KraftshalaApp
import ai.kraftshala.attendance.R
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import timber.log.Timber

/**
 * Foreground service required by Android 12+ to keep BLE running when the screen is off.
 * Runs both modes:
 *   - Instructor: starts BleAdvertiser
 *   - Student: starts BleScanner
 * Determined by intent extras.
 */
class BleSessionService : Service() {

    private var advertiser: BleAdvertiser? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nonce = intent?.getStringExtra(EXTRA_NONCE) ?: return START_NOT_STICKY
        val isInstructor = intent.getBooleanExtra(EXTRA_IS_INSTRUCTOR, false)

        startForeground(NOTIFICATION_ID, buildNotification())

        if (isInstructor) {
            advertiser = BleAdvertiser(this).also { it.start(nonce) }
            Timber.i("Foreground service: instructor broadcasting")
        } else {
            Timber.i("Foreground service: student scanning")
            // Scanning flow is observed by the ViewModel directly; this service just keeps the process alive.
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, KraftshalaApp.BLE_CHANNEL_ID)
            .setContentTitle(getString(R.string.ble_service_name))
            .setContentText(getString(R.string.ble_service_description))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        advertiser?.stop()
        Timber.i("BLE service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 9001
        const val EXTRA_NONCE = "extra_nonce"
        const val EXTRA_IS_INSTRUCTOR = "extra_is_instructor"
    }
}
