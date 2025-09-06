package de.svws_nfc.simpleclone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class CloneService : Service() {

    companion object {
        const val CHANNEL_ID = "clone_service"
        const val ACTION_START = "de.svws_nfc.simpleclone.action.START"
        const val ACTION_STOP  = "de.svws_nfc.simpleclone.action.STOP"
        const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID, "Clone", NotificationManager.IMPORTANCE_LOW
            )
            mgr?.createNotificationChannel(ch)
            val n: Notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Cloning")
                .setContentText("Running…")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .build()
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // TODO: start actual cloning work (NFC ops, file IO, etc.)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
