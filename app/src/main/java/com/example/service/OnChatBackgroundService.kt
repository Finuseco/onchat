package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.api.OnChatWebSocketManager
import com.example.util.OnChatNotificationHelper
import com.example.util.RingtoneHelper
import kotlinx.coroutines.*

/**
 * OnChat 24/7 Background Service
 * Keeps WebSocket connection active in background just like WhatsApp.
 * Wakes up device screen, triggers real-time incoming call ringtone, and handles incoming messages.
 */
class OnChatBackgroundService : Service() {

    companion object {
        private const val TAG = "OnChatBgService"
        private const val NOTIF_CHANNEL_ID = "onchat_persistent_service"
        private const val NOTIF_ID = 7001

        fun start(context: Context) {
            val intent = Intent(context, OnChatBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "OnChatBackgroundService created")
        createServiceNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    buildForegroundNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIF_ID, buildForegroundNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }

        val wsManager = OnChatWebSocketManager.getInstance()
        wsManager.connect()

        observeEvents(wsManager)
    }

    private fun observeEvents(wsManager: OnChatWebSocketManager) {
        // 1. Incoming calls: Wake up phone and ring
        serviceScope.launch {
            wsManager.incomingCalls.collect { callState ->
                wakeUpPhoneScreen()
                RingtoneHelper.playCallRingtone(applicationContext)
                OnChatNotificationHelper.showIncomingCallNotification(
                    context = applicationContext,
                    callId = callState.callId,
                    callerName = callState.callerName,
                    callerWalletId = callState.callerWalletId,
                    callerAvatar = callState.callerAvatar,
                    isVideo = callState.isVideo
                )
            }
        }

        // 2. Incoming messages
        serviceScope.launch {
            wsManager.incomingMessages.collect { pair ->
                RingtoneHelper.playNotificationSound(applicationContext)
                OnChatNotificationHelper.showMessageNotification(
                    context = applicationContext,
                    conversationId = pair.first,
                    senderName = pair.second.senderName ?: "Message OnChat",
                    senderWalletId = null,
                    messageText = pair.second.content
                )
            }
        }
    }

    private fun wakeUpPhoneScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                @Suppress("DEPRECATION")
                val wl = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "OnChat:CallWakeLock"
                )
                wl.acquire(30000L) // Keep screen awake for 30s
                wakeLock = wl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire WakeLock: ${e.message}")
        }
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Service OnChat temps réel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient les appels et notifications en temps réel"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("OnChat Connecté")
            .setContentText("En attente d'appels et messages en temps réel 24/7")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        OnChatWebSocketManager.getInstance().connect()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        Log.i(TAG, "OnChatBackgroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
