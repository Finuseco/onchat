package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.ui.components.AvatarBadge
import com.example.ui.components.StatusVideoPlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    phone: String,
    viewModel: OnChatViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val profileResp by viewModel.publicProfile.collectAsState()
    val searchedProfile by viewModel.searchedProfile.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val realVideosFeed by viewModel.realVideosFeed.collectAsState()
    val isLoading by viewModel.isPublicProfileLoading.collectAsState()
    val context = LocalContext.current

    val targetConv = remember(conversations, phone) {
        conversations.find {
            it.contactPhone == phone ||
            it.id == phone ||
            it.contactWalletId == phone ||
            it.name.equals(phone, ignoreCase = true)
        }
    }

    LaunchedEffect(phone) {
        if (phone.isNotBlank()) {
            viewModel.fetchPublicProfile(phone)
            viewModel.searchProfileByPhoneState(phone)
            viewModel.fetchVideosFeed()
        }
    }

    // Resolve user data with cascading fallbacks
    val pFromPublic = profileResp?.profile
    val pFromSearch = searchedProfile?.profile

    val targetUserId = pFromPublic?.id ?: pFromSearch?.userId ?: 0L

    val fullName = pFromPublic?.resolvedFullName
        ?: pFromSearch?.resolvedFullName
        ?: targetConv?.name
        ?: if (phone.isNotBlank()) phone else "Utilisateur OnChat"

    val avatarUrl = pFromPublic?.resolvedAvatar
        ?: pFromSearch?.displayAvatar
        ?: targetConv?.avatarUrl

    val coverUrl = pFromPublic?.resolvedCover
        ?: pFromSearch?.displayCover
        ?: targetConv?.coverUrl
        ?: "https://app.cashpay-all.com/images/default_cover.jpg"

    val profession = pFromPublic?.profession
        ?: pFromSearch?.profession
        ?: targetConv?.contactProfession
        ?: "Créateur OnChat"

    val walletId = pFromPublic?.walletId
        ?: pFromSearch?.resolvedWalletId
        ?: targetConv?.contactWalletId
        ?: ""

    val city = pFromPublic?.city ?: pFromSearch?.city ?: targetConv?.contactCity
    val country = pFromPublic?.country ?: pFromSearch?.country ?: targetConv?.contactCountry
    val isVerified = pFromPublic?.resolvedVerificationStatus == "verified" ||
            pFromSearch?.resolvedVerificationStatus == "verified" ||
            targetConv?.contactVerificationStatus == "verified"

    val relation = profileResp?.relation
    val stats = profileResp?.stats
    val media = profileResp?.media ?: emptyList()

    var isFollowing by remember(relation) {
        mutableStateOf(relation?.following == true || relation?.isFriend == true)
    }

    var selectedVideoForPlayback by remember { mutableStateOf<VideoItem?>(null) }
    var selectedMediaTab by remember { mutableIntStateOf(0) } // 0: Vidées, 1: Photos

    // Filter user's published videos from feed
    val userVideos = remember(realVideosFeed, targetUserId, phone) {
        realVideosFeed.filter { video ->
            val pub = video.publisher
            val pubId = pub?.id ?: pub?.ownerId
            val pubPhone = pub?.phone
            (targetUserId != 0L && pubId == targetUserId) || (!phone.isNullOrBlank() && pubPhone == phone)
        }
    }

    if (selectedVideoForPlayback != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            StatusVideoPlayer(
                videoUrl = selectedVideoForPlayback!!.resolvedUrl,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { selectedVideoForPlayback = null },
                modifier = Modifier
                    .padding(top = 40.dp, start = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fermer", tint = Color.White)
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = CashPayNavyDark,
            topBar = {
                TopAppBar(
                    title = { Text("Profil Public", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                // Header Cover & Avatar Overlap (Full screen top)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // Full Edge-to-Edge Cover Photo
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Couverture",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(CashPayNavy),
                        contentScale = ContentScale.Crop
                    )

                    // Subtle Dark Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        CashPayNavyDark.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    // Centered Overlapping Avatar Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        AvatarBadge(
                            name = fullName,
                            avatarUrl = avatarUrl,
                            size = 124.dp,
                            isGroup = false,
                            isOnline = true
                        )
                    }
                }

                // Info Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fullName,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Vérifié",
                                tint = OnChatCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (profession.isNotBlank()) {
                        Text(
                            text = profession,
                            color = CashPayGrey,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    val bio = pFromPublic?.bio ?: pFromSearch?.bio ?: targetConv?.contactBio
                    val activity = pFromPublic?.activityDescription ?: pFromSearch?.activityDescription ?: targetConv?.contactActivityDescription
                    
                    if (!bio.isNullOrBlank() || !activity.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CashPayNavySurface)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "À PROPOS",
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (!bio.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bio,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            if (!activity.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(if (bio.isNullOrBlank()) 8.dp else 12.dp))
                                Text(
                                    text = activity,
                                    color = CashPayGrey,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Details Section (Nationality, Language, Astro)
                    val nationality = pFromPublic?.nationality ?: pFromSearch?.nationality ?: targetConv?.contactNationality
                    val language = pFromPublic?.language ?: pFromSearch?.language ?: targetConv?.contactLanguage
                    val birthDate = pFromPublic?.resolvedBirthDate ?: targetConv?.contactBirthDate
                    
                    val astroSign = searchedProfile?.profile?.astro?.resolvedSign ?: targetConv?.contactAstroNameFr ?: targetConv?.contactAstroSign
                    val astroSymbol = searchedProfile?.profile?.astro?.resolvedSymbol ?: targetConv?.contactAstroSymbol
                    
                    if (!nationality.isNullOrBlank() || !language.isNullOrBlank() || !birthDate.isNullOrBlank() || !astroSign.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CashPayNavySurface)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "DÉTAILS",
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            
                            if (!nationality.isNullOrBlank()) {
                                DetailItem(Icons.Default.Flag, "Nationalité", nationality)
                            }
                            if (!language.isNullOrBlank()) {
                                DetailItem(Icons.Default.Language, "Langue", language)
                            }
                            if (!birthDate.isNullOrBlank()) {
                                DetailItem(Icons.Default.Cake, "Anniversaire", birthDate)
                            }
                            if (!astroSign.isNullOrBlank()) {
                                DetailItem(Icons.Default.AutoAwesome, "Signe Astro", "${astroSymbol ?: ""} $astroSign")
                            }
                        }
                    }

                    if (walletId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wallet ID: $walletId",
                            color = OnChatCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val location = listOfNotNull(city, country).filter { it.isNotBlank() }.joinToString(", ")
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
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Social Action Buttons: Icon on top, Text below ("Chat", "Appel", "S'abonner")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. CHAT BUTTON (Icon on top, "Chat" below)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel.openContactChat(
                                    name = fullName,
                                    avatarUrl = avatarUrl,
                                    phone = phone,
                                    walletId = walletId,
                                    recipientUserId = null,
                                    bitcoinAddress = null
                                )
                                onBack()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(OnChatCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 2. APPEL BUTTON (Icon on top, "Appel" below)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel.startCall(
                                    peerName = fullName,
                                    peerWalletId = walletId.ifBlank { phone },
                                    peerAvatar = avatarUrl,
                                    isVideo = false
                                )
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(OnChatViolet),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Appel",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Appel", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // 3. S'ABONNER / FOLLOW BUTTON (Icon on top, "S'abonner" / "Abonné" below)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                isFollowing = !isFollowing
                                val action = if (isFollowing) "follow" else "unfollow"
                                viewModel.performSocialAction(targetUserId, action)
                                val msg = if (isFollowing) "Abonné à $fullName ! 🔔" else "Désabonné"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isFollowing) CashPayGreen else OnChatMagenta),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = "Abonnement",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isFollowing) "Abonné" else "S'abonner",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CashPayNavySurface)
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatMetric(label = "Abonnés", count = stats?.followers ?: if (isFollowing) 1 else 0)
                        Divider(modifier = Modifier.height(28.dp).width(1.dp), color = CashPayNavyBorder)
                        StatMetric(label = "Suivis", count = stats?.following ?: 0)
                        Divider(modifier = Modifier.height(28.dp).width(1.dp), color = CashPayNavyBorder)
                        StatMetric(label = "Publications", count = userVideos.size + media.size)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Media Tabs (0: Vidéos Shorts, 1: Photos & Albums)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CashPayNavySurface, shape = RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedMediaTab == 0) OnChatVioletDeep else Color.Transparent)
                                .clickable { selectedMediaTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vidéos (${userVideos.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedMediaTab == 1) OnChatVioletDeep else Color.Transparent)
                                .clickable { selectedMediaTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Photos (${media.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media Content Grid
                    if (selectedMediaTab == 0) {
                        // Vidéos Section with Views & Comments
                        if (userVideos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CashPayNavySurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.VideocamOff, contentDescription = null, tint = CashPayGrey, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Aucune vidéo publiée par cet utilisateur", color = CashPayGrey, fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(userVideos) { video ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E24))
                                            .clickable { selectedVideoForPlayback = video }
                                    ) {
                                        AsyncImage(
                                            model = video.resolvedThumbnailUrl ?: video.resolvedUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Dark gradient for views & comments text
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                    )
                                                )
                                        )

                                        // Play Icon in center
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }

                                        // Views and Comments at bottom
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("${video.interactions?.views ?: 0}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Favorite, contentDescription = null, tint = OnChatMagenta, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("${video.interactions?.realLikesCount ?: 0}", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Photos Section
                        if (media.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CashPayNavySurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CashPayGrey, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Aucune photo disponible", color = CashPayGrey, fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(media) { item ->
                                    AsyncImage(
                                        model = item.resolvedUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
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
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, color = CashPayGrey, fontSize = 11.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun StatMetric(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = CashPayGrey, fontSize = 11.sp)
    }
}
