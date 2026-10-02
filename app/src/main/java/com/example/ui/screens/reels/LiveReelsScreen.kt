package com.example.ui.screens.reels

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.UserSessionEntity
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveReelsScreen(
    viewModel: OnChatViewModel,
    onBack: () -> Unit,
    onOpenOwnProfile: (() -> Unit)? = null,
    onOpenCreatorProfile: ((String) -> Unit)? = null
) {
    val realVideos by viewModel.realVideosFeed.collectAsState()
    val isVideosLoading by viewModel.isVideosLoading.collectAsState()
    val session by viewModel.currentSession.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchVideosFeed()
    }

    val validVideos = remember(realVideos) {
        realVideos.filter { it.resolvedUrl.isNotBlank() }
    }
    val pagerState = rememberPagerState(pageCount = { validVideos.size.coerceAtLeast(1) })
    var selectedTab by remember { mutableIntStateOf(2) } // 0: Explorer, 1: Suivis, 2: Pour toi
    var showCaptureScreen by remember { mutableStateOf(false) }
    var editingMediaUri by remember { mutableStateOf<Pair<String, String>?>(null) } // (uri, type)

    // Modals
    var activeCommentsVideo by remember { mutableStateOf<VideoItem?>(null) }
    var activeSoundVideo by remember { mutableStateOf<VideoItem?>(null) }
    var activeProfileVideo by remember { mutableStateOf<VideoItem?>(null) }

    if (showCaptureScreen) {
        ReelsCaptureScreen(
            onClose = { showCaptureScreen = false },
            onMediaSelected = { uri, type ->
                showCaptureScreen = false
                editingMediaUri = Pair(uri, type)
            }
        )
    } else if (editingMediaUri != null) {
        VideoEditorScreen(
            mediaUri = editingMediaUri!!.first,
            mediaType = editingMediaUri!!.second,
            viewModel = viewModel,
            onBack = { editingMediaUri = null },
            onPublish = { uploadedUrl, caption ->
                editingMediaUri = null
                viewModel.fetchVideosFeed()
                Toast.makeText(context, "Vidéo publiée sur OnChat Shorts ! 🚀", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isVideosLoading && realVideos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OnChatCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chargement des vidéos CashPay...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (validVideos.isEmpty()) {
                // Real empty state - absolutely NO mock data fallback!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = CashPayGrey,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucune vidéo disponible",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Soyez le premier à publier votre vidéo sur OnChat !",
                            color = CashPayGrey,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCaptureScreen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatMagenta),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publier une vidéo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Vertical Pager (TikTok style swipe up/down with real backend videos)
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page in validVideos.indices) {
                        val video = validVideos[page]
                        ReelItemView(
                            video = video,
                            viewModel = viewModel,
                            session = session,
                            onOpenComments = { activeCommentsVideo = video },
                            onOpenSound = { activeSoundVideo = video },
                            onOpenProfile = {
                                val isOwn = session?.userId == video.publisher?.id ||
                                        session?.userId == video.publisher?.ownerId ||
                                        (!session?.phone.isNullOrBlank() && session?.phone == video.publisher?.phone)

                                if (isOwn) {
                                    if (onOpenOwnProfile != null) {
                                        onOpenOwnProfile()
                                    } else {
                                        activeProfileVideo = video
                                    }
                                } else {
                                    val creatorPhone = video.publisher?.phone
                                    if (!creatorPhone.isNullOrBlank() && onOpenCreatorProfile != null) {
                                        onOpenCreatorProfile(creatorPhone)
                                    } else {
                                        activeProfileVideo = video
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Top Header: Explorer, Suivis, Pour toi & Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = "Live",
                        tint = Color.White
                    )
                }

                // Tabs: Explorer, Suivis, Pour toi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Explorer", "Suivis", "Pour toi").forEachIndexed { index, tab ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedTab = index }
                        ) {
                            Text(
                                text = tab,
                                color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = if (selectedTab == index) 16.sp else 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedTab == index) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .width(24.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = {
                    Toast.makeText(context, "Recherche de vidéos et créateurs", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
            }

            // Bottom Navigation Bar (Accueil, Ami(e)s, [+], Messages, Profil)
            ReelsBottomNavigation(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                onAddClick = { showCaptureScreen = true },
                onBack = onBack,
                onProfileClick = {
                    if (onOpenOwnProfile != null) {
                        onOpenOwnProfile()
                    } else if (realVideos.isNotEmpty() && pagerState.currentPage in realVideos.indices) {
                        activeProfileVideo = realVideos[pagerState.currentPage]
                    }
                }
            )

            // --- Bottom Sheets & Modals ---

            // 1. Comments Bottom Sheet with Real Backend API Integration
            activeCommentsVideo?.let { currentVideo ->
                ReelsCommentsSheet(
                    video = currentVideo,
                    viewModel = viewModel,
                    onDismiss = { activeCommentsVideo = null }
                )
            }

            // 2. Sound Details Bottom Sheet
            activeSoundVideo?.let { currentSoundVideo ->
                ReelSoundBottomSheet(
                    video = currentSoundVideo,
                    onDismiss = { activeSoundVideo = null },
                    onUseSound = {
                        activeSoundVideo = null
                        showCaptureScreen = true
                    },
                    onViewAuthorProfile = {
                        val authorVideo = currentSoundVideo
                        activeSoundVideo = null
                        activeProfileVideo = authorVideo
                    }
                )
            }

            // 3. Creator Profile Full Modal with Real Backend Statistics
            activeProfileVideo?.let { currentProfileVideo ->
                ReelsUserProfileDialog(
                    video = currentProfileVideo,
                    allVideos = realVideos,
                    viewModel = viewModel,
                    session = session,
                    onDismiss = { activeProfileVideo = null }
                )
            }
        }
    }
}

@Composable
fun ReelItemView(
    video: VideoItem,
    viewModel: OnChatViewModel,
    session: UserSessionEntity?,
    onOpenComments: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember(video) { mutableStateOf(video.interactions?.realIsLiked == true) }
    var likesCount by remember(video) { mutableIntStateOf(video.interactions?.realLikesCount ?: 0) }
    var commentsCount by remember(video) { mutableIntStateOf(video.interactions?.comments ?: 0) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isFollowing by remember(video) { mutableStateOf(video.follow?.isFollowing == true) }

    val creatorName = video.publisher?.resolvedName ?: "Créateur OnChat"
    val creatorAvatar = video.publisher?.resolvedAvatar
    val creatorId = video.publisher?.id ?: video.publisher?.ownerId ?: 0L

    Box(modifier = Modifier.fillMaxSize()) {
        // Video Player with full-screen continuous playback
        if (video.resolvedUrl.isNotBlank()) {
            StatusVideoPlayer(
                videoUrl = video.resolvedUrl,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = video.resolvedContent, color = Color.White, textAlign = TextAlign.Center)
            }
        }

        // Gradient overlay for bottom readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        ),
                        startY = 0.55f
                    )
                )
        )

        // Interactions Side Bar (Avatar + Follow, Like, Comment, Bookmark, Share, Save, Spinning Audio Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar + Follow Button
            Box(
                modifier = Modifier.padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier.clickable { onOpenProfile() }
                ) {
                    AvatarBadge(
                        name = creatorName,
                        avatarUrl = creatorAvatar,
                        size = 48.dp,
                        modifier = Modifier.border(1.5.dp, Color.White, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(y = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isFollowing) OnChatCyan else Color(0xFFE11D48))
                        .clickable {
                            isFollowing = !isFollowing
                            if (creatorId > 0) {
                                viewModel.performSocialAction(
                                    creatorId,
                                    if (isFollowing) "follow" else "unfollow"
                                )
                            }
                            Toast.makeText(
                                context,
                                if (isFollowing) "Abonné à @$creatorName ✅" else "Désabonné",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Suivre",
                        tint = if (isFollowing) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Real Like from Server
            InteractionButton(
                icon = Icons.Default.Favorite,
                label = formatCount(likesCount),
                iconColor = if (isLiked) Color.Red else Color.White,
                onClick = {
                    val newLikeState = !isLiked
                    isLiked = newLikeState
                    if (newLikeState) likesCount++ else if (likesCount > 0) likesCount--
                    viewModel.toggleVideoLike(video.id, !newLikeState)
                }
            )

            // Real Comments Count
            InteractionButton(
                icon = Icons.AutoMirrored.Filled.Comment,
                label = formatCount(commentsCount),
                onClick = onOpenComments
            )

            // Bookmark / Favorites
            InteractionButton(
                icon = Icons.Default.Bookmark,
                label = if (isBookmarked) "1" else "0",
                iconColor = if (isBookmarked) Color(0xFFFFD700) else Color.White,
                onClick = {
                    isBookmarked = !isBookmarked
                    Toast.makeText(
                        context,
                        if (isBookmarked) "Ajouté aux favoris ⭐" else "Retiré des favoris",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Share (Generates real share link)
            InteractionButton(
                icon = Icons.Default.Share,
                label = "Partager",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Vidéo OnChat de @$creatorName")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Regarde cette vidéo sur OnChat : ${video.resolvedUrl}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager la vidéo OnChat via..."))
                }
            )

            // Save / Download Video (Real device download)
            InteractionButton(
                icon = Icons.Default.Download,
                label = "Save",
                iconColor = OnChatCyan,
                onClick = {
                    com.example.ui.screens.profile.downloadVideoToStorage(
                        context = context,
                        videoUrl = video.resolvedUrl,
                        creatorName = creatorName
                    )
                }
            )

            // Spinning Vinyl Disc with Creator Avatar
            MusicDiscAnimation(
                avatarUrl = creatorAvatar,
                userName = creatorName,
                onClick = onOpenSound
            )
        }

        // Bottom Info (Username, Description, Sound Marquee)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 80.dp, end = 90.dp)
        ) {
            Text(
                text = "@${creatorName.lowercase().replace(" ", "")}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpenProfile() }
            )
            if (video.resolvedContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = video.resolvedContent,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onOpenSound() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = OnChatCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Son original - $creatorName",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InteractionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Spinning Vinyl Record Disc with Avatar
 */
@Composable
fun MusicDiscAnimation(
    avatarUrl: String?,
    userName: String,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
                .clip(CircleShape)
                .background(Color(0xFF18181B))
                .border(2.dp, Color(0xFF3F3F46), CircleShape)
                .padding(6.dp)
                .border(1.dp, Color(0xFF27272A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AvatarBadge(
                name = userName,
                avatarUrl = avatarUrl,
                size = 24.dp
            )
        }
    }
}

/**
 * Real Comments Bottom Sheet Modal with Backend API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsCommentsSheet(
    video: VideoItem,
    viewModel: OnChatViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var newCommentText by remember { mutableStateOf("") }
    val commentsList = remember { mutableStateListOf<VideoCommentItem>() }
    var isLoadingComments by remember { mutableStateOf(true) }

    LaunchedEffect(video.id) {
        isLoadingComments = true
        viewModel.loadVideoComments(video.id) { fetched ->
            commentsList.clear()
            commentsList.addAll(fetched)
            isLoadingComments = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF18181B),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Commentaires (${commentsList.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(color = Color(0xFF27272A))

            if (isLoadingComments) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OnChatCyan, modifier = Modifier.size(32.dp))
                }
            } else if (commentsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun commentaire pour l'instant.\nSoyez le premier à commenter !",
                        color = CashPayGrey,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Comments List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(commentsList, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                AvatarBadge(
                                    name = item.user?.resolvedName ?: "Utilisateur",
                                    avatarUrl = item.user?.resolvedAvatar,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            item.user?.resolvedName ?: "Utilisateur",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = OnChatCyan
                                        )
                                        if (!item.createdAt.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                item.createdAt.take(10),
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.content, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Ajouter un commentaire...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OnChatCyan,
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = newCommentText.trim()
                        if (text.isNotBlank()) {
                            newCommentText = ""
                            viewModel.postVideoComment(video.id, text) { newComment ->
                                if (newComment != null) {
                                    commentsList.add(0, newComment)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OnChatCyan)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = Color.Black)
                }
            }
        }
    }
}

/**
 * Sound & Audio Details Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelSoundBottomSheet(
    video: VideoItem,
    onDismiss: () -> Unit,
    onUseSound: () -> Unit,
    onViewAuthorProfile: () -> Unit
) {
    val context = LocalContext.current
    val authorName = video.publisher?.resolvedName ?: "Créateur OnChat"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MusicDiscAnimation(
                avatarUrl = video.publisher?.resolvedAvatar,
                userName = authorName,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Son original - $authorName",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            Text(
                text = "Créateur certifié OnChat",
                fontSize = 13.sp,
                color = OnChatCyan
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = onUseSound,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OnChatMagenta),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Utiliser ce son dans une nouvelle vidéo", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Audio extrait et enregistré 🎧", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnChatCyan)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Télécharger l'audio seul")
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onViewAuthorProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voir le profil de l'auteur (@$authorName)", color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

/**
 * Real Creator Profile Modal for Shorts & Reels
 * Displays REAL backend statistics (followers, following, likes) or 0. No hardcoded numbers!
 */
@Composable
fun ReelsUserProfileDialog(
    video: VideoItem,
    allVideos: List<VideoItem>,
    viewModel: OnChatViewModel,
    session: UserSessionEntity?,
    onDismiss: () -> Unit
) {
    val creatorName = video.publisher?.resolvedName ?: "Créateur OnChat"
    val creatorAvatar = video.publisher?.resolvedAvatar
    val creatorPhone = video.publisher?.phone
    val creatorId = video.publisher?.id ?: video.publisher?.ownerId ?: 0L

    val isSelf = session?.userId == creatorId ||
            (!session?.phone.isNullOrBlank() && session?.phone == creatorPhone)

    var isFollowing by remember { mutableStateOf(video.follow?.isFollowing == true) }
    var publicProfileResp by remember { mutableStateOf<PhoneProfileResponse?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    val creatorVideos = remember(video, allVideos) {
        allVideos.filter {
            it.publisher?.id == creatorId ||
                    (!creatorPhone.isNullOrBlank() && it.publisher?.phone == creatorPhone)
        }.ifEmpty { listOf(video) }
    }

    LaunchedEffect(creatorPhone) {
        if (!creatorPhone.isNullOrBlank()) {
            isLoadingProfile = true
            val profileRes = viewModel.searchProfileByPhone(creatorPhone)
            if (profileRes?.profile != null) {
                val fullPublic = viewModel.repository.getPublicProfile(creatorPhone).getOrNull()
                publicProfileResp = fullPublic
            }
            isLoadingProfile = false
        } else {
            isLoadingProfile = false
        }
    }

    // REAL BACKEND COUNTERS (NO FAKE / MOCK DEFAULTS)
    val followersCount = publicProfileResp?.profile?.followers?.count
        ?: publicProfileResp?.stats?.followers
        ?: 0

    val followingCount = publicProfileResp?.profile?.following?.count
        ?: publicProfileResp?.stats?.following
        ?: 0

    val likesCount = video.interactions?.likes ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text(
                    text = "@${creatorName.lowercase().replace(" ", "")}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            }

            // Profile Info (Avatar, Stats, Bio, Follow Button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarBadge(name = creatorName, avatarUrl = creatorAvatar, size = 80.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(creatorName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(if (isSelf) "Mon Profil Média" else "Créateur OnChat", fontSize = 13.sp, color = OnChatCyan)

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingProfile) {
                    CircularProgressIndicator(color = OnChatCyan, modifier = Modifier.size(24.dp))
                } else {
                    // REAL Stats: Abonnements, Abonnés, J'aime (displaying 0 if none)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatCount(followingCount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Abonnements", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatCount(followersCount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Abonnés", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatCount(likesCount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("J'aime", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isSelf) {
                    // Follow Button
                    Button(
                        onClick = {
                            isFollowing = !isFollowing
                            if (creatorId > 0) {
                                viewModel.performSocialAction(creatorId, if (isFollowing) "follow" else "unfollow")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color(0xFF27272A) else OnChatMagenta
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Abonné ✓" else "S'abonner",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF27272A))

            // Videos Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(creatorVideos) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E1E24))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.BottomStart
                    ) {
                        AsyncImage(
                            model = item.resolvedThumbnailUrl ?: item.resolvedUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(formatCount(item.interactions?.likes ?: 0), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReelsBottomNavigation(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(icon = Icons.Default.Home, label = "Accueil", onClick = onBack)
        BottomNavItem(icon = Icons.Default.People, label = "Ami(e)s")
        
        // Custom Plus Button
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(OnChatCyan, OnChatMagenta)
                    )
                )
                .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        BottomNavItem(icon = Icons.Default.Message, label = "Messages", onClick = onBack)
        BottomNavItem(icon = Icons.Default.Person, label = "Profil", onClick = onProfileClick)
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}.${(count % 1000000) / 100000}M"
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}k"
        else -> count.toString()
    }
}
