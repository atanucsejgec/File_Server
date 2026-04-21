package com.apk.fileserver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.apk.fileserver.MainActivity
import com.apk.fileserver.R

class FileServerService : Service() {

    // ═══════════════════════════════════════════════
    //              CONSTANTS
    // ═══════════════════════════════════════════════

    companion object {
        const val CHANNEL_ID = "file_server_channel"
        const val CHANNEL_NAME = "File Server"
        const val NOTIFICATION_ID = 1001

        // Intent actions
        const val ACTION_START = "com.apk.fileserver.START"
        const val ACTION_STOP  = "com.apk.fileserver.STOP"

        // Intent extras
        const val EXTRA_PORT    = "extra_port"
        const val EXTRA_IP      = "extra_ip"
        const val EXTRA_URL     = "extra_url"

        /**
         * Build intent to start service
         */
        fun buildStartIntent(
            context: Context,
            port: Int,
            ip: String,
            url: String
        ): Intent {
            return Intent(context, FileServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_URL, url)
            }
        }

        /**
         * Build intent to stop service
         */
        fun buildStopIntent(context: Context): Intent {
            return Intent(context, FileServerService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    // ═══════════════════════════════════════════════
    //              BINDER (for Activity binding)
    // ═══════════════════════════════════════════════

    inner class LocalBinder : Binder() {
        fun getService(): FileServerService = this@FileServerService
    }

    private val binder = LocalBinder()

    // ═══════════════════════════════════════════════
    //              STATE
    // ═══════════════════════════════════════════════

    private var serverPort = 8080
    private var serverIp   = ""
    private var serverUrl  = ""
    private var isRunning  = false

    // ═══════════════════════════════════════════════
    //              LIFECYCLE
    // ═══════════════════════════════════════════════

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                serverPort = intent.getIntExtra(EXTRA_PORT, 8080)
                serverIp   = intent.getStringExtra(EXTRA_IP) ?: ""
                serverUrl  = intent.getStringExtra(EXTRA_URL) ?: ""
                isRunning  = true
                startForeground(NOTIFICATION_ID, buildNotification())
            }

            ACTION_STOP -> {
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        // START_STICKY: system restarts service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    // ═══════════════════════════════════════════════
    //              NOTIFICATION CHANNEL
    // ═══════════════════════════════════════════════

    /**
     * Create notification channel (required Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // Low = no sound
            ).apply {
                description = "Shows when file server is running"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    // ═══════════════════════════════════════════════
    //              NOTIFICATION BUILDER
    // ═══════════════════════════════════════════════

    /**
     * Build the persistent foreground notification
     * Shows server URL and stop button
     */
    private fun buildNotification(): Notification {
        // Tap notification → open app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openAppPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // Stop button in notification
        val stopIntent = buildStopIntent(this)
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (serverUrl.isNotEmpty()) {
            "Running at $serverUrl"
        } else {
            "Server is running on port $serverPort"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LocalShare Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(openAppPending)
            .setOngoing(true)           // Cannot be swiped away
            .setSilent(true)            // No sound/vibration
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Server",
                stopPending
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Server running at $serverUrl\n" +
                                "Open browser on PC and navigate to this address"
                    )
            )
            .build()
    }

    // ═══════════════════════════════════════════════
    //              PUBLIC METHODS
    // ═══════════════════════════════════════════════

    /**
     * Update notification with new info
     * Call when IP or port changes
     */
    fun updateNotification(ip: String, port: Int, url: String) {
        serverIp   = ip
        serverPort = port
        serverUrl  = url

        if (isRunning) {
            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    /**
     * Check if service is currently running
     */
    fun isServerRunning(): Boolean = isRunning

    /**
     * Get current server URL
     */
    fun getServerUrl(): String = serverUrl
}