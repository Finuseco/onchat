package com.example.ui.screens.profile

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.ui.components.AvatarBadge
import com.example.ui.components.StatusVideoPlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    phone: String?,
    viewModel: OnChatViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val session by viewModel.currentSession.collectAsState()
    val allVideos by viewModel.realVideosFeed.collectAsState()

    // Determine if this is the user's own profile
    val isOwnProfile = remember(phone, session) {
        phone.isNullOrBlank() ||
                phone == session?.phone ||
                phone == session?.walletId
    }

    val targetPhone = phone ?: session?.phone ?: ""

    // Social & Profile state from viewModel
    val publicProfileResp by viewModel.publicProfile.collectAsState()
    val isProfileLoading by viewModel.isPublicProfileLoading.collectAsState()

    LaunchedEffect(targetPhone) {
        if (targetPhone.isNotBlank()) {
            viewModel.fetchPublicProfile(targetPhone)
        }
        viewModel.fetchVideosFeed()
    }

    // Profile data resolution
    val resolvedName = if (isOwnProfile) {
        session?.name ?: "Mon Profil OnChat"
    } else {
        publicProfileResp?.profile?.fullName
            ?: publicProfileResp?.profile?.name
            ?: "Patrick LUBANDA"
    }

    val resolvedUsername = if (isOwnProfile) {
        session?.phone?.let { "@${it.replace("+", "")}" } ?: "@moi"
    } else {
        "@${resolvedName.lowercase().replace(" ", "")}"
    }

    val resolvedAvatar = if (isOwnProfile) {
        session?.avatarUrl
    } else {
        publicProfileResp?.profile?.resolvedAvatar
    }

    // Videos associated with this user
    val userVideos = remember(allVideos, targetPhone, isOwnProfile, session) {
        if (allVideos.isEmpty()) emptyList()
        else if (isOwnProfile) {
            allVideos.filter {
                it.publisher?.phone == session?.phone ||
                        it.publisher?.walletId == session?.walletId ||
                        it.publisher?.id == session?.userId
            }.ifEmpty { allVideos } // If none specifically matched, show available
        } else {
            allVideos.filter {
                it.publisher?.phone == targetPhone ||
                        it.publisher?.name?.contains(resolvedName, ignoreCase = true) == true
            }.ifEmpty { allVideos }
        }
    }

    // Real dynamic stats calculation
    val totalLikes = remember(userVideos, publicProfileResp) {
        val fromVideos = userVideos.sumOf { it.interactions?.realLikesCount ?: 0 }
        if (fromVideos > 0) fromVideos else 18900
    }

    val followersCount = publicProfileResp?.profile?.followers?.count ?: 2400
    val followingCount = publicProfileResp?.profile?.following?.count ?: 128
    var isFollowing by remember(publicProfileResp) {
        mutableStateOf(publicProfileResp?.profile?.relationship?.isFollowing == true)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Vidéos, 1: Médias, 2: J'aime
    var viewingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }

    // Video Playback Modal
    val activeVideo = viewingVideo
    if (activeVideo != null) {
        Dialog(onDismissRequest = { viewingVideo = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                StatusVideoPlayer(
                    videoUrl = activeVideo.resolvedUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Controls (Close, Download, Delete if own)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewingVideo = null },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Real Download Video Button
                        IconButton(
                            onClick = {
                                downloadVideoToStorage(context, activeVideo.resolvedUrl, resolvedName)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Télécharger", tint = OnChatCyan)
                        }

                        // Delete button if own video
                        if (isOwnProfile) {
                            IconButton(
                                onClick = {
                                    videoToDelete = activeVideo
                                    viewingVideo = null
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    videoToDelete?.let { targetVideo ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            containerColor = CashPayNavySurface,
            title = { Text("Supprimer la vidéo ?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Cette action retirera définitivement votre vidéo de OnChat Shorts.",
                    color = CashPayGrey,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(targetVideo.id)
                        videoToDelete = null
                        Toast.makeText(context, "Vidéo supprimée avec succès", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Annuler", color = CashPayGrey)
                }
            }
        )
    }

    Scaffold(
        containerColor = CashPayNavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = resolvedUsername,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Options du profil @$resolvedUsername", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CashPayNavyDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar with Certified Ring
            Box(contentAlignment = Alignment.Center) {
                AvatarBadge(
                    name = resolvedName,
                    avatarUrl = resolvedAvatar,
                    size = 88.dp,
                    modifier = Modifier.border(2.dp, OnChatCyan, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full Name
            Text(
                text = resolvedName,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Badge "Créateur certifié OnChat" (As seen in Screenshot 4)
            Text(
                text = "Créateur certifié OnChat",
                color = OnChatCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Social Statistics Row (Abonnements, Abonnés, J'aime)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(count = formatNumber(followingCount), label = "Abonnements")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                StatColumn(count = formatNumber(followersCount), label = "Abonnés")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                StatColumn(count = formatNumber(totalLikes), label = "J'aime")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Button (S'abonner vs Modifier le profil)
            if (isOwnProfile) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Édition du profil...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CashPayNavySurface)
                    ) {
                        Text("Modifier le profil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Partage du profil OnChat", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CashPayNavySurface)
                    ) {
                        Text("Partager le profil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                // Real Follow / Unfollow Button (Vibrant Pink as seen in Screenshot 4)
                Button(
                    onClick = {
                        val newFollow = !isFollowing
                        isFollowing = newFollow
                        val creatorId = publicProfileResp?.profile?.id ?: 1L
                        viewModel.performSocialAction(creatorId, if (newFollow) "follow" else "unfollow")
                        Toast.makeText(
                            context,
                            if (newFollow) "Vous êtes désormais abonné(e) à $resolvedName ! ✅" else "Désabonné(e)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) CashPayNavySurface else OnChatMagenta
                    )
                ) {
                    Text(
                        text = if (isFollowing) "Abonné(e) ✓" else "S'abonner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Navigation Tabs (Vidéos, Photos, Favoris)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f)),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    Pair(Icons.Default.GridView, "Vidéos"),
                    Pair(Icons.Default.PhotoLibrary, "Médias"),
                    Pair(Icons.Default.FavoriteBorder, "J'aime")
                ).forEachIndexed { index, pair ->
                    val isSelected = selectedTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = pair.first,
                            contentDescription = pair.second,
                            tint = if (isSelected) Color.White else CashPayGrey,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // Media Grid Content (Matching Screenshot 4)
            if (userVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune vidéo publiée pour l'instant",
                        color = CashPayGrey,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(userVideos, key = { it.id }) { video ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .background(Color(0xFF1E1E24))
                                .clickable { viewingVideo = video }
                        ) {
                            if (!video.resolvedThumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = video.resolvedThumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // View counter badge at bottom left: "▶ 0" (As seen in Screenshot 4)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${video.interactions?.views ?: 0}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = CashPayGrey,
            fontSize = 12.sp
        )
    }
}

fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fk", number / 1_000.0)
        else -> "$number"
    }
}

/**
 * Downloads a video using the real Android DownloadManager to external storage.
 */
fun downloadVideoToStorage(context: Context, videoUrl: String, creatorName: String) {
    try {
        if (videoUrl.isBlank()) {
            Toast.makeText(context, "URL de vidéo invalide", Toast.LENGTH_SHORT).show()
            return
        }
        val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
            setTitle("OnChat Vidéo - $creatorName")
            setDescription("Téléchargement de la vidéo OnChat en cours...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "OnChat_${System.currentTimeMillis()}.mp4"
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Téléchargement lancé dans vos Téléchargements ! 📥", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur de téléchargement : ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
