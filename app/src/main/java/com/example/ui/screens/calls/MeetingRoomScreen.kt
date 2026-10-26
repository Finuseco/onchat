package com.example.ui.screens.calls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActiveMeetingSession
import com.example.data.model.MeetingBillingDto
import com.example.data.model.MeetingParticipantDto
import com.example.ui.components.AvatarBadge
import com.example.ui.components.CameraPreview
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingRoomScreen(
    session: ActiveMeetingSession,
    viewModel: OnChatViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showParticipantsSheet by remember { mutableStateOf(false) }
    var showBillingDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showEndConfirmDialog by remember { mutableStateOf(false) }

    // Intercepter retour arrière matériel pour quitter proprement
    BackHandler {
        viewModel.leaveMeeting()
        onClose()
    }

    // Minuteur de durée
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session.meetCode) {
        val start = session.joinedAt
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000
            delay(1000)
        }
    }

    // Rafraîchir les participants périodiquement (toutes les 5 secondes)
    LaunchedEffect(session.meetCode) {
        while (true) {
            delay(5000)
            viewModel.refreshMeetingParticipants()
        }
    }

    val elapsedFormatted = remember(elapsedSeconds) {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("meeting_room_screen"),
        containerColor = Color(0xFF090E17),
        topBar = {
            MeetingTopBar(
                session = session,
                durationFormatted = elapsedFormatted,
                onShare = { showShareDialog = true },
                onBilling = { showBillingDialog = true },
                onShowParticipants = { showParticipantsSheet = true },
                onLeave = {
                    viewModel.leaveMeeting()
                    onClose()
                }
            )
        },
        bottomBar = {
            MeetingBottomControls(
                session = session,
                onToggleMic = { viewModel.toggleMeetingMic() },
                onToggleCamera = { viewModel.toggleMeetingCamera() },
                onFlipCamera = { viewModel.toggleMeetingCameraFront() },
                onToggleRaiseHand = { viewModel.toggleMeetingRaiseHand() },
                onOpenParticipants = { showParticipantsSheet = true },
                onEndMeeting = {
                    if (session.isHost) {
                        showEndConfirmDialog = true
                    } else {
                        viewModel.leaveMeeting()
                        onClose()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Bannière notification si main levée par des participants (pour l'hôte)
            val handsRaisedCount = remember(session.participants) {
                session.participants.count { it.raiseHand && !it.isHost }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (session.isHost && handsRaisedCount > 0) {
                    HandsRaisedAlertBanner(
                        count = handsRaisedCount,
                        onClick = { showParticipantsSheet = true }
                    )
                }

                // Grille vidéo/audio des participants
                ParticipantsGrid(
                    session = session,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    // 1. Feuille des participants & gestion de la parole
    if (showParticipantsSheet) {
        ParticipantsModalBottomSheet(
            session = session,
            onDismiss = { showParticipantsSheet = false },
            onGrantSpeech = { userId, canSpeak ->
                viewModel.grantOrRevokeSpeech(userId, canSpeak)
            }
        )
    }

    // 2. Dialog Facturation (GET & POST /meetings/{meetCode}/billing)
    if (showBillingDialog) {
        MeetingBillingDialog(
            session = session,
            onDismiss = { showBillingDialog = false },
            onUpdateBilling = { amount, currency ->
                viewModel.updateMeetingBilling(amount, currency)
                showBillingDialog = false
            }
        )
    }

    // 3. Dialog Partage (Code & Lien de réunion)
    if (showShareDialog) {
        MeetingShareDialog(
            session = session,
            onDismiss = { showShareDialog = false }
        )
    }

    // 4. Confirmation de fin de réunion (Hôte uniquement)
    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            title = {
                Text(
                    text = "Terminer la réunion ?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "En tant qu'hôte, cette action mettra fin à la réunion pour l'ensemble des participants connectés.",
                    color = CashPayGrey,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndConfirmDialog = false
                        viewModel.endMeeting()
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF334B)),
                    modifier = Modifier.testTag("confirm_end_meeting_button")
                ) {
                    Text("Terminer pour tous", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text("Annuler", color = CashPayGrey)
                }
            },
            containerColor = CashPayNavySurface
        )
    }
}

@Composable
fun MeetingTopBar(
    session: ActiveMeetingSession,
    durationFormatted: String,
    onShare: () -> Unit,
    onBilling: () -> Unit,
    onShowParticipants: () -> Unit,
    onLeave: () -> Unit
) {
    Surface(
        color = CashPayNavySurface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton quitter
            IconButton(
                onClick = onLeave,
                modifier = Modifier.testTag("leave_meeting_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitter la réunion",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Titre & Code de réunion
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (session.type == "audio") OnChatCyan.copy(alpha = 0.2f) else OnChatViolet.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (session.type == "audio") "AUDIO" else "VIDÉO",
                            color = if (session.type == "audio") OnChatCyan else OnChatVioletLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.topic,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Code : ${session.meetCode}",
                        color = OnChatCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $durationFormatted",
                        color = CashPayGrey,
                        fontSize = 11.sp
                    )
                }
            }

            // Bouton Partager
            IconButton(onClick = onShare, modifier = Modifier.testTag("share_meeting_button")) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Partager",
                    tint = OnChatCyan
                )
            }

            // Bouton Facturation
            IconButton(onClick = onBilling, modifier = Modifier.testTag("billing_meeting_button")) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "Facturation",
                    tint = Color(0xFFF59E0B)
                )
            }

            // Bouton Participants Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CashPayNavyDark)
                    .clickable { onShowParticipants() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Participants",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${session.participants.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HandsRaisedAlertBanner(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count participant(s) demandent la parole",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Gérer",
                color = Color(0xFFF59E0B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ParticipantsGrid(
    session: ActiveMeetingSession,
    modifier: Modifier = Modifier
) {
    val participants = session.participants.ifEmpty {
        listOf(
            MeetingParticipantDto(
                userId = 101L,
                name = "Vous (Hôte)",
                isHost = session.isHost,
                role = if (session.isHost) "host" else "viewer",
                canSpeak = session.canSpeak,
                raiseHand = session.isHandRaised
            )
        )
    }

    val columns = if (participants.size <= 2) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(participants) { participant ->
            val isLocalUser = participant.isHost && session.isHost
            ParticipantTile(
                participant = participant,
                isLocal = isLocalUser,
                isVideoMeeting = session.type != "audio",
                isCameraOn = if (isLocalUser) session.isCameraOn else true,
                isCameraFront = session.isCameraFront,
                isMuted = if (isLocalUser) session.isMicMuted else !participant.canSpeak
            )
        }
    }
}

@Composable
fun ParticipantTile(
    participant: MeetingParticipantDto,
    isLocal: Boolean,
    isVideoMeeting: Boolean,
    isCameraOn: Boolean,
    isCameraFront: Boolean,
    isMuted: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isVideoMeeting) 220.dp else 140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = if (participant.raiseHand) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder)
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isVideoMeeting && isLocal && isCameraOn) {
                // Flux Caméra réel pour l'utilisateur local
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    useFrontCamera = isCameraFront
                )
            } else {
                // Fond stylisé avec Avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(CashPayNavyDark, Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarBadge(
                            name = participant.name,
                            avatarUrl = participant.avatar,
                            size = if (isVideoMeeting) 64.dp else 52.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!isMuted) {
                            PulsingAudioWaves()
                        }
                    }
                }
            }

            // Badge Hôte / Rôle en haut à gauche
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (participant.isHost) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OnChatViolet
                    ) {
                        Text(
                            text = "Hôte",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (participant.role) {
                            "speaker" -> OnChatCyan.copy(alpha = 0.3f)
                            else -> Color.Black.copy(alpha = 0.5f)
                        }
                    ) {
                        Text(
                            text = when (participant.role) {
                                "speaker" -> "Intervenant"
                                else -> "Spectateur"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (participant.raiseHand) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = "Main levée",
                                tint = Color.Black,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Demande", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Statut Micro en haut à droite
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) Color(0xFFFF334B) else Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Nom du participant en bas
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = participant.name + if (isLocal) " (Moi)" else "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PulsingAudioWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((8 * (index + 1) * scale).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OnChatCyan)
            )
        }
    }
}

