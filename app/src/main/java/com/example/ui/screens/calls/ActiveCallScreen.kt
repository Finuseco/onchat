package com.example.ui.screens.calls

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AvatarBadge
import com.example.ui.components.CameraPreview
import com.example.ui.theme.*
import com.example.ui.viewmodel.CallSessionState
import com.example.ui.viewmodel.OnChatViewModel

@Composable
fun ActiveCallScreen(
    callState: CallSessionState,
    viewModel: OnChatViewModel
) {
    val durationFormatted = remember(callState.durationSeconds) {
        val minutes = callState.durationSeconds / 60
        val seconds = callState.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // Pulsing animation for calling / connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Voice frequency bars animation when in voice call
    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val waveHeight3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Video mode canvas or Audio mode avatar
        if (callState.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                // Background Video feed: CameraPreview
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    useFrontCamera = !callState.isCameraFacingBack
                )

                // Top peer info overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 70.dp, start = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarBadge(name = callState.peerName, avatarUrl = callState.peerAvatar, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = callState.peerName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (callState.peerWalletId.isNotBlank()) {
                                Text(
                                    text = "ID: ${callState.peerWalletId}",
                                    color = OnChatCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (callState.isConnected) "En direct • $durationFormatted" else "Sonnerie en cours...",
                                color = if (callState.isConnected) SuccessGreen else OnChatCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Picture-In-Picture Camera Indicator (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 70.dp, end = 16.dp)
                        .size(width = 90.dp, height = 130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(2.dp, OnChatCyan, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (callState.isCameraFacingBack) Icons.Default.CameraRear else Icons.Default.CameraFront,
                            contentDescription = null,
                            tint = OnChatCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (callState.isCameraFacingBack) "Cam Arrière" else "Cam Avant",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Audio call center layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!callState.isConnected) {
                            Box(
                                modifier = Modifier
                                    .size((140 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(OnChatVioletDeep.copy(alpha = 0.35f))
                            )
                            Box(
                                modifier = Modifier
                                    .size((120 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(OnChatCyan.copy(alpha = 0.25f))
                            )
                        }
                        AvatarBadge(name = callState.peerName, avatarUrl = callState.peerAvatar, size = 120.dp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = callState.peerName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (callState.peerWalletId.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CashPayNavySurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "ID Wallet : ${callState.peerWalletId}",
                                color = OnChatCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (callState.isConnected) "Connecté • $durationFormatted" else "Sonnerie en cours...",
                        color = if (callState.isConnected) SuccessGreen else OnChatCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Audio spectrum wave animation when connected
                    if (callState.isConnected) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(waveHeight1, waveHeight2, waveHeight3, waveHeight1 * 0.8f, waveHeight2 * 0.9f, waveHeight3 * 1.1f).forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(h.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (callState.isMuted) Color.Gray else OnChatCyan)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CashPayNavySurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (callState.isConnected) SuccessGreen else OnChatCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (callState.isConnected) "Appel Chiffré" else "Connexion...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (callState.isConnected) {
                Text(
                    text = durationFormatted,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Button
                CallControlButton(
                    icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    isActive = callState.isMuted,
                    activeColor = Color(0xFFEF4444),
                    defaultColor = CashPayNavySurface,
                    onClick = { viewModel.toggleCallMute() }
                )

                // Speaker Button
                CallControlButton(
                    icon = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    isActive = callState.isSpeakerOn,
                    activeColor = OnChatCyan,
                    defaultColor = CashPayNavySurface,
                    onClick = { viewModel.toggleCallSpeaker() }
                )

                // Flip camera if video
                if (callState.isVideo) {
                    CallControlButton(
                        icon = Icons.Default.Cameraswitch,
                        isActive = false,
                        activeColor = OnChatCyan,
                        defaultColor = CashPayNavySurface,
                        onClick = { viewModel.toggleCameraDirection() }
                    )
                }

                // End Call Red Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                        .clickable { viewModel.endCall() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Raccrocher",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    defaultColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else defaultColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
