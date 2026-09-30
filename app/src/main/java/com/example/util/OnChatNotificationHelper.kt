package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R

/**
 * OnChat Rich Push Notification Manager
 * Features:
 * - Left side: Circular caller/sender avatar with custom colored border ring
 * - Center: Full name, status preview, and action context
 * - Right side: Crisp circular OnChat app logo badge (un-distorted, beautifully formatted)
 * - Action buttons: Direct Accept/Decline for incoming calls, Callback for missed calls, Inline Reply for messages
 * - Custom RemoteViews styling with full backward compatibility
 */
object OnChatNotificationHelper {
    private const val TAG = "OnChatNotifHelper"

    const val CHANNEL_CALLS_ID = "onchat_incoming_calls"
    const val CHANNEL_MISSED_CALLS_ID = "onchat_missed_calls"
    const val CHANNEL_MESSAGES_ID = "onchat_messages"

    const val ACTION_ACCEPT_CALL = "com.example.onchat.ACTION_ACCEPT_CALL"
    const val ACTION_DECLINE_CALL = "com.example.onchat.ACTION_DECLINE_CALL"
    const val ACTION_START_CALL = "com.example.onchat.ACTION_START_CALL"
    const val ACTION_REPLY_MESSAGE = "com.example.onchat.ACTION_REPLY_MESSAGE"
    const val ACTION_OPEN_CHAT = "com.example.onchat.ACTION_OPEN_CHAT"

    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_CALLER_WALLET_ID = "extra_caller_wallet_id"
    const val EXTRA_IS_VIDEO = "extra_is_video"
    const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    const val KEY_TEXT_REPLY = "key_text_reply"

    private const val NOTIFICATION_CALL_ID = 9001
    private const val NOTIFICATION_BASE_MISSED_ID = 9500
    private const val NOTIFICATION_BASE_MSG_ID = 8000

    /**
     * Create notification channels for Android 8.0+
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. High Priority Incoming Call Channel with ringtone and vibration
            val callRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributesCall = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_CALLS_ID,
                "Appels entrants OnChat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les appels vocaux et vidéo entrants"
                setSound(callRingtoneUri, audioAttributesCall)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // 2. High Priority Missed Call Channel with notification sound
            val notifSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributesNotif = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val missedCallChannel = NotificationChannel(
                CHANNEL_MISSED_CALLS_ID,
                "Appels manqués OnChat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes pour les appels audio et vidéo manqués"
                setSound(notifSoundUri, audioAttributesNotif)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // 3. High Priority Message Channel
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Messages OnChat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les nouveaux messages et alertes reçus"
                setSound(notifSoundUri, audioAttributesNotif)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(missedCallChannel)
            notificationManager.createNotificationChannel(msgChannel)
        }
    }

    /**
     * Generate a circular avatar with a distinct colorful border ring
     */
    fun createCircularAvatarWithBorder(
        name: String,
        borderColor: Int = Color.parseColor("#06B6D4"),
        sizePx: Int = 160
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = intArrayOf(
            Color.parseColor("#4338CA"),
            Color.parseColor("#0891B2"),
            Color.parseColor("#059669"),
            Color.parseColor("#7C3AED"),
            Color.parseColor("#DB2777"),
            Color.parseColor("#D97706")
        )
        val colorIndex = Math.abs(name.hashCode()) % colors.size

        val radius = sizePx / 2f
        val strokeWidth = sizePx * 0.06f
        val innerRadius = radius - strokeWidth

        // Draw background circle
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors[colorIndex]
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius, radius, innerRadius, fillPaint)

        // Draw colored ring border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(radius, radius, radius - (strokeWidth / 2f), borderPaint)

        // Draw user initial
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.44f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val initial = name.trim().take(1).uppercase().ifBlank { "O" }
        val textBounds = Rect()
        textPaint.getTextBounds(initial, 0, initial.length, textBounds)
        val yOffset = radius + (textBounds.height() / 2f)
        canvas.drawText(initial, radius, yOffset, textPaint)

