package com.example.ui.screens.calls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.ApiClient
import com.example.data.model.*
import com.example.ui.components.AvatarBadge
import com.example.ui.components.SkeletonCallRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class CallsTabMode(val title: String) {
    MEETINGS("Réunions & Vidéo"),
    DIRECT_CALLS("Appels Directs")
}

@Composable
fun CallsScreen(
    viewModel: OnChatViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(CallsTabMode.MEETINGS) }

    // State flows
    val recentMeetings by viewModel.recentMeetings.collectAsState()
    val isMeetingsLoading by viewModel.isMeetingsLoading.collectAsState()
    val apiCallsHistory by viewModel.apiCallsHistory.collectAsState()
    val localCalls by viewModel.calls.collectAsState()
    val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()

    // Modals
    var showCreateMeetingDialog by remember { mutableStateOf(false) }
    var showJoinMeetingDialog by remember { mutableStateOf(false) }
    var createdMeetingResult by remember { mutableStateOf<CreateMeetingResponse?>(null) }
    var showDirectCallDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
            .testTag("calls_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented Header Navigation Bar
            Surface(
                color = CashPayNavySurface,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = CashPayNavySurface,
                    contentColor = OnChatCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                            color = OnChatCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    CallsTabMode.values().forEach { tab ->
                        val selected = activeTab == tab
                        Tab(
                            selected = selected,
                            onClick = { activeTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) OnChatCyan else CashPayGrey
                                )
                            },
                            modifier = Modifier.testTag("calls_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    CallsTabMode.MEETINGS -> {
                        MeetingsTabContent(
                            meetings = recentMeetings,
                            isLoading = isMeetingsLoading,
                            onCreateMeeting = { showCreateMeetingDialog = true },
                            onJoinMeetingWithCode = { showJoinMeetingDialog = true },
                            onSelectMeeting = { code -> viewModel.joinMeeting(code) },
                            onRefresh = { viewModel.fetchRecentMeetings() }
                        )
                    }
                    CallsTabMode.DIRECT_CALLS -> {
                        DirectCallsTabContent(
                            apiCalls = apiCallsHistory,
                            localCalls = localCalls,
                            isLoading = isSkeletonLoading,
                            onStartCall = { showDirectCallDialog = true },
                            onRecall = { peerName, walletId, isVideo ->
                                viewModel.startCall(
                                    peerName = peerName,
                                    peerWalletId = walletId,
                                    isVideo = isVideo
                                )
                            },
                            onRefresh = { viewModel.fetchCallsHistoryApi() }
                        )
                    }
                }
            }
        }

        // FAB to start actions depending on tab
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            when (activeTab) {
                CallsTabMode.MEETINGS -> {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateMeetingDialog = true },
                        containerColor = OnChatViolet,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("fab_create_meeting")
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nouvelle réunion", fontWeight = FontWeight.Bold)
                    }
                }
                CallsTabMode.DIRECT_CALLS -> {
                    FloatingActionButton(
                        onClick = { showDirectCallDialog = true },
                        containerColor = OnChatCyan,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("fab_direct_call")
                    ) {
                        Icon(Icons.Default.AddIcCall, contentDescription = "Nouvel appel")
                    }
                }
            }
        }
    }

    // Modal 1: Créer une réunion (POST /api/v1/meetings/create)
    if (showCreateMeetingDialog) {
        CreateMeetingDialog(
            onDismiss = { showCreateMeetingDialog = false },
            onCreate = { topic, type, scheduledAt, durationMin, price, currency ->
                viewModel.createMeeting(
                    topic = topic,
                    type = type,
                    scheduledAt = scheduledAt,
                    durationMin = durationMin,
                    priceAmount = price,
                    priceCurrency = currency,
                    autoJoin = false,
                    onSuccess = { res ->
                        showCreateMeetingDialog = false
                        createdMeetingResult = res
                    }
                )
            }
        )
    }

    // Modal 2: Succès Création Réunion & Partage (joinUrl + meetCode)
    if (createdMeetingResult != null) {
        val result = createdMeetingResult!!
        MeetingCreatedSuccessDialog(
            response = result,
            onDismiss = { createdMeetingResult = null },
            onJoinNow = {
                val code = result.meetCode ?: ""
                createdMeetingResult = null
                viewModel.joinMeeting(code)
            }
        )
    }

    // Modal 3: Rejoindre une réunion avec un code (POST /api/v1/meetings/{meetCode}/join)
    if (showJoinMeetingDialog) {
        JoinMeetingDialog(
            onDismiss = { showJoinMeetingDialog = false },
            onJoin = { code ->
                showJoinMeetingDialog = false
                viewModel.joinMeeting(code)
            }
        )
    }

    // Modal 4: Démarrer un appel direct (POST /api/v1/calls/initiate)
    if (showDirectCallDialog) {
        DirectCallDialog(
            onDismiss = { showDirectCallDialog = false },
            onStartCall = { query, isVideo ->
                val myWallet = currentSession?.walletId.orEmpty()
                if (query.equals(myWallet, ignoreCase = true)) {
                    Toast.makeText(context, "Impossible de s'appeler soi-même (400)", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.initiateCallApi(query.trim(), isVideo)
                    showDirectCallDialog = false
                }
            }
        )
    }
}

