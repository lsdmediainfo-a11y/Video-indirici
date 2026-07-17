package com.antigravity.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadForegroundService : Service() {

    private val CHANNEL_ID = "antigravity_dl_channel"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("URL") ?: return START_NOT_STICKY
        val fileName = intent.getStringExtra("FILE_NAME") ?: "download.mp4"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("İndiriliyor: $fileName")
            .setContentText("Bağlantılar kuruluyor (8 Thread)...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()

        startForeground(1, notification)

        val downloader = MultiThreadDownloader(threadCount = 8)
        val destFile = File(getExternalFilesDir(null), fileName)

        scope.launch {
            downloader.download(url, destFile) { progress ->
                val update = NotificationCompat.Builder(this@DownloadForegroundService, CHANNEL_ID)
                    .setContentTitle("İndiriliyor: $fileName")
                    .setContentText("%$progress tamamlandı")
                    .setProgress(100, progress, false)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .build()
                
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(1, update)
            }
            
            // On Complete
            val complete = NotificationCompat.Builder(this@DownloadForegroundService, CHANNEL_ID)
                .setContentTitle("İndirme Tamamlandı")
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .build()
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, complete)
            
            stopForeground(true)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Antigravity Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
