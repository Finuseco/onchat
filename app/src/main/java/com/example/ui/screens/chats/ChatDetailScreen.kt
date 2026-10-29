package com.example.ui.screens.chats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import com.example.data.model.assetBalance

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactCheckResult
import com.example.data.model.Message
import com.example.data.model.TransferQuoteResponse
import com.example.data.model.TransferResponse
import com.example.ui.components.AvatarBadge
import com.example.ui.components.SkeletonMessageBubble
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import com.example.util.AudioRecorder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    viewModel: OnChatViewModel,
    onBack: () -> Unit,
    onOpenProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val conversations by viewModel.conversations.collectAsState()
    val conv = remember(conversations, conversationId) {
        conversations.find { it.id == conversationId }
    }
    val messages by viewModel.activeMessages.collectAsState()
    val session by viewModel.currentSession.collectAsState()
    val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsState()
    val walletData by viewModel.walletData.collectAsState()
    val isWalletLoading by viewModel.isWalletLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Background Image state
    val chatBackgroundUri by viewModel.repository.currentSessionFlow.map { it?.customChatBackgroundUri }.collectAsState(initial = null)
    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setChatBackgroundUri(it.toString()) }
    }

    // Audio recording state
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var recordingFile by remember { mutableStateOf<java.io.File?>(null) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = java.io.File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
                recordingFile = file
                recordingDurationSeconds = 0
                audioRecorder.start(file)
                isRecording = true
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur micro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permission microphone requise", Toast.LENGTH_SHORT).show()
        }
    }

    // Recording duration timer ticker (unlimited duration, no 10-minute cap)
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingDurationSeconds += 1
            }
        }
    }

    fun startRecordingFlow() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val file = java.io.File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
                recordingFile = file
                recordingDurationSeconds = 0
                audioRecorder.start(file)
                isRecording = true
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur micro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAndSendRecording() {
        if (!isRecording) return
        try {
            audioRecorder.stop()
        } catch (_: Exception) {}
        isRecording = false
        val file = recordingFile
        if (file != null && file.exists() && file.length() > 0) {
            viewModel.sendAudioMessage(file)
            Toast.makeText(context, "Note vocale envoyée !", Toast.LENGTH_SHORT).show()
        }
        recordingFile = null
        recordingDurationSeconds = 0
    }

    fun cancelRecording() {
        if (!isRecording) return
        try {
            audioRecorder.stop()
        } catch (_: Exception) {}
        isRecording = false
        recordingFile?.delete()
        recordingFile = null
        recordingDurationSeconds = 0
        Toast.makeText(context, "Enregistrement annulé", Toast.LENGTH_SHORT).show()
    }

    // Full Screen Image Viewer state (Triple of imageUrl, fileName, message)
    var selectedImageForViewer by remember { mutableStateOf<Triple<String, String?, Message>?>(null) }

    // Attachments bottom sheet & Emoji / Sticker picker states
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiStickerPicker by remember { mutableStateOf(false) }

    // Message action modal / dialogs
    var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var messageIdToEdit by remember { mutableStateOf<String?>(null) }
    var editContentText by remember { mutableStateOf("") }

    // In-Chat Transfer Modal states (CashPay 2-step preview & confirmation)
    var showTransferModal by remember { mutableStateOf(false) }
    var transferAmountText by remember { mutableStateOf("10000") }
    var transferCurrency by remember { mutableStateOf(conv?.currency ?: "CDF") }
    var transferPin by remember { mutableStateOf("") }
    var isTransferLoading by remember { mutableStateOf(false) }
    var transferPreviewResult by remember { mutableStateOf<TransferResponse?>(null) }
    var transferSuccessResult by remember { mutableStateOf<TransferResponse?>(null) }
    var transferErrorMessage by remember { mutableStateOf<String?>(null) }
    var transferStep by remember { mutableStateOf(1) } // 1: Saisie, 2: Visualisation (Preview), 3: PIN Confirm, 4: Succès

    // Real-time WebSocket typing status
    val typingMap by viewModel.typingStatus.collectAsState()
    val typingUser = typingMap[conversationId]

    // Fetch conversation details from server and mark as read via REST + WebSocket
    LaunchedEffect(conversationId) {
        viewModel.fetchConversationDetails(conversationId)
        viewModel.onEnterChat(conversationId)
    }

    // Scroll to bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Root Container with Background
    Box(modifier = Modifier.fillMaxSize().background(CashPayNavyDark)) {
        // Full Screen Background Image (from albums/gallery)
        if (chatBackgroundUri != null) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(chatBackgroundUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.5f // Sufficiently dimmed for readability
            )
        } else {
            // Telegram Wallpaper Subtle Background Pattern
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val patternColor = Color(0xFF1A233D).copy(alpha = 0.35f)
                for (x in 0..4) {
                    for (y in 0..8) {
                        val cx = (w / 4) * x
                        val cy = (h / 8) * y + ((x % 2) * 40f)
                        drawCircle(patternColor, radius = 6f, center = Offset(cx, cy))
                    }
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenProfile?.invoke(conversationId)
                                }
                        ) {
                            AvatarBadge(
                                name = conv?.name ?: "Discussion",
                                avatarUrl = conv?.avatarUrl,
                                size = 40.dp,
                                isGroup = conv?.isGroup ?: false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = conv?.name ?: "Discussion",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        typingUser != null -> "✍️ $typingUser écrit..."
                                        conv?.isGroup == true -> "Groupe OnChat • ${conv.currency ?: "USD"}"
                                        else -> "en ligne • CashPay"
                                    },
                                    color = if (typingUser != null) Color(0xFF4ECCA3) else OnChatCyan,
                                    fontSize = 12.sp,
                                    fontWeight = if (typingUser != null) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (conv?.isGroup == true) {
                                conversationId.removePrefix("g_").toLongOrNull()?.let { viewModel.createGroupMeeting(it, "audio") }
                            } else viewModel.startCall(
                                peerName = conv?.name ?: "Contact",
                                peerWalletId = conv?.contactWalletId ?: "",
                                peerAvatar = conv?.avatarUrl,
                                isVideo = false
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Appel audio",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            if (conv?.isGroup == true) {
                                conversationId.removePrefix("g_").toLongOrNull()?.let { viewModel.createGroupMeeting(it, "video") }
                            } else viewModel.startCall(
                                peerName = conv?.name ?: "Contact",
                                peerWalletId = conv?.contactWalletId ?: "",
                                peerAvatar = conv?.avatarUrl,
                                isVideo = true
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Appel vidéo",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            backgroundPickerLauncher.launch("image/*")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Changer le fond",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            onOpenProfile?.invoke(conversationId)
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Profil",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CashPayNavy.copy(alpha = 0.8f)
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(CashPayNavy.copy(alpha = 0.9f))
                        .navigationBarsPadding()
                ) {
                    if (isRecording) {
                        // Dedicated Live Voice Recording Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .background(CashPayNavyDark, shape = RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Blinking recording dot
                            val pulseTrans = rememberInfiniteTransition(label = "rec")
                            val recAlpha by pulseTrans.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                                label = "rec_alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5252).copy(alpha = recAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Timer mm:ss (Unlimited recording, no 10-minute cap)
                            val recMins = recordingDurationSeconds / 60
                            val recSecs = recordingDurationSeconds % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", recMins, recSecs),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Enregistrement audio...",
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            // Cancel / Trash Button
                            IconButton(
                                onClick = { cancelRecording() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Annuler",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Send Voice Note Button
                            IconButton(
                                onClick = { stopAndSendRecording() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OnChatCyan)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Envoyer l'audio",
                                    tint = CashPayNavyDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Bottom Input Bar matching Screenshot 1:
                        // [😄 Emoji] [Message field] [📎 Paperclip Attachment] [🎤/➤ Send]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Emoji / Stickers Toggle Button
                            IconButton(
                                onClick = {
                                    showEmojiStickerPicker = !showEmojiStickerPicker
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (showEmojiStickerPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                                    contentDescription = "Émojis & Stickers",
                                    tint = if (showEmojiStickerPicker) OnChatCyan else CashPayGrey,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 2. Main Message Input Field
                            TextField(
                                value = inputText,
                                onValueChange = {
                                    inputText = it
                                    if (it.isNotEmpty()) {
                                        viewModel.sendTyping(conversationId)
                                    }
                                    if (showEmojiStickerPicker) {
                                        showEmojiStickerPicker = false
                                    }
                                },
                                placeholder = {
                                    Text("Message", color = CashPayGrey, fontSize = 15.sp)
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CashPayNavySurface.copy(alpha = 0.7f),
                                    unfocusedContainerColor = CashPayNavySurface.copy(alpha = 0.7f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = OnChatCyan,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp, max = 100.dp),
                                maxLines = 4
                            )

                            // 3. Attachment Paperclip Button (Opens Wallet / Fichier / Position / Liste / Contact)
                            IconButton(
                                onClick = {
                                    showAttachmentSheet = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Menu Pièces Jointes & Wallet",
                                    tint = CashPayGrey,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // 4. Send or Audio Record Button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = if (inputText.isNotBlank()) OnChatPrimaryGradient
                                        else Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0)))
                                    )
                                    .clickable {
                                        if (inputText.isNotBlank()) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                            showEmojiStickerPicker = false
                                        } else {
                                            startRecordingFlow()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (inputText.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                    contentDescription = if (inputText.isNotBlank()) "Envoyer" else "Enregistrer audio",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Expandable Emoji & Sticker Picker Keyboard
                    AnimatedVisibility(
                        visible = showEmojiStickerPicker,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        EmojiStickerPicker(
                            onEmojiSelected = { emoji ->
                                inputText += emoji
                            },
                            onStickerSelected = { sticker ->
                                viewModel.sendMessage("${sticker.emojiRepresentation} ${sticker.title}", type = "sticker")
                                showEmojiStickerPicker = false
                            },
                            onGifSelected = { gifTitle ->
                                viewModel.sendMessage("🎬 $gifTitle", type = "sticker")
                                showEmojiStickerPicker = false
                            },
                            onBackspace = {
                                if (inputText.isNotEmpty()) {
                                    inputText = inputText.dropLast(1)
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isSkeletonLoading && messages.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(3) {
                            SkeletonMessageBubble(isOutgoing = false)
                            SkeletonMessageBubble(isOutgoing = true)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date header badge matching Screenshot 1 ("21 septembre")
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = CashPayNavySurface.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "21 septembre",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        items(messages, key = { it.id }) { msg ->
                            MessageBubbleItem(
                                message = msg,
                                myAvatar = session?.avatarUrl,
                                contactAvatar = conv?.avatarUrl,
                                onImageClick = { imgUrl, fileName ->
                                    selectedImageForViewer = Triple(imgUrl, fileName, msg)
                                },
                                onJoinMeeting = { meetCode ->
                                    viewModel.joinMeeting(meetCode)
                                },
                                onLongPress = {
                                    if (msg.isMine) {
                                        selectedMessageForAction = msg
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Screen Zoomable Image Viewer Dialog with Multi-Conversation Forwarding
    if (selectedImageForViewer != null) {
        val (imgUrl, fName, msg) = selectedImageForViewer!!
        ImageViewerDialog(
            imageUrl = imgUrl,
            fileName = fName,
            senderName = if (msg.isMine) "Moi" else (conv?.name ?: msg.senderName),
            timestamp = msg.timestamp,
            conversations = conversations,
            onDismiss = { selectedImageForViewer = null },
            onForward = { targetConvIds ->
                viewModel.forwardMessageToConversations(targetConvIds, msg)
                selectedImageForViewer = null
            }
        )
    }

    // Attachment Bottom Sheet Menu (Matches Screenshot 2: Wallet, Fichier, Position, Liste, Contact)
    if (showAttachmentSheet) {
        ChatAttachmentSheet(
            recipientName = conv?.name ?: "Contact",
            conversationCurrency = conv?.currency ?: "USD",
            walletData = walletData,
            isWalletLoading = isWalletLoading,
            onRefreshWallet = { viewModel.refreshWallet() },
            onDismiss = { showAttachmentSheet = false },
            onSendMoneyClick = { chosenCurrency ->
                transferCurrency = chosenCurrency
                transferStep = 1
                transferPin = ""
                transferPreviewResult = null
                transferSuccessResult = null
                transferErrorMessage = null
                transferAmountText = when (chosenCurrency.uppercase()) {
                    "CDF" -> "10000"
                    "BTC" -> "0.001"
                    else -> "10"
                }
                showTransferModal = true
            },
            onSendLocation = { title, subtitle, lat, lng ->
                viewModel.sendMessage("$title • $subtitle", type = "location")
            },
            onSendFile = { fileName, fileType, sizeStr, uri ->
                if (uri != null) {
                    when (fileType) {
                        "video" -> {
                            viewModel.uploadVideo(uri) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Vidéo envoyée !", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Erreur vidéo: $error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        "image" -> {
                            coroutineScope.launch {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    val url = viewModel.uploadMediaBytes(bytes, fileName, "image/jpeg")
                                    if (url != null) {
                                        viewModel.sendMessage("🖼️ Photo", type = "image", fileUrl = url, fileName = fileName)
                                    } else {
                                        Toast.makeText(context, "Erreur upload image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        "audio" -> {
                            coroutineScope.launch {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    val url = viewModel.uploadMediaBytes(bytes, fileName, "audio/mp4")
                                    if (url != null) {
                                        viewModel.sendMessage("🎤 Note vocale", type = "audio", fileUrl = url, fileName = fileName)
                                    }
                                }
                            }
                        }
                        else -> {
                            // Doc
                            coroutineScope.launch {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    val url = viewModel.uploadMediaBytes(bytes, fileName, "application/pdf")
                                    if (url != null) {
                                        viewModel.sendMessage("📄 $fileName ($sizeStr)", type = "doc", fileUrl = url, fileName = fileName)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    viewModel.sendMessage("📄 $fileName ($sizeStr)", type = if (fileType == "image") "image" else "doc")
                }
            },
            onSendContact = { name, phone, walletId, avatar ->
                val avatarPart = if (!avatar.isNullOrBlank()) " • Avatar: $avatar" else ""
                viewModel.sendMessage("$name ($phone) • Wallet: $walletId$avatarPart", type = "contact")
            },
            onSendPoll = { question, options ->
                viewModel.sendMessage("$question | ${options.joinToString(";")}", type = "poll")
            },
            searchProfile = { phone -> viewModel.repository.searchProfileByPhone(phone).getOrNull() }
        )
    }

    // In-Chat Financial Transfer Modal Sheet (Official CashPay 2-Step Preview & Confirmation)
    if (showTransferModal) {
        var recipientWalletId by remember { mutableStateOf<String?>(conv?.contactWalletId) }
        var recipientBtcAddress by remember { mutableStateOf<String?>(conv?.contactBitcoinAddress) }

        LaunchedEffect(conv?.id, conv?.contactPhone) {
            val (w, b) = viewModel.resolveConversationRecipient(conv)
            if (!w.isNullOrBlank()) recipientWalletId = w
            if (!b.isNullOrBlank()) recipientBtcAddress = b
            if (conv?.contactPhone != null) {
                val result = viewModel.searchProfileByPhone(conv.contactPhone!!)
                if (result?.walletId != null) {
                    recipientWalletId = result.walletId
                }
                if (result?.bitcoinAddress != null) {
                    recipientBtcAddress = result.bitcoinAddress
                }
            }
        }

        LaunchedEffect(transferCurrency, conv?.contactPhone) {
            if (transferCurrency.equals("BTC", ignoreCase = true) && recipientBtcAddress.isNullOrBlank() && conv?.contactPhone != null) {
                val addr = viewModel.getRecipientBitcoinAddress(conv.contactPhone!!)
                if (addr != null) recipientBtcAddress = addr
            }
        }

        // Available balance for the selected currency from authenticated wallet API
        val realBalance = walletData?.assetBalance(transferCurrency) ?: 0.0

        AlertDialog(
            onDismissRequest = {
                if (!isTransferLoading) {
                    showTransferModal = false
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(brush = OnChatPrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (transferStep) {
                                1 -> Icons.Default.CurrencyExchange
                                2 -> Icons.Default.Visibility
                                3 -> Icons.Default.Lock
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when (transferStep) {
                                1 -> "Transfert CashPay"
                                2 -> "Vérifier le transfert"
                                3 -> "Confirmer le transfert"
                                else -> "Transfert effectué"
                            },
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (transferStep) {
                                1 -> "Étape 1 — Saisie"
                                2 -> "Étape 2 — Visualisation"
                                3 -> "Étape 3 — Validation sécurisée"
                                else -> "Succès"
                            },
                            color = OnChatCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Recipient Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBadge(
                            name = conv?.name ?: "Contact",
                            avatarUrl = conv?.avatarUrl,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = conv?.name ?: "Destinataire",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val displayId = if (transferCurrency.equals("BTC", ignoreCase = true)) {
                                recipientBtcAddress ?: "Adresse BTC..."
                            } else {
                                recipientWalletId ?: conv?.contactWalletId ?: "Wallet ID..."
                            }
                            Text(
                                text = displayId,
                                color = CashPayGrey,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(bottom = 12.dp))

                    when (transferStep) {
                        1 -> {
                            // Destinataire Bitcoin ou Wallet ID avec option de saisie / collage
                            if (transferCurrency.uppercase() in setOf("BTC", "USDT")) {
                                Text("L’adresse ${transferCurrency} est vérifiée sur le compte CashPay de ce contact avant confirmation.", color = CashPayGrey, fontSize = 12.sp)
                            } else {
                                Text("Le destinataire est le correspondant de cette conversation, vérifié par CashPay.", color = CashPayGrey, fontSize = 12.sp)
                            }

                            // --- Étape 1 : Saisie ---
                            Text("Devise du transfert :", color = CashPayGrey, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            ) {
                                val availableCurrencies = remember(walletData) {
                                    val list = mutableListOf<String>()
                                    walletData?.nationalCurrency?.code?.let { list.add(it) }
                                    if (!list.contains("CDF")) list.add(0, "CDF")
                                    if (!list.contains("USD")) list.add("USD")
                                    if (!list.contains("EUR")) list.add("EUR")
                                    if (!list.contains("BTC")) list.add("BTC")
                                    if (!list.contains("USDT")) list.add("USDT")
                                    list
                                }
                                availableCurrencies.forEach { curr ->
                                    val isSelected = transferCurrency.equals(curr, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            transferCurrency = curr
                                            transferAmountText = when (curr.uppercase()) {
                                                "CDF" -> "10000"
                                                "BTC" -> "0.001"
                                                else -> "10"
                                            }
                                        },
                                        label = { Text(curr, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OnChatCyan,
                                            selectedLabelColor = CashPayNavyDark,
                                            containerColor = CashPayNavyDark,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Solde disponible actuel
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CashPayNavyDark,
                                border = BorderStroke(1.dp, CashPayNavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Solde disponible :", color = CashPayGrey, fontSize = 12.sp)
                                    Text(
                                        text = if (transferCurrency.equals("BTC", ignoreCase = true))
                                            String.format(Locale.US, "%.6f BTC", realBalance)
                                        else
                                            String.format(Locale.US, "%,.2f %s", realBalance, transferCurrency),
                                        color = if (realBalance > 0) OnChatCyan else Color(0xFFFF5252),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Montant :", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = transferAmountText,
                                onValueChange = { transferAmountText = it },
                                label = { Text("Montant", color = CashPayGrey) },
                                trailingIcon = {
                                    Text(
                                        text = transferCurrency,
                                        color = OnChatCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OnChatCyan,
                                    unfocusedBorderColor = CashPayNavyBorder,
                                    cursorColor = OnChatCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick presets
                            val quickPresets = when (transferCurrency.uppercase()) {
                                "CDF" -> listOf("5000" to "5 000 FC", "10000" to "10 000 FC", "25000" to "25 000 FC", "50000" to "50 000 FC", "100000" to "100 000 FC")
                                "USD" -> listOf("5" to "$5", "10" to "$10", "25" to "$25", "50" to "$50", "100" to "$100")
                                "EUR" -> listOf("5" to "5 €", "10" to "10 €", "25" to "25 €", "50" to "50 €", "100" to "100 €")
                                "BTC" -> listOf("0.0005" to "0.0005 BTC", "0.001" to "0.001 BTC", "0.005" to "0.005 BTC", "0.01" to "0.01 BTC")
                                else -> listOf("10" to "10 $transferCurrency", "25" to "25 $transferCurrency", "50" to "50 $transferCurrency", "100" to "100 $transferCurrency")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                quickPresets.forEach { (valStr, labelStr) ->
                                    FilterChip(
                                        selected = transferAmountText == valStr,
                                        onClick = { transferAmountText = valStr },
                                        label = { Text(labelStr, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OnChatViolet,
                                            selectedLabelColor = Color.White,
                                            containerColor = CashPayNavyDark,
                                            labelColor = CashPayGrey
                                        )
                                    )
                                }
                            }

                            if (transferErrorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = transferErrorMessage!!,
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        2 -> {
                            // --- Étape 2 : Page de visualisation du transfert (Preview) ---
                            val preview = transferPreviewResult
                            val isInsufficient = preview == null || !preview.success || preview.isInsufficientBalance

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                                border = BorderStroke(1.dp, if (isInsufficient) Color(0xFFFF5252) else OnChatCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Destinataire
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Destinataire :", color = CashPayGrey, fontSize = 12.sp)
                                        Text(
                                            text = if (transferCurrency.equals("BTC", ignoreCase = true)) {
                                                recipientBtcAddress ?: "Inconnu"
                                            } else {
                                                recipientWalletId ?: conv?.contactWalletId ?: "Inconnu"
                                            },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Montant
                                    val amt = preview?.resolvedAmount ?: (transferAmountText.toDoubleOrNull() ?: 0.0)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Montant :", color = CashPayGrey, fontSize = 12.sp)
                                        Text(
                                            text = "$amt $transferCurrency",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Frais
                                    val fee = preview?.resolvedFee ?: 0.0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Frais :", color = CashPayGrey, fontSize = 12.sp)
                                        Text(
                                            text = "$fee $transferCurrency",
                                            color = OnChatCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Divider(color = CashPayNavyBorder)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Total à débiter
                                    val total = preview?.resolvedTotalDebit ?: (amt + fee)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total à débiter :", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "$total $transferCurrency",
                                            color = if (isInsufficient) Color(0xFFFF5252) else Color(0xFF25D366),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isInsufficient) {
                                // Cas solde insuffisant
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1D1D)),
                                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Solde insuffisant",
                                                color = Color(0xFFFF5252),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val detailMsg = preview?.message ?: preview?.error ?: "Le solde de votre compte ne permet pas de couvrir le montant et les frais de cette transaction."
                                        Text(
                                            text = detailMsg,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ℹ️ Aucune transaction n'est créée. Le code PIN n'est pas demandé.",
                                            color = CashPayGrey,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                // Cas solde suffisant
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1B3B2B),
                                    border = BorderStroke(1.dp, Color(0xFF25D366)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Solde disponible suffisant",
                                            color = Color(0xFF25D366),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // --- Étape 3 : Confirmation avec PIN ---
                            val preview = transferPreviewResult
                            val amt = preview?.resolvedAmount ?: (transferAmountText.toDoubleOrNull() ?: 0.0)
                            val fee = preview?.resolvedFee ?: 0.0
                            val total = preview?.resolvedTotalDebit ?: (amt + fee)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                                border = BorderStroke(1.dp, CashPayNavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Montant :", color = CashPayGrey, fontSize = 12.sp)
                                        Text("$amt $transferCurrency", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Frais :", color = CashPayGrey, fontSize = 12.sp)
                                        Text("$fee $transferCurrency", color = OnChatCyan, fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total :", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("$total $transferCurrency", color = Color(0xFF25D366), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Confirmer avec votre PIN :",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = transferPin,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                        transferPin = it
                                        transferErrorMessage = null
                                    }
                                },
                                placeholder = { Text("• • • •", color = CashPayGrey, fontSize = 18.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OnChatCyan,
                                    unfocusedBorderColor = CashPayNavyBorder,
                                    cursorColor = OnChatCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Visual dot indicator
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (1..4).forEach { idx ->
                                    val isFilled = transferPin.length >= idx
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) OnChatCyan else CashPayNavyDark)
                                            .then(
                                                if (!isFilled) Modifier.border(1.dp, CashPayNavyBorder, CircleShape)
                                                else Modifier
                                            )
                                    )
                                }
                            }

                            if (transferErrorMessage != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Transfert échoué",
                                            color = Color(0xFFFF5252),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = transferErrorMessage!!,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        4 -> {
                            // --- Étape 4 : Succès ---
                            val res = transferSuccessResult
                            val amt = res?.resolvedAmount ?: (transferAmountText.toDoubleOrNull() ?: 0.0)
                            val fee = res?.resolvedFee ?: (transferPreviewResult?.resolvedFee ?: 0.0)
                            val total = res?.resolvedTotalDebit ?: (amt + fee)
                            val ref = res?.resolvedReference ?: "TR-${System.currentTimeMillis()}"

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Transfert réussi",
                                    color = Color(0xFF25D366),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Votre transfert a été effectué avec succès.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                                    border = BorderStroke(1.dp, CashPayNavyBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Montant envoyé :", color = CashPayGrey, fontSize = 12.sp)
                                            Text("$amt $transferCurrency", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Frais :", color = CashPayGrey, fontSize = 12.sp)
                                            Text("$fee $transferCurrency", color = OnChatCyan, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total débité :", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$total $transferCurrency", color = Color(0xFF25D366), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        val senderNewBal = res?.transaction?.senderNewBalance
                                        if (senderNewBal != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Nouveau solde :", color = CashPayGrey, fontSize = 12.sp)
                                                Text("$senderNewBal $transferCurrency", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Divider(color = CashPayNavyBorder)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Transaction :", color = CashPayGrey, fontSize = 11.sp)
                                            Text(
                                                text = ref,
                                                color = OnChatCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                when (transferStep) {
                    1 -> {
                        Button(
                            onClick = {
                                val amount = transferAmountText.toDoubleOrNull()
                                if (amount == null || amount <= 0.0) {
                                    Toast.makeText(context, "Montant invalide", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val targetWallet = (recipientWalletId ?: conv?.contactWalletId)?.takeIf { it.isNotBlank() }
                                val targetBtc = (recipientBtcAddress ?: conv?.contactBitcoinAddress)?.takeIf { it.isNotBlank() }
                                if (conversationId.startsWith("g_")) {
                                    transferErrorMessage = "Le transfert est réservé à une conversation personnelle."
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isTransferLoading = true
                                    transferErrorMessage = null
                                    val previewRes = viewModel.previewTransfer(
                                        receiverWalletId = targetWallet,
                                        recipientAddress = targetBtc ?: targetWallet,
                                        amount = amount,
                                        currency = transferCurrency
                                    )
                                    isTransferLoading = false
                                    if (previewRes.isSuccess) {
                                        transferPreviewResult = previewRes.getOrNull()
                                        transferStep = 2
                                    } else {
                                        transferErrorMessage = previewRes.exceptionOrNull()?.message ?: "Erreur de prévisualisation"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                            enabled = !isTransferLoading
                        ) {
                            if (isTransferLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CashPayNavyDark, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Suivant", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = CashPayNavyDark, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    2 -> {
                        val isInsufficient = transferPreviewResult == null ||
                                !transferPreviewResult!!.success ||
                                transferPreviewResult!!.isInsufficientBalance

                        if (isInsufficient) {
                            // En cas de solde insuffisant : aucun PIN demandé, juste possibilité de modifier le montant
                            Button(
                                onClick = {
                                    transferStep = 1
                                    transferErrorMessage = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan)
                            ) {
                                Text("Modifier le montant", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Solde suffisant : bouton confirmer pour aller vers saisie PIN
                            Button(
                                onClick = {
                                    transferStep = 3
                                    transferErrorMessage = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Confirmer le transfert", color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    3 -> {
                        Button(
                            onClick = {
                                if (transferPin.length < 4) {
                                    Toast.makeText(context, "Entrez votre code PIN (4 chiffres)", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val amount = transferPreviewResult?.resolvedAmount ?: (transferAmountText.toDoubleOrNull() ?: 10.0)
                                isTransferLoading = true
                                transferErrorMessage = null
                                coroutineScope.launch {
                                    val result = viewModel.executeInChatTransfer(
                                        conversationId = conversationId,
                                        amount = amount,
                                        currency = transferCurrency,
                                        pin = transferPin,
                                        receiverWalletId = recipientWalletId ?: conv?.contactWalletId,
                                        recipientAddress = recipientBtcAddress ?: conv?.contactBitcoinAddress,
                                        note = "Transfert CashPay"
                                    )
                                    isTransferLoading = false
                                    if (result.isSuccess) {
                                        val body = result.getOrNull()
                                        if (body != null && body.isTransferSuccess) {
                                            transferSuccessResult = body
                                            transferStep = 4
                                            viewModel.sendMessage(
                                                "💸 Transfert CashPay Réussi ($amount $transferCurrency)",
                                                type = "in_chat_transfer",
                                                metadata = mapOf(
                                                    "amount" to amount.toString(),
                                                    "currency" to transferCurrency,
                                                    "reference" to (body.resolvedReference ?: "")
                                                )
                                            )
                                        } else {
                                            transferErrorMessage = body?.error ?: body?.message ?: "Code PIN incorrect."
                                        }
                                    } else {
                                        transferErrorMessage = result.exceptionOrNull()?.message ?: "Code PIN incorrect."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            enabled = !isTransferLoading
                        ) {
                            if (isTransferLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Confirmer", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    4 -> {
                        Button(
                            onClick = {
                                showTransferModal = false
                                transferStep = 1
                                transferPin = ""
                                transferPreviewResult = null
                                transferSuccessResult = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan)
                        ) {
                            Text("Terminer", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                when (transferStep) {
                    2 -> {
                        TextButton(
                            onClick = { transferStep = 1 }
                        ) {
                            Text("Modifier", color = CashPayGrey)
                        }
                    }
                    3 -> {
                        TextButton(
                            onClick = { transferStep = 2 }
                        ) {
                            Text("Retour", color = CashPayGrey)
                        }
                    }
                    4 -> {
                        // Pas de bouton retour après succès
                    }
                    else -> {
                        TextButton(
                            onClick = { showTransferModal = false },
                            enabled = !isTransferLoading
                        ) {
                            Text("Annuler", color = CashPayGrey)
                        }
                    }
                }
            },
            containerColor = CashPayNavySurface
        )
    }

    // Context Menu for Edit / Delete Message
    if (selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("Options du message", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "\"${targetMsg.content}\"",
                        color = CashPayGrey,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            editContentText = targetMsg.content
                            messageIdToEdit = targetMsg.id
                            showEditDialog = true
                            selectedMessageForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CashPayNavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = OnChatCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifier mon message", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.deleteMessage(targetMsg.id)
                            selectedMessageForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B1818)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supprimer pour tous", color = Color.White)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedMessageForAction = null }) {
                    Text("Annuler", color = OnChatCyan)
                }
            },
            containerColor = CashPayNavySurface
        )
    }

    // Edit Message Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modifier le message", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editContentText,
                    onValueChange = { editContentText = it },
                    label = { Text("Nouveau contenu", color = CashPayGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (messageIdToEdit != null) {
                            viewModel.editMessage(messageIdToEdit!!, editContentText)
                        }
                        showEditDialog = false
                        messageIdToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler", color = CashPayGrey)
                }
            },
            containerColor = CashPayNavySurface
        )
    }
}

@Composable
fun MessageBubbleItem(
    message: Message,
    myAvatar: String?,
    contactAvatar: String? = null,
    onImageClick: (imageUrl: String, fileName: String?) -> Unit = { _, _ -> },
    onJoinMeeting: (meetCode: String) -> Unit = {},
    onLongPress: () -> Unit
) {
    val attachmentContext = LocalContext.current
    val attachmentScope = rememberCoroutineScope()
    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    val meetMatch = remember(message.content) {
        Regex("""(?:feedback/meet/|meet/)([a-zA-Z0-9\-_]+)""").find(message.content)
    }
    val isMeetingMessage = meetMatch != null || message.type == "meeting"
    val extractedMeetCode = meetMatch?.groupValues?.getOrNull(1) ?: message.metadata?.get("meetCode") ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isMine) {
            // Correspondent Avatar on the Left
            AvatarBadge(
                name = message.senderName,
                avatarUrl = message.senderAvatar?.takeIf { it.isNotBlank() } ?: contactAvatar,
                size = 34.dp,
                isGroup = false
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier.widthIn(max = 280.dp),
            contentAlignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            when {
                // Interactive Call & Meeting Invitation Card (Screenshot 2)
                isMeetingMessage && extractedMeetCode.isNotBlank() -> {
                    MeetingInviteBubble(
                        message = message,
                        meetCode = extractedMeetCode,
                        timeFormatted = timeFormatted,
                        onJoinMeeting = onJoinMeeting
                    )
                }

                message.type == "in_chat_transfer" || message.transferAmount != null -> {
                    // Dedicated In-Chat Transfer Card (CashPay All)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface.copy(alpha = 0.9f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(OnChatCyan, OnChatViolet))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(OnChatCyan.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = OnChatCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Transfert CashPay",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "RÉUSSI",
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val amountDisplay = message.transferAmount ?: message.metadata?.get("amount")?.toDoubleOrNull() ?: 0.0
                            val currencyDisplay = message.transferCurrency ?: message.metadata?.get("currency") ?: "USD"
                            Text(
                                text = String.format(Locale.US, "%,.2f %s", amountDisplay, currencyDisplay),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            val ref = message.transferReference ?: message.metadata?.get("reference") ?: "TR${message.id}"
                            Text(
                                text = "Réf : $ref",
                                color = CashPayGrey,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transaction certifiée",
                                    color = OnChatCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = timeFormatted,
                                    color = CashPayGrey,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Telegram-style Voice Message Waveform Bubble (Screenshot 1)
                message.type == "audio" -> {
                    VoiceMessageBubble(message = message, timeFormatted = timeFormatted)
                }

                // Interactive Location Bubble (Screenshot 2)
                message.type == "location" -> {
                    LocationCardBubble(message = message, timeFormatted = timeFormatted)
                }

                // Expressive Sticker Bubble
                message.type == "sticker" -> {
                    StickerBubble(message = message, timeFormatted = timeFormatted)
                }

                // Contact Card Bubble
                message.type == "contact" -> {
                    ContactCardBubble(message = message, timeFormatted = timeFormatted)
                }

                // Poll Card Bubble
                message.type == "poll" -> {
                    PollCardBubble(message = message, timeFormatted = timeFormatted)
                }

                // Standard Text / Image / Document Bubble
                else -> {
                    val rawUrl = message.fileUrl ?: if (message.content.startsWith("http")) message.content else null
                    val fileNameLower = (message.fileName ?: message.content).lowercase()
                    val isImageFile = message.type.equals("image", ignoreCase = true) ||
                            fileNameLower.endsWith(".png") ||
                            fileNameLower.endsWith(".jpg") ||
                            fileNameLower.endsWith(".jpeg") ||
                            fileNameLower.endsWith(".webp") ||
                            fileNameLower.endsWith(".gif") ||
                            fileNameLower.contains(".png ") ||
                            fileNameLower.contains(".jpg ") ||
                            fileNameLower.contains(".jpeg ")

                    val resolvedImageUrl = when {
                        rawUrl.isNullOrBlank() -> null
                        rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                        rawUrl.startsWith("content://") || rawUrl.startsWith("file://") -> rawUrl
                        rawUrl.startsWith("/") -> "https://app.cashpay-all.com$rawUrl"
                        rawUrl.contains("uploads/") -> "https://app.cashpay-all.com/${rawUrl.substringAfter("uploads/")}"
                        else -> "https://app.cashpay-all.com/$rawUrl"
                    }

                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (message.isMine) 16.dp else 2.dp,
                                    bottomEnd = if (message.isMine) 2.dp else 16.dp
                                )
                            )
                            .then(
                                if (message.isMine) Modifier.background(brush = OnChatBubbleGradient)
                                else Modifier.background(Color(0xFFE8F0FE)) // Light Blue for incoming
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            if (isImageFile && resolvedImageUrl != null) {
                                // Real Interactive Async Image with Zoom & Viewer Click
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onImageClick(resolvedImageUrl, message.fileName ?: message.content)
                                        }
                                ) {
                                    var isImgLoading by remember { mutableStateOf(true) }
                                    var isImgError by remember { mutableStateOf(false) }

                                    if (isImgLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .background(CashPayNavyDark.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = OnChatCyan,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }

                                    if (isImgError) {
                                        Surface(
                                            color = CashPayNavyDark.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = message.fileName ?: "Photo partagée",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Cliquer pour ouvrir",
                                                    color = OnChatCyan,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(resolvedImageUrl)
                                                .apply { com.example.data.api.AuthenticatedMedia.headers(resolvedImageUrl.orEmpty()).forEach { (name, value) -> addHeader(name, value) } }
                                                .memoryCacheKey("${com.example.data.api.ApiClient.activeWalletId}:$resolvedImageUrl")
                                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                                .crossfade(true)
                                                .listener(
                                                    onStart = { isImgLoading = true; isImgError = false },
                                                    onSuccess = { _, _ -> isImgLoading = false; isImgError = false },
                                                    onError = { _, _ -> isImgLoading = false; isImgError = true }
                                                )
                                                .build(),
                                            contentDescription = "Photo",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 160.dp, max = 280.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                if (message.content.isNotBlank() && !message.content.startsWith("http") && message.content != "🖼️ Photo") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = message.content,
                                        color = if (message.isMine) Color.White else Color.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            } else if (message.type == "doc" || (!message.fileUrl.isNullOrBlank() && !isImageFile)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp).clickable(enabled = !resolvedImageUrl.isNullOrBlank()) {
                                        attachmentScope.launch {
                                            try { com.example.media.AttachmentOpener.open(attachmentContext, resolvedImageUrl!!) }
                                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                            catch (e: Exception) { android.widget.Toast.makeText(attachmentContext, e.message ?: "Impossible d'ouvrir le fichier", android.widget.Toast.LENGTH_LONG).show() }
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (message.isMine) Color.White.copy(alpha = 0.2f) else OnChatBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (message.isMine) Color.White else OnChatBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message.fileName ?: message.content,
                                        color = if (message.isMine) Color.White else Color.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    color = if (message.isMine) Color.White else Color.Black,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (message.isEdited) {
                                    Text(
                                        text = "modifié • ",
                                        color = if (message.isMine) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = timeFormatted,
                                    color = if (message.isMine) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                    fontSize = 11.sp
                                )
                                if (message.isMine) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = when (message.status.lowercase()) {
                                            "failed" -> Icons.Default.ErrorOutline
                                            "sending" -> Icons.Default.Schedule
                                            "read", "delivered" -> Icons.Default.DoneAll
                                            else -> Icons.Default.Done
                                        },
                                        contentDescription = when (message.status.lowercase()) {
                                            "failed" -> "Échec de l'envoi"
                                            "sending" -> "Envoi en cours"
                                            "read" -> "Lu"
                                            "delivered" -> "Délivré"
                                            else -> "Envoyé"
                                        },
                                        tint = when (message.status.lowercase()) {
                                            "failed" -> Color.Red
                                            "read" -> OnChatCyan
                                            else -> Color.LightGray
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (message.isMine) {
            // User Avatar on the Right
            Spacer(modifier = Modifier.width(8.dp))
            AvatarBadge(
                name = "Moi",
                avatarUrl = myAvatar,
                size = 34.dp,
                isGroup = false
            )
        }
    }
}
