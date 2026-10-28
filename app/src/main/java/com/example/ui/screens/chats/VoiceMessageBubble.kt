package com.example.ui.screens.chats

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun VoiceMessageBubble(
    message: Message,
    timeFormatted: String
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember {
        mutableIntStateOf(
            if ((message.durationSeconds ?: 0) > 0) (message.durationSeconds!! * 1000)
            else 0
        )
    }
    var playerError by remember { mutableStateOf(false) }

    // Resolve URL for audio file (web upload, native upload, or local)
    val resolvedAudioUrl = remember(message.fileUrl, message.content) {
        val raw = message.fileUrl ?: if (message.content.startsWith("http")) message.content else null
        when {
            raw.isNullOrBlank() -> null
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("file://") || raw.startsWith("content://") -> raw
            raw.startsWith("/") -> "https://app.cashpay-all.com$raw"
            raw.contains("uploads/") -> "https://app.cashpay-all.com/${raw.substringAfter("uploads/")}"
            else -> "https://app.cashpay-all.com/$raw"
        }
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        isBuffering = false
    }

    DisposableEffect(message.id) {
        onDispose {
            releasePlayer()
        }
    }

    // Playback progress ticker
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val player = mediaPlayer
            if (player != null && player.isPlaying) {
                currentPositionMs = player.currentPosition
                val dur = player.duration
                if (dur > 0 && totalDurationMs <= 0) {
                    totalDurationMs = dur
                }
            }
            delay(100)
        }
    }

    fun togglePlayback() {
        if (resolvedAudioUrl.isNullOrBlank()) {
            playerError = true
            return
        }

        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            return
        }

        // Resume if already prepared
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.start()
                isPlaying = true
                return
            } catch (e: Exception) {
                releasePlayer()
            }
        }

        // Initialize and start MediaPlayer
        isBuffering = true
        playerError = false
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (resolvedAudioUrl.startsWith("http")) {
                    setDataSource(resolvedAudioUrl)
                } else if (resolvedAudioUrl.startsWith("file://") || resolvedAudioUrl.startsWith("/")) {
                    val filePath = resolvedAudioUrl.removePrefix("file://")
                    val file = File(filePath)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        setDataSource(context, Uri.parse(resolvedAudioUrl))
                    }
                } else {
                    setDataSource(context, Uri.parse(resolvedAudioUrl))
                }

                setOnPreparedListener { mp ->
                    isBuffering = false
                    val dur = mp.duration
                    if (dur > 0) totalDurationMs = dur
                    mp.start()
                    isPlaying = true
                }

                setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = 0
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("VoiceMessageBubble", "MediaPlayer error: what=$what extra=$extra url=$resolvedAudioUrl")
                    isBuffering = false
                    isPlaying = false
                    playerError = true
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("VoiceMessageBubble", "Failed to init player: ${e.message}")
            isBuffering = false
            isPlaying = false
            playerError = true
        }
    }

    // Heights for 28 waveform bars
    val waveformBars = remember {
        listOf(
            6, 10, 16, 22, 18, 26, 30, 20, 24, 28, 18, 12, 22, 26, 28, 22, 16, 20, 24, 28, 20, 14, 18, 12, 8, 6, 4, 4
        )
    }

    // Container matching Screenshot 1 (Telegram style rounded voice bubble with OnChat gradient)
    Box(
        modifier = Modifier
            .widthIn(min = 230.dp, max = 295.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isMine) 18.dp else 4.dp,
                    bottomEnd = if (message.isMine) 4.dp else 18.dp
                )
            )
            .then(
                if (message.isMine) Modifier.background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0)))
                )
                else Modifier.background(CashPayReceivedBubble)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { togglePlayback() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isBuffering -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF1E88E5),
                                strokeWidth = 2.dp
                            )
                        }
                        playerError -> {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Réessayer",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        isPlaying -> {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Lire",
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Waveform Bars & Duration
                Column(modifier = Modifier.weight(1f)) {
                    // Visual Interactive Waveform
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .pointerInput(totalDurationMs) {
                                detectTapGestures { offset ->
                                    if (totalDurationMs > 0 && mediaPlayer != null) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        val seekTarget = (fraction * totalDurationMs).toInt()
                                        try {
                                            mediaPlayer?.seekTo(seekTarget)
                                            currentPositionMs = seekTarget
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                    ) {
                        waveformBars.forEachIndexed { index, height ->
                            val progressRatio = if (totalDurationMs > 0) {
                                currentPositionMs.toFloat() / totalDurationMs.toFloat()
                            } else {
                                0f
                            }
                            val barRatio = index.toFloat() / waveformBars.size.toFloat()
                            val isPlayed = barRatio <= progressRatio

                            Box(
                                modifier = Modifier
                                    .width(2.8.dp)
                                    .height(height.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        if (isPlayed) Color.White
                                        else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Duration display (e.g. 00:24 •)
                    val displaySeconds = if (isPlaying || currentPositionMs > 0) {
                        currentPositionMs / 1000
                    } else if (totalDurationMs > 0) {
                        totalDurationMs / 1000
                    } else {
                        message.durationSeconds ?: 0
                    }

                    val mins = displaySeconds / 60
                    val secs = displaySeconds % 60
                    val displayTime = String.format(Locale.US, "%02d:%02d", mins, secs)

                    Text(
                        text = if (playerError) "Erreur de lecture" else "$displayTime •",
                        color = if (playerError) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Timestamp and read receipt ticks
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                if (message.isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Délivré",
                        tint = OnChatCyan,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
