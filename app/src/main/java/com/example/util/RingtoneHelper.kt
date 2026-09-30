package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object RingtoneHelper {
    private const val TAG = "RingtoneHelper"
    private var activeRingtone: Ringtone? = null
    private var activeMediaPlayer: android.media.MediaPlayer? = null

    data class RingtoneOption(
        val title: String,
        val uriString: String
    )

    fun getAvailableRingtones(context: Context, type: Int = RingtoneManager.TYPE_RINGTONE): List<RingtoneOption> {
        val list = mutableListOf<RingtoneOption>()
        try {
            val manager = RingtoneManager(context)
            manager.setType(type)
            val cursor = manager.cursor
            while (cursor != null && cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = manager.getRingtoneUri(cursor.position)
                if (title != null && uri != null) {
                    list.add(RingtoneOption(title, uri.toString()))
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            list.add(RingtoneOption("Sonnerie par défaut du système", "default"))
            list.add(RingtoneOption("OnChat Chime", "onchat_chime"))
            list.add(RingtoneOption("CashPay Pulse", "cashpay_pulse"))
        }
        return list
    }

    fun playCallRingtone(context: Context, customUriString: String? = null) {
        stopCallRingtone(context)
        try {
            val uri = if (!customUriString.isNullOrBlank() && customUriString != "default") {
                Uri.parse(customUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            var played = false
            try {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(android.media.AudioManager.STREAM_RING)
                        .build()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = true
                }
                ringtone?.play()
                activeRingtone = ringtone
                played = ringtone != null && ringtone.isPlaying
            } catch (e: Exception) {
                android.util.Log.w(TAG, "RingtoneManager play failed: ${e.message}")
            }

            // Fallback to MediaPlayer if RingtoneManager did not start
            if (!played && uri != null) {
                try {
                    val mp = android.media.MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setLegacyStreamType(android.media.AudioManager.STREAM_RING)
                                .build()
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
                    activeMediaPlayer = mp
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "MediaPlayer fallback failed: ${e.message}")
                }
            }

            // Trigger continuous incoming call vibration pattern (repeating)
            vibrateRepeating(context, longArrayOf(0, 1000, 800, 1000, 800))
        } catch (_: Exception) {}
    }

    fun playNotificationSound(context: Context, customUriString: String? = null) {
        try {
            val uri = if (!customUriString.isNullOrBlank() && customUriString != "default") {
                Uri.parse(customUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
            vibrateOnce(context, longArrayOf(0, 180, 120, 220))
        } catch (_: Exception) {}
    }

    fun stopCallRingtone(context: Context) {
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (_: Exception) {}
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (_: Exception) {}
        try {
            stopVibration(context)
        } catch (_: Exception) {}
    }

    private fun vibrateRepeating(context: Context, pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (_: Exception) {}
    }

    private fun vibrateOnce(context: Context, pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (_: Exception) {}
    }
}
