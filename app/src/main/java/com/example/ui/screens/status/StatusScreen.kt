package com.example.ui.screens.status

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.data.model.StatusGroup
import com.example.data.model.OnChatStatusItem
import com.example.data.model.ProductItem
import com.example.data.model.EventItem
import com.example.data.api.ApiClient
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import coil.compose.AsyncImage
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusScreen(
    viewModel: OnChatViewModel,
    onOpenCreateStatus: () -> Unit
) {
    val statuses by viewModel.statuses.collectAsState()
    val shoppingFeed by viewModel.shoppingFeed.collectAsState()
    val eventsFeed by viewModel.eventsFeed.collectAsState()
    val monPaysPages by viewModel.monPaysPages.collectAsState()
    val monPaysFeed by viewModel.monPaysFeed.collectAsState()
    val context = LocalContext.current
    val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()

    var activeViewingStatus by remember { mutableStateOf<StatusGroup?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCompanyId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
    ) {
        if (isSkeletonLoading && statuses.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SkeletonStatusStoryBar()
                Spacer(modifier = Modifier.height(16.dp))
                repeat(5) {
                    SkeletonStatusRow()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    HorizontalStatusRail(
                        currentSessionName = currentSession?.name ?: "Moi",
                        currentSessionAvatar = currentSession?.avatarUrl,
                        statuses = statuses,
                        onMyStatusClick = { showCreateDialog = true },
                        onStatusGroupClick = { activeViewingStatus = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    JojoShoppingSection(
                        products = shoppingFeed,
                        onFollowClick = { sellerId -> viewModel.toggleFollowShop(sellerId) },
                        onOpenProduct = { url ->
                            try {
                                val fullUrl = if (url.startsWith("http")) url else "https://app.cashpay-all.com$url"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                            } catch (_: Exception) {}
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    EventsSection(
                        events = eventsFeed,
                        onOpenEvent = { url ->
                            try {
                                val fullUrl = if (url.startsWith("http")) url else "https://app.cashpay-all.com$url"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                            } catch (_: Exception) {}
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MonPaysSection(
                        pages = monPaysPages,
                        feed = monPaysFeed,
                        onFollowClick = { id -> viewModel.toggleFollowMonPaysPage(id) },
                        onOpenPage = { companyId -> selectedCompanyId = companyId }
                    )
                }
            }
        }

        // Floating Action Button for Status
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = CashPayNavySurface,
                contentColor = OnChatCyan,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Statut texte", modifier = Modifier.size(18.dp))
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = OnChatPrimaryGradient)
                    .clickable { showCreateDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Nouveau statut",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Status Story Viewer Dialog
    if (activeViewingStatus != null) {
        StatusViewerDialog(
            statusGroup = activeViewingStatus!!,
            onDismiss = { activeViewingStatus = null },
            viewModel = viewModel
        )
    }

    // Create Status Dialog (POST /api/v1/statuses)
    if (showCreateDialog) {
        CreateStatusDialog(
            onDismiss = { showCreateDialog = false },
            onPostStatus = { type, content, color, imageUri, videoUri, visibility ->
                viewModel.postStatus(
                    type = type,
                    content = content,
                    backgroundColor = color,
                    imageUri = imageUri,
                    videoUri = videoUri,
                    visibility = visibility
                )
                showCreateDialog = false
            }
        )
    }

    // MonPays Company Profile Dialog
    if (selectedCompanyId != null) {
        MonPaysCompanyProfileDialog(
            companyId = selectedCompanyId!!,
            viewModel = viewModel,
            onDismiss = { selectedCompanyId = null }
        )
    }
}

@Composable
fun StatusGroupRow(
    statusGroup: StatusGroup,
    onClick: () -> Unit
) {
    val timeFormatted = remember(statusGroup.lastUpdated) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(statusGroup.lastUpdated))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status avatar with colorful ring
        AvatarBadge(
            name = statusGroup.userName,
            avatarUrl = statusGroup.userAvatar,
            size = 54.dp,
            hasStatus = true
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusGroup.userName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aujourd'hui à $timeFormatted",
                color = CashPayGrey,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StatusViewerDialog(
    statusGroup: StatusGroup,
    onDismiss: () -> Unit,
    viewModel: OnChatViewModel? = null
) {
    var currentIndex by remember { mutableStateOf(0) }
    val statuses = statusGroup.statuses
    val currentStatus = statuses.getOrNull(currentIndex) ?: statuses.firstOrNull()

    var isLikedLocally by remember(currentStatus?.id) { mutableStateOf(currentStatus?.isLiked ?: false) }
    var likesCountLocally by remember(currentStatus?.id) { mutableStateOf(currentStatus?.likesCount ?: 0) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Record view when status is displayed
    LaunchedEffect(currentStatus?.id) {
        currentStatus?.id?.let { statusId ->
            viewModel?.recordStatusView(statusId)
        }
    }

    // Auto-advance timer (5 seconds for text/image, or video length)
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(currentIndex, isPaused) {
        if (isPaused) return@LaunchedEffect
        
        progress = 0f
        val durationMs = if (currentStatus?.type == "video") 15000L else 5000L
        val interval = 50L
        val steps = (durationMs / interval).toInt()
        for (i in 1..steps) {
            if (isPaused) break
            delay(interval)
            progress = i / steps.toFloat()
        }
        
        if (!isPaused) {
            if (currentIndex < statuses.size - 1) {
                currentIndex++
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val bgHex = currentStatus?.backgroundColor ?: "#000E38"
        val bgColor = remember(bgHex) {
            try {
                Color(android.graphics.Color.parseColor(bgHex))
            } catch (_: Exception) {
                CashPayNavy
            }
        }

        val session = viewModel?.currentSession?.collectAsState()?.value
        val isSelfStatus = statusGroup.userId == session?.userId ||
                statusGroup.userName.lowercase().contains("moi") ||
                statusGroup.userName.lowercase().contains("mon statut")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 3) {
                                // Tap left: Previous status
                                if (currentIndex > 0) currentIndex--
                            } else {
                                // Tap right: Next status
                                if (currentIndex < statuses.size - 1) currentIndex++ else onDismiss()
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Progress Bar at top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEachIndexed { index, _ ->
                        LinearProgressIndicator(
                            progress = {
                                when {
                                    index < currentIndex -> 1f
                                    index == currentIndex -> progress
                                    else -> 0f
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header with User Avatar, Name and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarBadge(name = statusGroup.userName, avatarUrl = statusGroup.userAvatar, size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusGroup.userName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val dateStr = currentStatus?.createdAt?.toLongOrNull()?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                        } ?: "Récemment"
                        Text(
                            text = "Aujourd'hui à $dateStr",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                // Middle Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val content = currentStatus?.content ?: ""
                    val mediaUrl = currentStatus?.mediaUrl
                    val type = currentStatus?.type ?: "text"
                    
                    if (type == "video" && !mediaUrl.isNullOrBlank()) {
                        StatusVideoPlayer(
                            videoUrl = mediaUrl,
                            isPaused = isPaused,
                            onVideoEnded = {
                                if (currentIndex < statuses.size - 1) currentIndex++ else onDismiss()
                            }
                        )
                    } else if (type == "image" && !mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Statut image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        // Text status
                        Text(
                            text = content,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 34.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Owner Private Insights Bar
                if (isSelfStatus && currentStatus != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { showInsightsSheet = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${currentStatus.viewsCount} vue(s)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$likesCountLocally j'aime(s)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom Reply Field & Like Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Répondre au statut...", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        enabled = false
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Like Button (❤️)
                    IconButton(
                        onClick = {
                            currentStatus?.id?.let { id ->
                                isLikedLocally = !isLikedLocally
                                if (isLikedLocally) likesCountLocally++ else if (likesCountLocally > 0) likesCountLocally--
                                viewModel?.toggleStatusLike(id, isLikedLocally)
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isLikedLocally) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "J'aime",
                            tint = if (isLikedLocally) Color.Red else Color.White
                        )
                    }
                }
            }
        }
    }

    // Owner Private Insights Dialog (Views & Likes Lists)
    if (showInsightsSheet && currentStatus != null) {
        PrivateStatusInsightsDialog(
            statusItem = currentStatus,
            onDismiss = { showInsightsSheet = false }
        )
    }
}

@Composable
fun PrivateStatusInsightsDialog(
    statusItem: OnChatStatusItem,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Vues, 1: Likes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Statistiques privées du statut",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CashPayNavyDark)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) OnChatViolet else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👁 ${statusItem.viewsCount} Vues", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) OnChatViolet else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("❤️ ${statusItem.likesCount} Likes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val list = if (selectedTab == 0) statusItem.viewers else statusItem.likers
                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Aucune vue enregistrée pour le moment" else "Aucun j'aime pour le moment",
                            color = CashPayGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(list) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarBadge(name = user.name, avatarUrl = user.avatar, size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = user.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "Vu récemments", color = CashPayGrey, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = OnChatCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CashPayNavySurface
    )
}

@Composable
fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onPostStatus: (type: String, content: String?, colorHex: String?, imageUri: Uri?, videoUri: Uri?, visibility: String) -> Unit
) {
    var statusType by remember { mutableStateOf("text") } // "text", "image", "video"
    var contentText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var visibility by remember { mutableStateOf("public") }
    val colors = listOf("#562EBF", "#1D8AE4", "#44BBF5", "#000E38", "#166534", "#831843")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            imageUrl = uri.toString()
            statusType = "image"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            statusType = "video"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau statut CashPay", color = Color.White) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CashPayNavyDark)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (statusType == "text") OnChatViolet else Color.Transparent)
                            .clickable { statusType = "text" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Texte", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (statusType == "image") OnChatViolet else Color.Transparent)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📷 Photo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (statusType == "video") OnChatViolet else Color.Transparent)
                            .clickable {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎥 Vidéo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (statusType == "text") {
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        placeholder = { Text("Que voulez-vous partager ?", color = CashPayGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OnChatCyan,
                            cursorColor = OnChatCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Couleur de fond :", color = CashPayGrey, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.forEach { hex ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedColor == hex) 3.dp else 1.dp,
                                        color = if (selectedColor == hex) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                } else if (statusType == "image") {
                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CashPayNavyDark)
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Aperçu photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choisir une image", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (statusType == "video") {
                    if (selectedVideoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Text("Vidéo sélectionnée", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choisir une vidéo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Visibilité :", color = CashPayGrey, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("public" to "Public", "friends" to "Amis", "private" to "Privé").forEach { (valStr, label) ->
                        FilterChip(
                            selected = visibility == valStr,
                            onClick = { visibility = valStr },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OnChatViolet,
                                selectedLabelColor = Color.White,
                                containerColor = CashPayNavyDark,
                                labelColor = CashPayGrey
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (statusType == "text" && contentText.isNotBlank()) {
                        onPostStatus("text", contentText.trim(), selectedColor, null, null, visibility)
                    } else if (statusType == "image" && selectedImageUri != null) {
                        onPostStatus("image", null, null, selectedImageUri, null, visibility)
                    } else if (statusType == "video" && selectedVideoUri != null) {
                        onPostStatus("video", null, null, null, selectedVideoUri, visibility)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                enabled = contentText.isNotBlank() || selectedImageUri != null || selectedVideoUri != null
            ) {
                Text("Publier", fontWeight = FontWeight.Bold)
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
fun JojoShoppingSection(
    products: List<ProductItem>,
    onFollowClick: (Int) -> Unit,
    onOpenProduct: (String) -> Unit
) {
    if (products.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANNONCES DE JOJO SHOPPING",
                color = OnChatCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(16.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                ProductCard(product = product, onFollowClick = onFollowClick, onOpen = onOpenProduct)
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductItem,
    onFollowClick: (Int) -> Unit,
    onOpen: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
        modifier = Modifier
            .width(180.dp)
            .clickable { onOpen(product.productUrl ?: "/shopping/${product.productId}") }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(CashPayNavyDark)
            ) {
                AsyncImage(
                    model = if (product.imageUrl?.startsWith("http") == true) product.imageUrl else "https://app.cashpay-all.com${product.imageUrl}",
                    contentDescription = product.productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.productName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.shopName ?: "Boutique"} · ${product.followerCount ?: 0} abonnés",
                    color = CashPayGrey,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${product.price} ${product.currency ?: "USD"}",
                        color = OnChatCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val isFollowing = product.isFollowing == true
                    Button(
                        onClick = {
                            val idInt = product.sellerId.toIntOrNull() ?: 12
                            onFollowClick(idInt)
                        },
                        colors = if (isFollowing) {
                            ButtonDefaults.buttonColors(
                                containerColor = OnChatViolet,
                                contentColor = Color.White
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = OnChatCyan
                            )
                        },
                        border = if (!isFollowing) androidx.compose.foundation.BorderStroke(1.dp, OnChatCyan) else null,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Abonné ✓" else "S'abonner",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventsSection(
    events: List<EventItem>,
    onOpenEvent: (String) -> Unit
) {
    if (events.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ÉVÉNEMENTS & BILLETS",
                color = CashPayOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.Event, contentDescription = null, tint = CashPayOrange, modifier = Modifier.size(16.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events) { event ->
                EventCard(event = event, onOpen = onOpenEvent)
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventItem,
    onOpen: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
        modifier = Modifier
            .width(200.dp)
            .clickable { onOpen(event.eventUrl ?: "/events/${event.eventId}") }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(CashPayNavyDark)
            ) {
                AsyncImage(
                    model = if (event.imageUrl?.startsWith("http") == true) event.imageUrl else "https://app.cashpay-all.com${event.imageUrl}",
                    contentDescription = event.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.location ?: "Kinshasa",
                    color = CashPayGrey,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "À partir de ${event.minimumTicketPrice ?: 10} ${event.currency ?: "USD"}",
                    color = CashPayOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onOpen(event.eventUrl ?: "/events/${event.eventId}") },
                    colors = ButtonDefaults.buttonColors(containerColor = CashPayOrange),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Voir l'événement", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HorizontalStatusRail(
    currentSessionName: String,
    currentSessionAvatar: String?,
    statuses: List<StatusGroup>,
    onMyStatusClick: () -> Unit,
    onStatusGroupClick: (StatusGroup) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Statut",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val fullSessionAvatar = remember(currentSessionAvatar) {
            if (currentSessionAvatar.isNullOrBlank()) null
            else if (currentSessionAvatar.startsWith("http://") || currentSessionAvatar.startsWith("https://")) currentSessionAvatar
            else if (currentSessionAvatar.startsWith("/")) "https://app.cashpay-all.com$currentSessionAvatar"
            else "https://app.cashpay-all.com/$currentSessionAvatar"
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // First item: "Mon statut"
            item {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CashPayNavySurface)
                        .clickable { onMyStatusClick() }
                ) {
                    // Background: dimmed user avatar or default
                    AsyncImage(
                        model = fullSessionAvatar ?: com.example.R.drawable.onchat_logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.45f
                    )

                    // Avatar in top-left corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AvatarBadge(
                                name = currentSessionName,
                                avatarUrl = fullSessionAvatar,
                                size = 42.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(brush = OnChatPrimaryGradient)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    // Name at bottom
                    Text(
                        text = "Mon statut",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            }

            // Other users' statuses
            items(statuses) { statusGroup ->
                val firstStatus = statusGroup.statuses.firstOrNull()
                val bgHex = firstStatus?.backgroundColor ?: "#000E38"
                val bgColor = try {
                    Color(android.graphics.Color.parseColor(bgHex))
                } catch (_: Exception) {
                    CashPayNavySurface
                }

                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onStatusGroupClick(statusGroup) }
                ) {
                    // If text status, show a preview of text
                    if (firstStatus?.type == "text") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 50.dp, start = 8.dp, end = 8.dp, bottom = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstStatus.content ?: "",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Image or Video status
                        val mediaUrl = firstStatus?.mediaUrl ?: firstStatus?.thumbnailUrl ?: ""
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    // Avatar in top-left corner with status ring
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .border(2.dp, brush = com.example.ui.theme.OnChatPrimaryGradient, shape = CircleShape)
                                .padding(2.dp)
                        ) {
                            AvatarBadge(
                                name = statusGroup.userName ?: "Contact",
                                avatarUrl = statusGroup.userAvatar,
                                size = 36.dp
                            )
                        }
                    }

                    // Name at bottom
                    Text(
                        text = statusGroup.userName ?: "Contact",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MonPaysSection(
    pages: List<com.example.data.model.MonPaysCompany>,
    feed: List<com.example.data.model.MonPaysActivity>,
    onFollowClick: (Long) -> Unit,
    onOpenPage: (Long) -> Unit
) {
    if (pages.isEmpty() && feed.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // MonPays Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PAGES MONPAYS VÉRIFIÉES",
                color = OnChatCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.Storefront, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
        }

        if (pages.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pages) { company ->
                    MonPaysPageCard(company = company, onFollowClick = onFollowClick, onOpen = onOpenPage)
                }
            }
        }

        if (feed.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ACTIVITÉS DES PAGES SUIVIES",
                color = CashPayGrey,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                feed.forEach { activity ->
                    MonPaysActivityCard(activity = activity)
                }
            }
        }
    }
}

@Composable
fun MonPaysPageCard(
    company: com.example.data.model.MonPaysCompany,
    onFollowClick: (Long) -> Unit,
    onOpen: (Long) -> Unit
) {
    val isFollowing = company.isFollowing == true
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
        modifier = Modifier
            .width(180.dp)
            .clickable { onOpen(company.id) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(CashPayNavyDark)
            ) {
                val coverUrl = company.coverPhotoUrl ?: company.logoUrl
                val fullCover = if (coverUrl.isNullOrBlank()) null
                else if (coverUrl.startsWith("http")) coverUrl
                else "https://app.cashpay-all.com$coverUrl"

                if (fullCover != null) {
                    AsyncImage(
                        model = fullCover,
                        contentDescription = company.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(OnChatViolet), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = company.displayName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${company.category ?: "Entreprise"} · ${company.followerCount ?: 0} abonnés",
                    color = CashPayGrey,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onFollowClick(company.id) },
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(containerColor = OnChatViolet, contentColor = Color.White)
                    } else {
                        ButtonDefaults.outlinedButtonColors(contentColor = OnChatCyan)
                    },
                    border = if (!isFollowing) androidx.compose.foundation.BorderStroke(1.dp, OnChatCyan) else null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Abonné ✓" else "S'abonner",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MonPaysActivityCard(activity: com.example.data.model.MonPaysActivity) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBadge(name = activity.companyName ?: "Page", avatarUrl = activity.companyLogo, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = activity.companyName ?: "Page MonPays", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = activity.createdAt ?: "Récemment", color = CashPayGrey, fontSize = 10.sp)
                }
            }
            if (!activity.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = activity.title, color = OnChatCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (!activity.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = activity.content, color = Color.White, fontSize = 12.sp)
            }
            if (!activity.mediaUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val mediaFull = if (activity.mediaUrl.startsWith("http")) activity.mediaUrl else "https://app.cashpay-all.com${activity.mediaUrl}"
                AsyncImage(
                    model = mediaFull,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun MonPaysCompanyProfileDialog(
    companyId: Long,
    viewModel: OnChatViewModel,
    onDismiss: () -> Unit
) {
    var detailResponse by remember { mutableStateOf<com.example.data.model.MonPaysDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(companyId) {
        viewModel.getMonPaysPageDetail(companyId) { res ->
            detailResponse = res
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(CashPayNavyDark)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = detailResponse?.company?.displayName ?: "Profil MonPays",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            containerColor = CashPayNavyDark
        ) { innerPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OnChatCyan)
                }
            } else {
                val company = detailResponse?.company
                val activities = detailResponse?.activities ?: emptyList()
                var isFollowingLocal by remember(company?.id) { mutableStateOf(company?.isFollowing ?: detailResponse?.isFollowing ?: false) }
                var followersCountLocal by remember(company?.id) { mutableStateOf(company?.followerCount ?: detailResponse?.followerCount ?: 0) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    item {
                        // Cover + Logo Header
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        ) {
                            val coverUrl = company?.coverPhotoUrl ?: company?.logoUrl
                            val fullCover = if (coverUrl.isNullOrBlank()) null
                            else if (coverUrl.startsWith("http")) coverUrl
                            else "https://app.cashpay-all.com$coverUrl"

                            if (fullCover != null) {
                                AsyncImage(
                                    model = fullCover,
                                    contentDescription = company?.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(OnChatViolet))
                            }

                            // Overlaid Logo
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 12.dp)
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(CashPayNavyDark)
                                    .border(2.5.dp, OnChatCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarBadge(
                                    name = company?.displayName ?: "Entreprise",
                                    avatarUrl = company?.logoUrl,
                                    size = 66.dp
                                )
                            }
                        }

                        // Company Information Block
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = company?.displayName ?: "Entreprise MonPays",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Vérifiée",
                                    tint = OnChatCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "${company?.category ?: "Commerce"} · ${company?.country ?: "CD"}",
                                color = CashPayGrey,
                                fontSize = 13.sp
                            )

                            if (!company?.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = company!!.description!!,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Follow Button & Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👥 $followersCountLocal abonnés",
                                    color = OnChatCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        if (company != null) {
                                            isFollowingLocal = !isFollowingLocal
                                            if (isFollowingLocal) followersCountLocal++ else if (followersCountLocal > 0) followersCountLocal--
                                            viewModel.toggleFollowMonPaysPage(company.id)
                                        }
                                    },
                                    colors = if (isFollowingLocal) {
                                        ButtonDefaults.buttonColors(containerColor = OnChatViolet, contentColor = Color.White)
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(contentColor = OnChatCyan)
                                    },
                                    border = if (!isFollowingLocal) androidx.compose.foundation.BorderStroke(1.dp, OnChatCyan) else null,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isFollowingLocal) "Abonné ✓" else "S'abonner",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = CashPayNavyBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "PUBLICATIONS & ACTIVITÉS",
                                color = OnChatCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (activities.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucune publication récente pour cette page.",
                                    color = CashPayGrey,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(activities) { activity ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                ActivityCardWithComments(activity = activity, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCardWithComments(
    activity: com.example.data.model.MonPaysActivity,
    viewModel: OnChatViewModel
) {
    var commentsList by remember { mutableStateOf<List<com.example.data.model.MonPaysComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var likesCount by remember { mutableIntStateOf(activity.likesCount ?: 0) }
    var isLiked by remember { mutableStateOf(activity.isLiked ?: false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBadge(name = activity.companyName ?: "Page", avatarUrl = activity.companyLogo, size = 40.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = activity.companyName ?: "Page MonPays", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = activity.createdAt ?: "Récemment", color = CashPayGrey, fontSize = 11.sp)
                }
            }

            if (!activity.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = activity.title, color = OnChatCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (!activity.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = activity.content, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
            }

            if (!activity.mediaUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val mediaFull = if (activity.mediaUrl.startsWith("http")) activity.mediaUrl else "https://app.cashpay-all.com${activity.mediaUrl}"
                AsyncImage(
                    model = mediaFull,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action bar (Likes, Comments, Shares)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable {
                        isLiked = !isLiked
                        if (isLiked) likesCount++ else if (likesCount > 0) likesCount--
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (isLiked) Color.Red else CashPayGrey,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$likesCount", color = CashPayGrey, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Comment, contentDescription = "Commentaires", tint = CashPayGrey, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${commentsList.size + (activity.commentsCount ?: 0)}", color = CashPayGrey, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = CashPayGrey, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Partager", color = CashPayGrey, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = CashPayNavyBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Local Comments List
            if (commentsList.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    commentsList.forEach { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            AvatarBadge(name = comment.userName ?: "Moi", avatarUrl = comment.userAvatar, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CashPayNavyDark)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = comment.userName ?: "Utilisateur", color = OnChatCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = comment.content, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Inline Comment Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Écrire un commentaire...", color = CashPayGrey, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.weight(1f).height(46.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            val newContent = commentText.trim()
                            val newComment = com.example.data.model.MonPaysComment(
                                postId = activity.id,
                                userId = ApiClient.currentUserId ?: 1L,
                                userName = "Moi",
                                content = newContent
                            )
                            commentsList = commentsList + newComment
                            viewModel.postMonPaysComment(activity.id, newContent) {}
                            commentText = ""
                        }
                    },
                    modifier = Modifier.clip(CircleShape).background(OnChatViolet).size(40.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Publier", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