@Composable
fun MeetingBottomControls(
    session: ActiveMeetingSession,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleRaiseHand: () -> Unit,
    onOpenParticipants: () -> Unit,
    onEndMeeting: () -> Unit
) {
    Surface(
        color = CashPayNavySurface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Bouton Micro
            ControlButton(
                icon = if (session.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (session.isMicMuted) "Muet" else "Micro",
                isActive = !session.isMicMuted,
                activeColor = OnChatCyan,
                testTag = "toggle_meeting_mic_button",
                onClick = onToggleMic
            )

            // 2. Bouton Caméra (si réunion vidéo)
            if (session.type != "audio") {
                ControlButton(
                    icon = if (session.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (session.isCameraOn) "Vidéo" else "Arrêtée",
                    isActive = session.isCameraOn,
                    activeColor = OnChatViolet,
                    testTag = "toggle_meeting_camera_button",
                    onClick = onToggleCamera
                )

                // 3. Bouton Retourner Caméra
                ControlButton(
                    icon = Icons.Default.FlipCameraAndroid,
                    label = "Inverser",
                    isActive = false,
                    testTag = "flip_meeting_camera_button",
                    onClick = onFlipCamera
                )
            }

            // 4. Bouton Lever la main (Demander la parole)
            ControlButton(
                icon = Icons.Default.PanTool,
                label = if (session.isHandRaised) "Baisser" else "Lever",
                isActive = session.isHandRaised,
                activeColor = Color(0xFFF59E0B),
                testTag = "raise_hand_button",
                onClick = onToggleRaiseHand
            )

            // 5. Bouton Raccrocher / Terminer
            ControlButton(
                icon = Icons.Default.CallEnd,
                label = if (session.isHost) "Terminer" else "Quitter",
                isActive = true,
                activeColor = Color(0xFFFF334B),
                testTag = "end_or_leave_button",
                onClick = onEndMeeting
            )
        }
    }
}

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = OnChatCyan,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else CashPayNavyDark)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else CashPayGrey,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) Color.White else CashPayGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// BottomSheet: Participants & Speech Rights
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsModalBottomSheet(
    session: ActiveMeetingSession,
    onDismiss: () -> Unit,
    onGrantSpeech: (userId: Long, canSpeak: Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CashPayNavySurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CashPayNavyBorder) },
        modifier = Modifier.testTag("participants_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Participants (${session.participants.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (session.isHost) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OnChatViolet.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Mode Hôte actif",
                            color = OnChatVioletLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = if (session.isHost) {
                    "En tant qu'hôte, vous pouvez accorder ou retirer le droit de parole aux participants."
                } else {
                    "Liste des personnes actuellement connectées à la réunion."
                },
                color = CashPayGrey,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Divider(color = CashPayNavyBorder, thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(session.participants) { participant ->
                    ParticipantRowItem(
                        participant = participant,
                        isCurrentHost = session.isHost,
                        onGrantSpeech = { canSpeak ->
                            onGrantSpeech(participant.userId, canSpeak)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ParticipantRowItem(
    participant: MeetingParticipantDto,
    isCurrentHost: Boolean,
    onGrantSpeech: (canSpeak: Boolean) -> Unit
) {
    Surface(
        color = CashPayNavyDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (participant.raiseHand) Color(0xFFF59E0B).copy(alpha = 0.5f) else CashPayNavyBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(name = participant.name, avatarUrl = participant.avatar, size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (participant.isHost) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Hôte",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rôle : ${participant.role}",
                        color = when (participant.role) {
                            "host" -> Color(0xFFF59E0B)
                            "speaker" -> OnChatCyan
                            else -> CashPayGrey
                        },
                        fontSize = 11.sp
                    )
                    if (participant.raiseHand) {
                        Text(
                            text = " • ✋ Main levée",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bouton Accorder/Retirer parole (si l'utilisateur connecté est hôte et la cible n'est pas hôte)
            if (isCurrentHost && !participant.isHost) {
                if (participant.canSpeak) {
                    OutlinedButton(
                        onClick = { onGrantSpeech(false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Couper parole", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onGrantSpeech(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Donner parole", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// Dialog: Facturation d'une réunion (GET & POST /meetings/{meetCode}/billing)
// ==========================================

@Composable
fun MeetingBillingDialog(
    session: ActiveMeetingSession,
    onDismiss: () -> Unit,
    onUpdateBilling: (amount: Double, currency: String) -> Unit
) {
    val currentBilling = session.billing ?: MeetingBillingDto()
    var priceText by remember { mutableStateOf(currentBilling.priceAmount.toString()) }
    var selectedCurrency by remember { mutableStateOf(currentBilling.priceCurrency) }
    var isEditMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Facturation Réunion",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CashPayNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Code Réunion", color = CashPayGrey, fontSize = 12.sp)
                            Text(session.meetCode, color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tarif d'accès", color = CashPayGrey, fontSize = 12.sp)
                            Text(
                                text = if (currentBilling.priceAmount > 0) {
                                    "${currentBilling.priceAmount} ${currentBilling.priceCurrency}"
                                } else {
                                    "Gratuit (0.00 USD)"
                                },
                                color = if (currentBilling.priceAmount > 0) Color(0xFFF59E0B) else SuccessGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Statut du paiement", color = CashPayGrey, fontSize = 12.sp)
                            Text(
                                text = if (currentBilling.isPaid) "Payée / Billeterie" else "Non payée",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Si l'utilisateur est hôte, autoriser la modification via POST /meetings/{meetCode}/billing
                if (session.isHost) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!isEditMode) {
                        Button(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CashPayNavyDark),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modifier le tarif (Hôte)", color = Color.White, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Montant ($selectedCurrency)", color = CashPayGrey) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                cursorColor = Color(0xFFF59E0B)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("USD", "CDF").forEach { curr ->
                                val selected = selectedCurrency == curr
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) Color(0xFFF59E0B) else CashPayNavyDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCurrency = curr }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = curr,
                                        color = if (selected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val amount = priceText.toDoubleOrNull() ?: 0.0
                                onUpdateBilling(amount, selectedCurrency)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enregistrer les modifications", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Dialog: Partage de Code et Lien
// ==========================================

@Composable
fun MeetingShareDialog(
    session: ActiveMeetingSession,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val joinUrl = session.joinUrl.ifBlank { "https://app.cashpay-all.com/feedback/meet/${session.meetCode}" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inviter des participants",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Partagez le code ou le lien ci-dessous pour permettre à vos contacts de rejoindre la réunion.",
                    color = CashPayGrey,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bloc Code
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CashPayNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Code de réunion", color = CashPayGrey, fontSize = 10.sp)
                            Text(session.meetCode, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code Réunion", session.meetCode))
                            Toast.makeText(context, "Code copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = OnChatCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bloc Lien
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CashPayNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lien d'invitation", color = CashPayGrey, fontSize = 10.sp)
                            Text(
                                text = joinUrl,
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Lien Réunion", joinUrl))
                            Toast.makeText(context, "Lien copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Link, contentDescription = "Copier lien", tint = OnChatCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bouton Partage Système Android
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Rejoignez ma réunion OnChat / CashPay All !\nSujet : ${session.topic}\nCode : ${session.meetCode}\nLien direct : $joinUrl"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Partager la réunion")
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partager via d'autres applications", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
