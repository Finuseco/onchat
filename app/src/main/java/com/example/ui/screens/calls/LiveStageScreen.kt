package com.example.ui.screens.calls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveStageRoom
import com.example.data.model.StageComment
import com.example.data.model.StageUser
import com.example.ui.components.AvatarBadge
import com.example.ui.components.CameraPreview
import com.example.ui.components.QrCodeModal
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStageScreen(
    room: LiveStageRoom,
    viewModel: OnChatViewModel,
    onLeave: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeContacts by viewModel.activeContacts.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var showSpeakersSheet by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Floating heart particle animation state
    var heartCount by remember { mutableIntStateOf(room.likesCount) }
    val floatingHearts = remember { mutableStateListOf<Float>() }

    // Live elapsed timer
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(room.id) {
        val start = room.startedAt
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000
            delay(1000)
        }
    }

    // Auto-scroll comments to bottom
    LaunchedEffect(room.comments.size) {
        if (room.comments.isNotEmpty()) {
            listState.animateScrollToItem(room.comments.size - 1)
        }
    }

    val elapsedFormatted = remember(elapsedSeconds) {
        val m = (elapsedSeconds / 60)
        val s = (elapsedSeconds % 60)
        String.format("%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- 1. Main Stage (Audio Conference or Video Stage) ---
        if (room.isAudioOnly) {
            AudioConferenceStage(room = room, viewModel = viewModel)
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (room.speakerOnStageName != null) {
                    // Dual Screen Stage (Host + Speaker on stage)
                    if (room.isCameraLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            ) {
                                StageVideoCell(
                                    name = room.hostName,
                                    avatarUrl = room.hostAvatar,
                                    role = "Hôte",
                                    isHost = true,
                                    isMicMuted = room.isMicMuted,
                                    isVideoMuted = room.isVideoMuted,
                                    isFrontCamera = room.isCameraFront
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                            ) {
                                StageVideoCell(
                                    name = room.speakerOnStageName ?: "Intervenant",
                                    avatarUrl = room.speakerOnStageAvatar,
                                    role = "Intervenant sur scène",
                                    isHost = false,
                                    isMicMuted = false,
                                    isVideoMuted = false,
                                    isFrontCamera = true,
                                    onDemote = if (room.isHost) {
                                        { viewModel.demoteSpeakerFromStage(room.id) }
                                    } else null
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(bottom = 2.dp)
                            ) {
                                StageVideoCell(
                                    name = room.hostName,
                                    avatarUrl = room.hostAvatar,
                                    role = "Hôte",
                                    isHost = true,
                                    isMicMuted = room.isMicMuted,
                                    isVideoMuted = room.isVideoMuted,
                                    isFrontCamera = room.isCameraFront
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 2.dp)
                            ) {
                                StageVideoCell(
                                    name = room.speakerOnStageName ?: "Intervenant",
                                    avatarUrl = room.speakerOnStageAvatar,
                                    role = "Intervenant sur scène",
                                    isHost = false,
                                    isMicMuted = false,
                                    isVideoMuted = false,
                                    isFrontCamera = true,
                                    onDemote = if (room.isHost) {
                                        { viewModel.demoteSpeakerFromStage(room.id) }
                                    } else null
                                )
                            }
                        }
                    }
                } else {
                    // Full Screen Host Stage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        StageVideoCell(
                            name = room.hostName,
                            avatarUrl = room.hostAvatar,
                            role = "Hôte de la conférence",
                            isHost = true,
                            isMicMuted = room.isMicMuted,
                            isVideoMuted = room.isVideoMuted,
                            isFrontCamera = room.isCameraFront
                        )
                    }
                }
            }
        }

        // --- 2. Top Header Overlay ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Host Info & Live Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                AvatarBadge(name = room.hostName, avatarUrl = room.hostAvatar, size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = room.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF334B))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE $elapsedFormatted",
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Spectator count & VIP badge & Actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Viewers badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${room.spectatorCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Paid conference badge
                if (room.isPaid) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${room.entryFee} ${room.currency}",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Share Button
                IconButton(
                    onClick = { showShareModal = true },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Leave / End button
                IconButton(
                    onClick = {
                        if (room.isHost) {
                            viewModel.endLiveRoom(room.id)
                        } else {
                            viewModel.leaveLiveRoom(room.id)
                        }
                        onLeave()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFE11D48), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Quitter", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // --- 3. Floating Comments Stream Overlay (Bottom-Left) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.38f)
                .padding(start = 12.dp, bottom = 74.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(room.comments, key = { it.id }) { c ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, CashPayNavyBorder.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBadge(name = c.senderName, avatarUrl = c.avatarUrl, size = 20.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${c.senderName}: ",
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = c.comment,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Floating Animated Hearts ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
                .width(50.dp)
                .height(200.dp)
        ) {
            floatingHearts.forEach { _ ->
                FloatingHeartParticle()
            }
        }

        // --- 5. Bottom Interactive Controls Bar ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Comment text input
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Commenter en direct...", color = CashPayGrey, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = OnChatCyan,
                    unfocusedBorderColor = CashPayNavyBorder,
                    cursorColor = OnChatCyan,
                    focusedContainerColor = Color.Black.copy(alpha = 0.7f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commentText.isNotBlank()) {
                        viewModel.sendLiveComment(room.id, commentText.trim())
                        commentText = ""
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            // Send comment button
            if (commentText.isNotBlank()) {
                IconButton(
                    onClick = {
                        viewModel.sendLiveComment(room.id, commentText.trim())
                        commentText = ""
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(OnChatViolet, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Raise hand / Manage stage button
            if (room.isHost) {
                // Host: Open Speaker Queue
                Box {
                    IconButton(
                        onClick = { showSpeakersSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (room.raisedHandRequests.isNotEmpty()) Color(0xFFF59E0B) else CashPayNavySurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Gérer les speakers",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (room.raisedHandRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${room.raisedHandRequests.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Spectator: Raise hand button
                IconButton(
                    onClick = {
                        viewModel.toggleRaiseHand(room.id)
                        val msg = if (room.hasRaisedHand) "Demande de parole annulée" else "Demande de parole envoyée à l'hôte"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (room.hasRaisedHand) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Lever la main",
                        tint = if (room.hasRaisedHand) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Likes / Hearts button
            IconButton(
                onClick = {
                    heartCount++
                    floatingHearts.add((0..100).random().toFloat())
                    viewModel.sendLiveLike(room.id)
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE11D48), CircleShape)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Aimer", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // Invite contacts button
            if (room.isHost) {
                IconButton(
                    onClick = { showSpeakersSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(OnChatViolet, CircleShape)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Inviter des contacts", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Camera toggle & Orientation controls (if Host and Video mode)
            if (room.isHost && !room.isAudioOnly) {
                IconButton(
                    onClick = { viewModel.toggleLiveOrientation(room.id) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CashPayNavySurface, CircleShape)
                ) {
                    Icon(
                        imageVector = if (room.isCameraLandscape) Icons.Default.StayCurrentPortrait else Icons.Default.StayCurrentLandscape,
                        contentDescription = "Rotation",
                        tint = OnChatCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleLiveCameraFlip(room.id) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CashPayNavySurface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Changer Caméra",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // --- 6. Host Manage Speakers & Invite Contacts Bottom Sheet ---
    if (showSpeakersSheet && room.isHost) {
        ModalBottomSheet(
            onDismissRequest = { showSpeakersSheet = false },
            containerColor = CashPayNavySurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Inviter & Gérer les Intervenants",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Invitez vos contacts actifs OnChat ou donnez la parole aux spectateurs.",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // 1. Inviter des contacts actifs
                Text(
                    text = "INVITER DES CONTACTS ACTIFS (ONCHAT)",
                    color = OnChatCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (activeContacts.isEmpty()) {
                    Text(
                        text = "Aucun contact actif trouvé pour l'instant.",
                        color = CashPayGrey,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(activeContacts) { contact ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarBadge(name = contact.name ?: contact.phone, avatarUrl = contact.avatar, size = 32.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(contact.name ?: "Contact", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            if (!contact.walletId.isNullOrBlank()) {
                                                Text(contact.walletId, color = OnChatCyan, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.inviteContactToLiveRoom(room.id, contact)
                                            Toast.makeText(context, "Invitation envoyée à ${contact.name ?: contact.walletId}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Inviter", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current speaker on stage
                if (room.speakerOnStageName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CashPayNavyDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarBadge(name = room.speakerOnStageName ?: "", avatarUrl = room.speakerOnStageAvatar, size = 40.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = room.speakerOnStageName ?: "",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Actuellement sur la piste",
                                        color = SuccessGreen,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.demoteSpeakerFromStage(room.id)
                                    showSpeakersSheet = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A1E1E)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Rétrograder", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "DEMANDES DE PAROLE (${room.raisedHandRequests.size})",
                    color = CashPayGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (room.raisedHandRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun spectateur n'a levé la main pour le moment.",
                            color = CashPayGrey,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(room.raisedHandRequests) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarBadge(name = user.name, avatarUrl = user.avatarUrl, size = 36.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(user.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text(user.walletId, color = OnChatCyan, fontSize = 11.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.inviteUserToStage(room.id, user.walletId, user.name, user.avatarUrl)
                                            showSpeakersSheet = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Faire monter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 7. Share Modal with QR & Link ---
    if (showShareModal) {
        val shareLink = "https://app.cashpay-all.com/live/${room.id}"
        QrCodeModal(
            title = "Partager la Conférence Live",
            subtitle = "Invitez vos contacts et abonnés à suivre votre direct.",
            qrData = shareLink,
            walletId = room.hostWalletId,
            onDismiss = { showShareModal = false }
        )
    }
}

@Composable
fun StageVideoCell(
    name: String,
    avatarUrl: String?,
    role: String,
    isHost: Boolean,
    isMicMuted: Boolean,
    isVideoMuted: Boolean,
    isFrontCamera: Boolean,
    onDemote: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, CashPayNavyBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!isVideoMuted) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                useFrontCamera = isFrontCamera
            )

            // Overlaid Name pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Center avatar + audio visualizer ripple
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    AvatarBadge(name = name, avatarUrl = avatarUrl, size = 96.dp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role,
                    color = if (isHost) OnChatCyan else SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom cell badges (Mic, Demote)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isMicMuted) Color.Red else SuccessGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMicMuted) "Muet" else "Micro actif",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            if (onDemote != null) {
                IconButton(
                    onClick = onDemote,
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = "Rétrograder",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingHeartParticle() {
    val infiniteTransition = rememberInfiniteTransition(label = "heart")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -180f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = Color(0xFFFF334B).copy(alpha = alpha),
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(24.dp)
    )
}

@Composable
fun AudioConferenceStage(
    room: LiveStageRoom,
    viewModel: OnChatViewModel
) {
    val speakers = remember(room) {
        val list = mutableListOf<StageUser>()
        list.add(StageUser(walletId = room.hostWalletId, name = room.hostName, avatarUrl = room.hostAvatar, isSpeakerOnStage = true))
        if (room.speakerOnStageName != null) {
            list.add(StageUser(walletId = room.speakerOnStageWalletId ?: "", name = room.speakerOnStageName, avatarUrl = room.speakerOnStageAvatar, isSpeakerOnStage = true))
        }
        room.activeSpeakers.forEach { s ->
            if (!list.any { it.walletId == s.walletId }) {
                list.add(s)
            }
        }
        list.take(5)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
            .padding(top = 80.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = OnChatCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OnChatCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "CONFÉRENCE AUDIO • ${speakers.size}/5 SUR SCÈNE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = room.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Grid of up to 5 audio speakers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                speakers.forEach { speaker ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(OnChatViolet.copy(alpha = 0.3f))
                                .border(
                                    2.dp,
                                    if (speaker.walletId == room.hostWalletId) Color(0xFFF59E0B) else OnChatCyan,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarBadge(name = speaker.name, avatarUrl = speaker.avatarUrl, size = 64.dp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = speaker.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (speaker.walletId == room.hostWalletId) "Hôte" else "Intervenant",
                            color = if (speaker.walletId == room.hostWalletId) Color(0xFFF59E0B) else OnChatCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CashPayNavySurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Voix en direct duplex via WebSocket • ${room.spectatorCount} participant(s)",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

