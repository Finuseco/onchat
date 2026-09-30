package com.example.ui.screens.chats

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VoiceMessageBubble(
    message: Message,
    timeFormatted: String
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentSeconds by remember { mutableStateOf(0) }
    val totalSeconds = remember(message.content) {
        // extract duration if encoded in text (e.g., "Note vocale (0:10)")
        if (message.content.contains("0:")) {
            val sec = message.content.substringAfter("0:").take(2).toIntOrNull() ?: 10
            sec
        } else {
            10
        }
    }

    // Interactive audio simulation timer
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (currentSeconds < totalSeconds) {
                delay(1000)
                currentSeconds += 1
            }
            isPlaying = false
            currentSeconds = 0
        }
    }

    // Heights for 28 waveform bars
    val waveformBars = remember {
        listOf(
            4, 7, 12, 18, 14, 22, 26, 16, 20, 24, 15, 10, 18, 22, 25, 19, 14, 16, 20, 24, 18, 12, 15, 9, 6, 4, 3, 3
        )
    }

    // Container matching Screenshot 1 (Telegram style rounded voice bubble with OnChat gradient)
    Box(
        modifier = Modifier
            .widthIn(min = 230.dp, max = 290.dp)
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
                        .clickable {
                            isPlaying = !isPlaying
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Lire",
                        tint = Color(0xFF1E88E5),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Waveform Bars & Duration
                Column(modifier = Modifier.weight(1f)) {
                    // Visual Waveform
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        waveformBars.forEachIndexed { index, height ->
                            val progressRatio = currentSeconds.toFloat() / totalSeconds.toFloat()
                            val barRatio = index.toFloat() / waveformBars.size.toFloat()
                            val isPlayed = isPlaying && barRatio <= progressRatio

                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(height.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        if (isPlayed) Color.White
                                        else Color.White.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Duration display (e.g. 00:10 •)
                    val displayTime = if (isPlaying) {
                        String.format("00:%02d", currentSeconds)
                    } else {
                        String.format("00:%02d", totalSeconds)
                    }

                    Text(
                        text = "$displayTime •",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Timestamp and checkmarks bottom right
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
                if (message.isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Délivré",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
