package com.example.ui.screens.chats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.ui.theme.*
import kotlinx.coroutines.isActive

@Composable
fun MeetingInviteBubble(
    message: Message,
    meetCode: String,
    timeFormatted: String,
    onJoinMeeting: (String) -> Unit
) {
    val context = LocalContext.current
    val manager = remember { com.example.data.api.OnChatWebSocketManager.getInstance() }
    val states by manager.meetingStates.collectAsState()
    var fallback by remember(meetCode) { mutableStateOf<com.example.data.model.MeetingStatusResponse?>(null) }
    val connection by manager.connectionStatus.collectAsState()
    val state = if (fallback?.status == "ended") fallback else states[meetCode]?.takeIf { connection == com.example.data.api.WsConnectionStatus.CONNECTED } ?: fallback
    DisposableEffect(meetCode) { manager.watchMeeting(meetCode); onDispose { manager.unwatchMeeting(meetCode) } }
    LaunchedEffect(meetCode) {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            try { val response = com.example.data.api.ApiClient.apiService.getMeetingStatus(meetCode); if (response.isSuccessful && response.body()?.success == true) fallback = response.body() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { }
            kotlinx.coroutines.delay(15000)
        }
    }
    val isVideo = message.content.contains("vidéo", ignoreCase = true) ||
            message.content.contains("video", ignoreCase = true) ||
            message.content.contains("🎥")
    val isAudio = !isVideo && (message.content.contains("audio", ignoreCase = true) || message.content.contains("📞"))

    val title = when {
        isVideo -> "Appel vidéo CashPay"
        isAudio -> "Appel audio CashPay"
        else -> "Réunion en direct OnChat"
    }

    // Pulsing live badge animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface.copy(alpha = 0.95f)),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                if (isVideo) listOf(OnChatViolet, OnChatCyan)
                else listOf(Color(0xFF25D366), OnChatCyan)
            )
        ),
        modifier = Modifier
            .widthIn(min = 260.dp, max = 310.dp)
            .padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Icon & Live Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVideo) OnChatViolet.copy(alpha = 0.2f)
                                else Color(0xFF25D366).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = title,
                            tint = if (isVideo) OnChatCyan else Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Session interactive",
                            color = CashPayGrey,
                            fontSize = 11.sp
                        )
                    }
                }

                // Pulsing Live Indicator
                Surface(
                    color = Color(0xFFFF5252).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = alphaAnim))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252).copy(alpha = alphaAnim))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (state?.status) { "active" -> "EN DIRECT"; "ended" -> "TERMINÉE"; else -> "À VÉRIFIER" },
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meeting Code Box with Copy Button
            Surface(
                color = CashPayNavyDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CashPayNavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CODE RÉUNION",
                            color = CashPayGrey,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = meetCode,
                            color = OnChatCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Code Réunion", meetCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copié : $meetCode", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier le code",
                            tint = CashPayGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Action: "Rejoindre l'appel / la réunion" Button
            Button(
                onClick = { onJoinMeeting(meetCode) },
                enabled = state?.status != "ended",
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVideo) OnChatCyan else Color(0xFF25D366)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = if (isVideo) CashPayNavyDark else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voir les conditions et rejoindre",
                        color = if (isVideo) CashPayNavyDark else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isVideo) CashPayNavyDark else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Timestamp & Delivery Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lien officiel CashPay",
                    color = CashPayGrey,
                    fontSize = 11.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormatted,
                        color = CashPayGrey,
                        fontSize = 11.sp
                    )
                    if (message.isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Délivré",
                            tint = OnChatCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
