package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PhoneProfileResponse
import com.example.data.model.SocialMediaItem
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    phone: String,
    viewModel: OnChatViewModel,
    onBack: () -> Unit
) {
    val profileResp by viewModel.publicProfile.collectAsState()
    val isLoading by viewModel.isPublicProfileLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(phone) {
        viewModel.fetchPublicProfile(phone)
    }

    Scaffold(
        containerColor = CashPayNavyDark,
        topBar = {
            TopAppBar(
                title = { Text("Profil Public", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OnChatCyan)
            }
        } else if (profileResp?.found == true) {
            val p = profileResp?.profile
            val relation = profileResp?.relation
            val stats = profileResp?.stats
            val media = profileResp?.media ?: emptyList()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // Cover Photo
                    AsyncImage(
                        model = p?.resolvedCover ?: "https://app.cashpay-all.com/images/default_cover.jpg",
                        contentDescription = "Couverture",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(CashPayNavy),
                        contentScale = ContentScale.Crop
                    )

                    // Profile Photo Overlap
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 0.dp)
                    ) {
                        AvatarBadge(
                            name = p?.resolvedFullName ?: "Utilisateur",
                            avatarUrl = p?.resolvedAvatar,
                            size = 120.dp,
                            isGroup = false,
                            isOnline = p?.resolvedAvailabilityStatus == "available"
                        )
                    }
                }

                // Info Zone
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = p?.resolvedFullName ?: "Utilisateur CashPay",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (p?.resolvedVerificationStatus == "verified") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Vérifié",
                                tint = OnChatCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (!p?.profession.isNullOrBlank()) {
                        Text(
                            text = p!!.profession!!,
                            color = CashPayGrey,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    val location = listOfNotNull(p?.city, p?.country).joinToString(", ")
                    if (location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = CashPayGrey,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = location,
                                color = CashPayGrey,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Social Actions Row
                    SocialActionsRow(
                        userId = p?.id ?: 0L,
                        relation = relation,
                        onAction = { action, type ->
                            viewModel.performSocialAction(p?.id ?: 0L, action, type)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Abonnés", count = stats?.followers ?: 0)
                        StatItem(label = "Suivis", count = stats?.following ?: 0)
                        StatItem(label = "Amis", count = stats?.friends ?: 0)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Media Grid Section
                    Text(
                        text = "PHOTOS / MÉDIAS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    MediaGrid(
                        media = media,
                        albums = profileResp?.albums ?: emptyList(),
                        isOwnProfile = false,
                        onAddMedia = { uri ->
                             // Handle add media
                        },
                        viewModel = viewModel
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = CashPayGrey, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Profil introuvable", color = Color.White, fontSize = 18.sp)
                    Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Retour")
                    }
                }
            }
        }
    }
}

@Composable
fun SocialActionsRow(
    userId: Long,
    relation: com.example.data.model.SocialRelation?,
    onAction: (String, String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Follow / Unfollow
        val isFollowing = relation?.following == true
        SocialButton(
            text = if (isFollowing) "Abonné" else "S'abonner",
            containerColor = if (isFollowing) CashPayNavySurface else OnChatCyan,
            contentColor = if (isFollowing) Color.White else Color.Black,
            modifier = Modifier.weight(1f).testTag("follow_button"),
            onClick = { onAction(if (isFollowing) "unfollow" else "follow", null) }
        )

        // Friendship
        val friendStatus = relation?.friendshipStatus ?: "none"
        val (friendText, friendColor) = when (friendStatus) {
            "sent" -> "Demande envoyée" to CashPayNavySurface
            "received" -> "Accepter" to OnChatViolet
            "accepted" -> "Amis" to CashPayNavySurface
            else -> "Demander en ami" to CashPayNavySurface
        }
        SocialButton(
            text = friendText,
            containerColor = friendColor,
            contentColor = Color.White,
            modifier = Modifier.weight(1f),
            onClick = {
                val action = when (friendStatus) {
                    "none" -> "friend-request"
                    "sent" -> "friend-cancel"
                    "received" -> "friend-accept"
                    "accepted" -> "friend-remove"
                    else -> "friend-request"
                }
                onAction(action, null)
            }
        )
        
        // Relation Action (More menu)
        IconButton(
            onClick = { onAction("relation-request", "professional") },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CashPayNavySurface)
                .size(48.dp)
        ) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Relation", tint = Color.White)
        }
    }
}

@Composable
fun SocialButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = CashPayGrey,
            fontSize = 13.sp
        )
    }
}

@Composable
fun MediaGrid(
    media: List<SocialMediaItem>,
    albums: List<com.example.data.model.SocialAlbum>,
    isOwnProfile: Boolean,
    onAddMedia: (android.net.Uri) -> Unit,
    viewModel: OnChatViewModel
) {
    val scope = rememberCoroutineScope()
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uploadProgress = 0.1f
            viewModel.uploadProfileMedia(
                uri = uri,
                mediaType = "album_image",
                visibility = "public",
                onProgress = { p -> uploadProgress = p },
                onComplete = { success, err ->
                    if (success) {
                        uploadProgress = 1.0f
                        Toast.makeText(context, "Média ajouté avec succès !", Toast.LENGTH_SHORT).show()
                    } else {
                        uploadProgress = null
                        Toast.makeText(context, err ?: "Échec du téléversement", Toast.LENGTH_SHORT).show()
                    }
                    scope.launch {
                        delay(1000)
                        uploadProgress = null
                    }
                }
            )
        }
    }

    // Modern Responsive Grid
    val itemsPerRow = 3
    val itemSize = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp - 48) / itemsPerRow

    Column {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp
        ) {
            // "Add" Square
            Box(
                modifier = Modifier
                    .size(itemSize.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CashPayNavySurface)
                    .clickable {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uploadProgress != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = uploadProgress!!,
                            color = OnChatCyan,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(uploadProgress!! * 100).toInt()}%",
                            color = OnChatCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = CashPayGrey, modifier = Modifier.size(32.dp))
                }
            }

            // Existing Albums
            albums.forEach { album ->
                Box(
                    modifier = Modifier
                        .size(itemSize.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OnChatViolet.copy(alpha = 0.2f))
                        .border(1.dp, OnChatViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { /* Open album */ }
                ) {
                    if (!album.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f
                        )
                    }
                    
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Text(album.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${album.mediaCount} items", color = CashPayGrey, fontSize = 9.sp)
                    }
                }
            }

            // Existing Media Items
            media.forEach { item ->
                Box(
                    modifier = Modifier
                        .size(itemSize.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CashPayNavySurface)
                        .clickable { /* Open viewer */ }
                ) {
                    AsyncImage(
                        model = item.resolvedDisplayUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (item.isVideo) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Vidéo",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center).size(24.dp)
                        )
                    }
                    
                    // Like/Comment badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(item.likeCount.toString(), color = Color.White, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing),
        content = { content() }
    )
}
