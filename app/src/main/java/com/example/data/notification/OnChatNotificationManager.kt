package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object OnChatNotificationManager {

    private const val CHANNEL_MESSAGES_ID = "onchat_messages_channel"
    private const val CHANNEL_CALLS_ID = "onchat_calls_channel"
    private const val CHANNEL_SOCIAL_ID = "onchat_social_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Messages & Discussions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les nouveaux messages OnChat"
                enableVibration(true)
            }

            val callsChannel = NotificationChannel(
                CHANNEL_CALLS_ID,
                "Appels Entrants",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Notifications pour les appels vocaux et vidéo"
                enableVibration(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_ID,
                "Publications & Abonnements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les nouveaux abonnés, vidéos et stories"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(callsChannel)
            manager.createNotificationChannel(socialChannel)
        }
    }

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        conversationId: String? = null
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!conversationId.isNullOrBlank()) {
                putExtra("conversationId", conversationId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun showCallNotification(
        context: Context,
        callerName: String,
        isVideo: Boolean
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            8888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeStr = if (isVideo) "Appel Vidéo entrant" else "Appel Vocal entrant"

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText("$typeStr...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(9999, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun showSocialNotification(
        context: Context,
        title: String,
        body: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
