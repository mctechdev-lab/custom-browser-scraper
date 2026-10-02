package com.example.custombrowser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class ExtractorService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetUrl = intent?.getStringExtra("TARGET_URL") ?: return START_NOT_STICKY
        val cookies = intent.getStringExtra("COOKIES") ?: ""

        startForegroundNotification()
        acquireWakeLock()

        scope.launch {
            while (isActive) {
                try {
                    val htmlContent = fetchPageWithCookies(targetUrl, cookies)
                    
                    // Replace with your active Python backend URL when ready
                    postToPythonBackend("https://httpbin.org/post", htmlContent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(30_000)
            }
        }

        return START_STICKY
    }

    private fun fetchPageWithCookies(targetUrl: String, cookies: String): String {
        val url = URL(targetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Cookie", cookies)
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun postToPythonBackend(apiUrl: String, payload: String) {
        val url = URL(apiUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "text/plain")
        conn.doOutput = true
        conn.outputStream.use { it.write(payload.toByteArray()) }
        conn.responseCode
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Scraper::WakeLock").apply {
            acquire()
        }
    }

    private fun startForegroundNotification() {
        val channelId = "sync_channel"
        val channel = NotificationChannel(channelId, "Background Sync", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Custom Browser Syncing")
            .setContentText("Background extraction active...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