        return bitmap
    }

    /**
     * Generate a crisp circular OnChat App Logo badge
     */
    fun getCircularAppLogoBadge(context: Context, sizePx: Int = 120): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        var srcBitmap: Bitmap? = null
        try {
            val resId = R.drawable.onchat_logo
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            srcBitmap = BitmapFactory.decodeResource(context.resources, resId, opts)
        } catch (_: Exception) {}

        val radius = sizePx / 2f
        val strokeWidth = sizePx * 0.05f
        val innerRadius = radius - strokeWidth

        if (srcBitmap != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val shader = BitmapShader(
                Bitmap.createScaledBitmap(srcBitmap, sizePx, sizePx, true),
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            canvas.drawCircle(radius, radius, innerRadius, paint)
        } else {
            // High quality fallback vector rendered OnChat badge
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4338CA")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(radius, radius, innerRadius, bgPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = sizePx * 0.38f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val textBounds = Rect()
            textPaint.getTextBounds("ON", 0, 2, textBounds)
            canvas.drawText("ON", radius, radius + (textBounds.height() / 2f), textPaint)
        }

        // Distinct border ring for OnChat brand
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(radius, radius, radius - (strokeWidth / 2f), borderPaint)

        return output
    }

    /**
     * Show rich Incoming Call notification with Left Avatar (colored ring), Center Caller Name,
     * Right Circular OnChat Logo badge, and Décrocher / Refuser action buttons.
     */
    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        callerWalletId: String,
        callerAvatar: String? = null,
        isVideo: Boolean = false
    ) {
        createNotificationChannels(context)

        val avatarBitmap = createCircularAvatarWithBorder(
            name = callerName,
            borderColor = if (isVideo) Color.parseColor("#8B5CF6") else Color.parseColor("#06B6D4")
        )
        val onchatLogoBitmap = getCircularAppLogoBadge(context)

        // Main Tap Intent: opens app and shows call screen
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_WALLET_ID, callerWalletId)
            putExtra(EXTRA_IS_VIDEO, isVideo)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Accept Action Intent (Green)
        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_WALLET_ID, callerWalletId)
            putExtra(EXTRA_IS_VIDEO, isVideo)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            1002,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Decline Action Intent (Red)
        val declineIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_DECLINE_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_WALLET_ID, callerWalletId)
            putExtra(EXTRA_IS_VIDEO, isVideo)
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context,
            1003,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val callTypeLabel = if (isVideo) "Appel vidéo entrant..." else "Appel audio entrant..."
        val subtitle = if (callerWalletId.isNotBlank()) "Wallet: $callerWalletId" else "OnChat Appel"

        // Build Custom RemoteViews for custom layout
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_incoming_call).apply {
            setImageViewBitmap(R.id.notif_caller_avatar, avatarBitmap)
            setTextViewText(R.id.notif_caller_name, callerName)
            setTextViewText(R.id.notif_call_status, callTypeLabel)
            setTextViewText(R.id.notif_call_wallet, subtitle)
            setImageViewBitmap(R.id.notif_app_logo, onchatLogoBitmap)
            setOnClickPendingIntent(R.id.notif_btn_accept, acceptPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_decline, declinePendingIntent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setLargeIcon(avatarBitmap)
            .setContentTitle(callerName)
            .setContentText("$callTypeLabel • $subtitle")
            .setSubText("OnChat")
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setCustomHeadsUpContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Refuser", declinePendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Décrocher", acceptPendingIntent)
            .setColor(Color.parseColor("#06B6D4"))

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_CALL_ID, builder.build())
            Log.d(TAG, "Incoming call notification shown for $callerName")
        } catch (e: Exception) {
            Log.e(TAG, "Could not show incoming call notification: ${e.message}")
        }
    }

    /**
     * Show rich Missed Call notification with Left Avatar, Center Name & Action, and Right OnChat Logo
     */
    fun showMissedCallNotification(
        context: Context,
        callerName: String,
        callerWalletId: String?,
        isVideo: Boolean = false
    ) {
        createNotificationChannels(context)

        val avatarBitmap = createCircularAvatarWithBorder(
            name = callerName,
            borderColor = Color.parseColor("#EF4444")
        )
        val onchatLogoBitmap = getCircularAppLogoBadge(context)
        val notifId = NOTIFICATION_BASE_MISSED_ID + (Math.abs((callerWalletId ?: callerName).hashCode()) % 500)

        // Tap on notification opens the calls screen
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Callback Action Intent: directly calls back the peer
        val callBackIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_START_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_WALLET_ID, callerWalletId ?: "")
            putExtra(EXTRA_IS_VIDEO, isVideo)
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            context,
            notifId + 1000,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val callTypeLabel = if (isVideo) "Appel vidéo manqué" else "Appel vocal manqué"
        val subtitle = if (!callerWalletId.isNullOrBlank()) "Wallet: $callerWalletId" else "Appuyez pour rappeler"

        val remoteViews = RemoteViews(context.packageName, R.layout.notification_missed_call).apply {
            setImageViewBitmap(R.id.notif_caller_avatar, avatarBitmap)
            setTextViewText(R.id.notif_caller_name, callerName)
            setTextViewText(R.id.notif_call_status, callTypeLabel)
            setTextViewText(R.id.notif_call_wallet, subtitle)
            setImageViewBitmap(R.id.notif_app_logo, onchatLogoBitmap)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setLargeIcon(avatarBitmap)
            .setContentTitle("Appel manqué : $callerName")
            .setContentText("$callTypeLabel ($subtitle)")
            .setSubText("OnChat Appel")
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Rappeler", callBackPendingIntent)
            .setColor(Color.parseColor("#EF4444"))

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notifId, builder.build())
            Log.d(TAG, "Missed call notification shown for $callerName (id=$notifId)")
        } catch (e: Exception) {
            Log.e(TAG, "Could not show missed call notification: ${e.message}")
        }
    }

    /**
     * Cancel incoming call notification
     */
    fun cancelIncomingCallNotification(context: Context) {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(NOTIFICATION_CALL_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Could not cancel incoming call notification: ${e.message}")
        }
    }

    /**
     * Show rich Message notification with Left Avatar (Cyan ring), Center Sender & Preview, Right OnChat Logo, and Direct Reply
     */
    fun showMessageNotification(
        context: Context,
        conversationId: String,
        senderName: String,
        senderWalletId: String?,
        messageText: String
    ) {
        createNotificationChannels(context)

        val avatarBitmap = createCircularAvatarWithBorder(
            name = senderName,
            borderColor = Color.parseColor("#38BDF8")
        )
        val onchatLogoBitmap = getCircularAppLogoBadge(context)
        val notificationId = NOTIFICATION_BASE_MSG_ID + (Math.abs(conversationId.hashCode()) % 1000)

        // Tap on notification opens the specific chat
        val openChatIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CHAT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
        }
        val openChatPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Inline Quick Reply action
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Répondre...")
            .build()

        val replyIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REPLY_MESSAGE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
        }
        val replyPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 2000,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Répondre",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val remoteViews = RemoteViews(context.packageName, R.layout.notification_message).apply {
            setImageViewBitmap(R.id.notif_sender_avatar, avatarBitmap)
            setTextViewText(R.id.notif_sender_name, senderName)
            setTextViewText(R.id.notif_message_text, messageText)
            setTextViewText(R.id.notif_message_sub, senderWalletId?.let { "ID: $it • OnChat" } ?: "OnChat Message")
            setImageViewBitmap(R.id.notif_app_logo, onchatLogoBitmap)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setLargeIcon(avatarBitmap)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSubText(senderWalletId?.let { "ID: $it" } ?: "OnChat")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openChatPendingIntent)
            .addAction(replyAction)
            .addAction(android.R.drawable.ic_menu_view, "Ouvrir", openChatPendingIntent)
            .setColor(Color.parseColor("#4338CA"))

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
            Log.d(TAG, "Message notification shown for conv $conversationId")
        } catch (e: Exception) {
            Log.e(TAG, "Could not show message notification: ${e.message}")
        }
    }
}
