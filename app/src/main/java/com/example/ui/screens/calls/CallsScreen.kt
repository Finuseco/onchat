package com.example.ui.screens.calls

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CallRecord
import com.example.data.model.LiveStageRoom
import com.example.ui.components.AvatarBadge
import com.example.ui.components.SkeletonCallRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallsScreen(
    viewModel: OnChatViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val calls by viewModel.calls.collectAsState()
    val liveRooms by viewModel.liveRoomsFeed.collectAsState()
    val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsState()

    var showNewLiveModal by remember { mutableStateOf(false) }
    var pendingPaidRoom by remember { mutableStateOf<LiveStageRoom?>(null) }
    var showDirectCallDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
    ) {
        if (isSkeletonLoading && calls.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(6) {
                    SkeletonCallRow()
                    Divider(color = CashPayNavyBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // --- 1. Top Live Stage Banner & Quick Start ---
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNewLiveModal = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(brush = OnChatPrimaryGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Lancer un Direct / Live Stage",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Conférence vidéo, double écran & accès payant",
                                    color = CashPayGrey,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = OnChatCyan
                            )
                        }
                    }
                }

                // --- 2. Live Streams Feeds (Followed Leaders & Active Stages) ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF334B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONFÉRENCES & LIVES EN COURS",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${liveRooms.size} actif(s)",
                            color = OnChatCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (liveRooms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune conférence en direct pour le moment.",
                                color = CashPayGrey,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(liveRooms) { room ->
                                LiveRoomCard(
                                    room = room,
                                    onClick = {
                                        if (room.isPaid && !room.isHost) {
                                            pendingPaidRoom = room
                                        } else {
                                            viewModel.joinLiveRoom(room)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // --- 3. Calls History Header ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HISTORIQUE DES APPELS RÉCENTS",
                            color = CashPayGrey,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- 4. Call History List ---
                if (calls.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun appel dans l'historique",
                                color = CashPayGrey,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(calls) { call ->
                        CallItemRow(
                            call = call,
                            onRecall = {
                                viewModel.startCall(
                                    peerName = call.peerName,
                                    peerWalletId = call.peerWalletId ?: "",
                                    peerAvatar = call.peerAvatar,
                                    isVideo = call.isVideo
                                )
                            }
                        )
                        Divider(
                            color = CashPayNavyBorder.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 78.dp)
                        )
                    }
                }
            }
        }

        // FAB to start direct call
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = OnChatVioletDeep.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = OnChatPrimaryGradient)
                    .clickable { showDirectCallDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddIcCall,
                    contentDescription = "Nouvel appel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Modal 1: Create Live Conference Dialog
    if (showNewLiveModal) {
        CreateLiveStageDialog(
            onDismiss = { showNewLiveModal = false },
            onStartLive = { title, isPaid, fee, currency, isAudioOnly ->
                viewModel.startLiveRoom(title, isPaid, fee, currency, isAudioOnly)
                showNewLiveModal = false
            }
        )
    }

    // Modal 2: Direct Call Dialog
    if (showDirectCallDialog) {
        DirectCallDialog(
            onDismiss = { showDirectCallDialog = false },
            onStartCall = { query, isVideo ->
                if (query.uppercase().startsWith("CD") || query.uppercase().startsWith("CP") || query.any { it.isDigit() }) {
                    viewModel.startCallByWalletId(query.trim(), isVideo)
                } else {
                    viewModel.startCall(peerName = query.trim(), isVideo = isVideo)
                }
                showDirectCallDialog = false
            }
        )
    }

    // Modal 3: Pay to Join Live Stage Dialog (Debit with PIN)
    if (pendingPaidRoom != null) {
        val room = pendingPaidRoom!!
        PayToJoinLiveDialog(
            room = room,
            onDismiss = { pendingPaidRoom = null },
            onConfirmPay = { pin ->
                coroutineScope.launch {
                    val ok = viewModel.payAndJoinLiveRoom(room, pin)
                    if (ok) {
                        pendingPaidRoom = null
                    } else {
                        Toast.makeText(context, "Échec du paiement. Vérifiez votre solde et votre PIN.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun LiveRoomCard(
    room: LiveStageRoom,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top badges (LIVE pill + Viewers count)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF334B)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color.White))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EN DIRECT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${room.spectatorCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Host info
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBadge(name = room.hostName, avatarUrl = room.hostAvatar, size = 36.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.hostName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID : ${room.hostWalletId}",
                        color = OnChatCyan,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = room.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer (Free vs Paid Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (room.isPaid) {
                    Text(
                        text = "${room.entryFee} ${room.currency}",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Gratuit",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (room.isPaid) Color(0xFFF59E0B) else OnChatViolet
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (room.isPaid) "Accès VIP" else "Rejoindre",
                        color = if (room.isPaid) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CreateLiveStageDialog(
    onDismiss: () -> Unit,
    onStartLive: (title: String, isPaid: Boolean, fee: Double, currency: String, isAudioOnly: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("Réunion & Échange OnChat") }
    var isAudioOnly by remember { mutableStateOf(false) }
    var isPaid by remember { mutableStateOf(false) }
    var entryFeeText by remember { mutableStateOf("5.00") }
    var currency by remember { mutableStateOf("USD") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lancer une Conférence",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Text(
                    text = "Créez une conférence audio multi-utilisateurs ou une visioconférence en direct avec vos contacts.",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Audio vs Video Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isAudioOnly = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isAudioOnly) OnChatViolet else CashPayNavyDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vidéo", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { isAudioOnly = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAudioOnly) OnChatCyan else CashPayNavyDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio (Multi)", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la réunion", color = CashPayGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Paid vs Free Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Accès Payant / Billeterie", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Les spectateurs paient pour entrer", color = CashPayGrey, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isPaid,
                        onCheckedChange = { isPaid = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
                    )
                }

                if (isPaid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = entryFeeText,
                        onValueChange = { entryFeeText = it },
                        label = { Text("Prix du ticket ($currency)", color = CashPayGrey) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF59E0B),
                            cursorColor = Color(0xFFF59E0B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val fee = entryFeeText.toDoubleOrNull() ?: 0.0
                        onStartLive(title.trim(), isPaid, if (isPaid) fee else 0.0, currency, isAudioOnly)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAudioOnly) OnChatCyan else OnChatViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(if (isAudioOnly) Icons.Default.VolumeUp else Icons.Default.Sensors, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAudioOnly) "Démarrer la conférence audio" else "Démarrer la visioconférence", fontWeight = FontWeight.Bold, color = if (isAudioOnly) Color.Black else Color.White)
                }
            }
        }
    }
}

@Composable
fun PayToJoinLiveDialog(
    room: LiveStageRoom,
    onDismiss: () -> Unit,
    onConfirmPay: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Conférence Privée VIP",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = room.title,
                    color = CashPayGrey,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CashPayNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Organisateur", color = CashPayGrey, fontSize = 12.sp)
                            Text(room.hostName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ticket d'entrée", color = CashPayGrey, fontSize = 12.sp)
                            Text("${room.entryFee} ${room.currency}", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("Code PIN de votre compte", color = CashPayGrey) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onConfirmPay(pin) },
                    enabled = pin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Payer & Entrer dans la conférence", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DirectCallDialog(
    onDismiss: () -> Unit,
    onStartCall: (name: String, isVideo: Boolean) -> Unit
) {
    var peerName by remember { mutableStateOf("") }
    var isVideo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Démarrer un Appel", color = Color.White) },
        text = {
            Column {
                Text(
                    text = "Saisissez le Wallet ID ou le nom du contact pour passer un appel direct sécurisé.",
                    color = CashPayGrey,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = peerName,
                    onValueChange = { peerName = it },
                    label = { Text("ID Wallet ou Nom du contact", color = CashPayGrey) },
                    placeholder = { Text("Ex : CD0100000001 ou Patrick", color = CashPayGrey.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isVideo = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isVideo) OnChatViolet else CashPayNavyBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio")
                    }

                    Button(
                        onClick = { isVideo = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVideo) OnChatViolet else CashPayNavyBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vidéo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (peerName.isNotBlank()) {
                        onStartCall(peerName.trim(), isVideo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet)
            ) {
                Text("Lancer l'appel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = CashPayGrey)
            }
        },
        containerColor = CashPayNavySurface
    )
}

@Composable
fun CallItemRow(
    call: CallRecord,
    onRecall: () -> Unit
) {
    val timeFormatted = remember(call.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(call.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRecall() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBadge(name = call.peerName, avatarUrl = call.peerAvatar, size = 50.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.peerName,
                color = if (call.isMissed) Color(0xFFFF5252) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!call.peerWalletId.isNullOrBlank()) {
                Text(
                    text = "ID: ${call.peerWalletId}",
                    color = OnChatCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        call.isMissed -> Icons.Default.CallMissed
                        call.isIncoming -> Icons.Default.CallReceived
                        else -> Icons.Default.CallMade
                    },
                    contentDescription = null,
                    tint = if (call.isMissed) Color(0xFFFF5252) else SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormatted,
                    color = CashPayGrey,
                    fontSize = 12.sp
                )
                if (call.durationSeconds > 0) {
                    Text(
                        text = " • ${call.durationSeconds / 60}m ${call.durationSeconds % 60}s",
                        color = CashPayGrey,
                        fontSize = 12.sp
                    )
                }
            }
        }

        IconButton(onClick = onRecall) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Rappeler",
                tint = OnChatCyan,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
