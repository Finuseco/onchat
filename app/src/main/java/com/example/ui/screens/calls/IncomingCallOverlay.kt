package com.example.ui.screens.calls

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncomingCallNotificationState
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import com.example.util.RingtoneHelper

@Composable
fun IncomingCallOverlay(
    callState: IncomingCallNotificationState,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current

    // Play ringtone on composition and stop on leave
    DisposableEffect(callState.callId) {
        RingtoneHelper.playCallRingtone(context)
        onDispose {
            RingtoneHelper.stopCallRingtone(context)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Top incoming call type header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CashPayNavySurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (callState.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = OnChatCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (callState.isVideo) "Appel Vidéo Entrant..." else "Appel Audio Entrant...",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center caller identity & pulse rings
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((140 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(OnChatViolet.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .size((120 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(OnChatCyan.copy(alpha = 0.25f))
                )
                AvatarBadge(name = callState.callerName, avatarUrl = callState.callerAvatar, size = 110.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callState.callerName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CashPayNavySurface,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "ID Wallet : ${callState.callerWalletId}",
                    color = OnChatCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sonnerie du téléphone en cours...",
                color = CashPayGrey,
                fontSize = 13.sp
            )
        }

        // Bottom Action buttons (Decline Red / Accept Green)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color.Red)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            RingtoneHelper.stopCallRingtone(context)
                            onDecline()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Refuser",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Refuser", color = CashPayGrey, fontSize = 13.sp)
            }

            // Accept Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(8.dp, CircleShape, spotColor = SuccessGreen)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            RingtoneHelper.stopCallRingtone(context)
                            onAccept()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (callState.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Répondre",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Répondre", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