// ==========================================
// Contenu Onglet 1: Réunions (Meetings)
// ==========================================

@Composable
fun MeetingsTabContent(
    meetings: List<MeetingItemDto>,
    isLoading: Boolean,
    onCreateMeeting: () -> Unit,
    onJoinMeetingWithCode: () -> Unit,
    onSelectMeeting: (code: String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card: Actions rapides Réunions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(brush = OnChatPrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Réunions & Conférences",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Créez ou rejoignez une conférence sécurisée",
                                color = CashPayGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCreateMeeting,
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_new_meeting")
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Créer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onJoinMeetingWithCode,
                            colors = ButtonDefaults.buttonColors(containerColor = CashPayNavyDark),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_join_meeting")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rejoindre", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section Réunions Récentes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RÉUNIONS RÉCENTES (HÔTE)",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualiser",
                        tint = OnChatCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isLoading && meetings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OnChatCyan)
                }
            }
        } else if (meetings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = CashPayGrey,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aucune réunion récente",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Créez votre première réunion pour obtenir un code et un lien de partage.",
                            color = CashPayGrey,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(meetings) { meeting ->
                MeetingItemCard(
                    meeting = meeting,
                    onJoin = { onSelectMeeting(meeting.code) }
                )
            }
        }
    }
}

@Composable
fun MeetingItemCard(
    meeting: MeetingItemDto,
    onJoin: () -> Unit
) {
    val context = LocalContext.current
    val isActive = meeting.status != "ended"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoin() }
            .testTag("meeting_item_${meeting.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge (Audio ou Vidéo)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (meeting.type == "audio") OnChatCyan.copy(alpha = 0.2f) else OnChatViolet.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (meeting.type == "audio") Icons.Default.Mic else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (meeting.type == "audio") OnChatCyan else OnChatVioletLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (meeting.type == "audio") "Audio" else "Vidéo",
                                color = if (meeting.type == "audio") OnChatCyan else OnChatVioletLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Pill (Active / Ended)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isActive) SuccessGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE" else "TERMINÉE",
                            color = if (isActive) SuccessGreen else CashPayGrey,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bouton Copier Code
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code Réunion", meeting.code))
                        Toast.makeText(context, "Code ${meeting.code} copié !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copier le code",
                        tint = CashPayGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Titre / Topic
            Text(
                text = meeting.topic ?: "Réunion OnChat",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Code & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code : ${meeting.code}",
                    color = OnChatCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isActive) {
                    Button(
                        onClick = onJoin,
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Entrer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// Contenu Onglet 2: Appels Directs
// ==========================================

@Composable
fun DirectCallsTabContent(
    apiCalls: List<ApiCallDto>,
    localCalls: List<CallRecord>,
    isLoading: Boolean,
    onStartCall: () -> Unit,
    onRecall: (name: String, walletId: String, isVideo: Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIQUE DES APPELS (API / CALLS)",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualiser",
                        tint = OnChatCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (apiCalls.isEmpty() && localCalls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = CashPayGrey,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aucun appel dans l'historique",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Initiez un appel audio ou vidéo avec un portefeuille CashPay.",
                            color = CashPayGrey,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else if (apiCalls.isNotEmpty()) {
            items(apiCalls) { call ->
                ApiCallItemRow(call = call, onRecall = onRecall)
            }
        } else {
            items(localCalls) { call ->
                CallItemRow(
                    call = call,
                    onRecall = { onRecall(call.peerName, call.peerWalletId.orEmpty(), call.isVideo) }
                )
            }
        }
    }
}

@Composable
fun ApiCallItemRow(
    call: ApiCallDto,
    onRecall: (name: String, walletId: String, isVideo: Boolean) -> Unit
) {
    val isMissed = call.status == "MISSED" || call.status == "REJECTED"
    val displayName = call.receiverName ?: call.callerName ?: "Appel CashPay"
    val targetWallet = call.receiverWalletId ?: call.callerWalletId ?: ""

    Surface(
        color = CashPayNavySurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRecall(displayName, targetWallet, call.isVideo) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(name = displayName, avatarUrl = call.receiverPhoto ?: call.callerPhoto, size = 46.dp)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = if (isMissed) Color(0xFFFF5252) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (targetWallet.isNotBlank()) {
                    Text(
                        text = "Wallet : $targetWallet",
                        color = OnChatCyan,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (call.status) {
                            "MISSED" -> Icons.Default.CallMissed
                            "REJECTED" -> Icons.Default.PhoneDisabled
                            "BUSY" -> Icons.Default.PhoneLocked
                            else -> Icons.Default.CallMade
                        },
                        contentDescription = null,
                        tint = if (isMissed) Color(0xFFFF5252) else SuccessGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Statut : ${call.status ?: "COMPLETED"}",
                        color = CashPayGrey,
                        fontSize = 11.sp
                    )
                    if (call.durationSeconds > 0) {
                        Text(
                            text = " • ${call.durationSeconds / 60}m ${call.durationSeconds % 60}s",
                            color = CashPayGrey,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            IconButton(onClick = { onRecall(displayName, targetWallet, call.isVideo) }) {
                Icon(
                    imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Rappeler",
                    tint = OnChatCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}


// ==========================================
// Dialogs: Créer, Rejoindre & Succès
// ==========================================

@Composable
fun CreateMeetingDialog(
    onDismiss: () -> Unit,
    onCreate: (topic: String, type: String, scheduledAt: String?, durationMin: Int, price: Double, currency: String) -> Unit
) {
    var topic by remember { mutableStateOf("Réunion d'équipe Finuseco") }
    var isAudioOnly by remember { mutableStateOf(false) }
    var durationMinutesText by remember { mutableStateOf("60") }
    var isPaid by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf("USD") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nouvelle Réunion",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Text(
                    text = "POST /api/v1/meetings/create",
                    color = OnChatCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Type Toggle (Audio vs Vidéo)
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
                        Icon(Icons.Default.Mic, contentDescription = null, tint = if (isAudioOnly) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio", fontSize = 12.sp, color = if (isAudioOnly) Color.Black else Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Topic Field (Max 120 chars)
                OutlinedTextField(
                    value = topic,
                    onValueChange = { if (it.length <= 120) topic = it },
                    label = { Text("Sujet de la réunion", color = CashPayGrey) },
                    supportingText = {
                        Text(
                            text = "${topic.length}/120 caractères",
                            color = CashPayGrey,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Durée en minutes
                OutlinedTextField(
                    value = durationMinutesText,
                    onValueChange = { durationMinutesText = it },
                    label = { Text("Durée estimée (minutes)", color = CashPayGrey) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Facturation / Billeterie Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Accès Payant / Billeterie", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Frais d'entrée pour la réunion", color = CashPayGrey, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isPaid,
                        onCheckedChange = { isPaid = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
                    )
                }

                if (isPaid) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Tarif", color = CashPayGrey) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                cursorColor = Color(0xFFF59E0B)
                            )
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("Devise", color = CashPayGrey) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                cursorColor = Color(0xFFF59E0B)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val duration = durationMinutesText.toIntOrNull() ?: 60
                        val price = if (isPaid) (priceText.toDoubleOrNull() ?: 0.0) else 0.0
                        onCreate(topic.trim(), if (isAudioOnly) "audio" else "video", null, duration, price, currency)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_create_meeting_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer la réunion", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MeetingCreatedSuccessDialog(
    response: CreateMeetingResponse,
    onDismiss: () -> Unit,
    onJoinNow: () -> Unit
) {
    val context = LocalContext.current
    val meetCode = response.meetCode ?: "code-meet"
    val joinUrl = response.joinUrl ?: "https://app.cashpay-all.com/feedback/meet/$meetCode"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Réunion Créée !",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = response.topic ?: "Réunion OnChat",
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
                            Text("Code de réunion (meetCode)", color = CashPayGrey, fontSize = 10.sp)
                            Text(meetCode, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code Réunion", meetCode))
                            Toast.makeText(context, "Code copié !", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = OnChatCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bloc Lien de réunion (joinUrl)
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
                            Text("Lien de partage (joinUrl)", color = CashPayGrey, fontSize = 10.sp)
                            Text(joinUrl, color = OnChatCyan, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Lien Réunion", joinUrl))
                            Toast.makeText(context, "Lien copié !", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Link, contentDescription = "Copier", tint = OnChatCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onJoinNow,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("enter_meeting_now_button")
                ) {
                    Text("Entrer dans la réunion", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Rejoignez ma réunion OnChat / CashPay All !\nCode : $meetCode\nLien : $joinUrl"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Partager"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager l'invitation", color = OnChatCyan)
                }
            }
        }
    }
}

@Composable
fun JoinMeetingDialog(
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rejoindre une réunion", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Saisissez le code de réunion fourni par l'hôte ou collez le lien complet.",
                    color = CashPayGrey,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { raw ->
                        // Si l'utilisateur colle un lien "https://app.cashpay-all.com/feedback/meet/CODE", extraire le code
                        val cleaned = if (raw.contains("/meet/")) {
                            raw.substringAfterLast("/meet/").trim()
                        } else {
                            raw.trim()
                        }
                        codeInput = cleaned
                    },
                    label = { Text("Code de réunion (meetCode)", color = CashPayGrey) },
                    placeholder = { Text("Ex: abc123-def456-789abc", color = CashPayGrey.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_meet_code")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeInput.isNotBlank()) {
                        onJoin(codeInput.trim())
                    }
                },
                enabled = codeInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                modifier = Modifier.testTag("submit_join_meeting_button")
            ) {
                Text("Rejoindre")
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
                    text = "Saisissez le Wallet ID de l'utilisateur CashPay pour initier un appel (POST /api/v1/calls/initiate).",
                    color = CashPayGrey,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = peerName,
                    onValueChange = { peerName = it },
                    label = { Text("ID Wallet du destinataire", color = CashPayGrey) },
                    placeholder = { Text("Ex : CD0100000002", color = CashPayGrey.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("direct_call_wallet_input")
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
                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                modifier = Modifier.testTag("submit_direct_call_button")
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
