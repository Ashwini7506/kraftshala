package ai.kraftshala.attendance

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import timber.log.Timber

class KraftshalaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BLE_CHANNEL_ID,
                "Kraftshala session",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Active BLE session tracking" }
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    companion object {
        const val BLE_CHANNEL_ID = "kraftshala_ble_session"
    }
}
