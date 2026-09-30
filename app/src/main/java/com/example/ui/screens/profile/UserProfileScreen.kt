package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import coil.compose.AsyncImage
import java.util.Locale
import com.example.data.db.UserSessionEntity
import com.example.data.model.Conversation
import com.example.data.model.ProfileMediaItem
import com.example.ui.components.AvatarBadge
import com.example.ui.components.ProfileAlbumViewer
import com.example.ui.components.QrCodeModal
import com.example.ui.components.QrScannerModal
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import com.example.util.RingtoneHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    conversationId: String?,
    contactName: String?,
    contactPhone: String?,
    contactWalletId: String?,
    contactAvatarUrl: String?,
    viewModel: OnChatViewModel,
    isSelfProfile: Boolean = false,
    onBack: () -> Unit,
    onOpenChat: ((String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenPublicProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val session by viewModel.currentSession.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val searchedProfile by viewModel.searchedProfile.collectAsState()

    val targetConv = remember(conversations, conversationId) {
        conversations.find { it.id == conversationId }
    }

    LaunchedEffect(isSelfProfile, conversationId, contactPhone) {
        if (isSelfProfile) {
            viewModel.fetchAuthMe()
            viewModel.fetchProfileMedia()
            viewModel.fetchTransactions()
        } else {
            if (conversationId != null) {
                viewModel.fetchConversationDetails(conversationId)
            }
            if (contactPhone != null) {
                viewModel.searchProfileByPhoneState(contactPhone)
            }
        }
    }

    // Resolved profile attributes (enforcing full name strictly and hiding first/last name)
    val searchedP = searchedProfile?.profile
    
    val fullName = if (isSelfProfile) {
        session?.name ?: "Mon Profil"
    } else {
        contactName ?: targetConv?.name ?: searchedP?.resolvedFullName ?: "Contact OnChat"
    }

    val walletId = if (isSelfProfile) {
        session?.walletId.orEmpty()
    } else {
        contactWalletId ?: targetConv?.contactWalletId ?: searchedP?.resolvedWalletId ?: ""
    }

    val avatarUrl = if (isSelfProfile) {
        session?.avatarUrl
    } else {
        contactAvatarUrl ?: targetConv?.avatarUrl ?: searchedP?.displayAvatar
    }

    val coverUrl = if (isSelfProfile) {
        session?.coverUrl
    } else {
        targetConv?.coverUrl ?: searchedP?.displayCover
    }

    val phone = if (isSelfProfile) session?.phone else contactPhone ?: searchedProfile?.phone
    val email = if (isSelfProfile) session?.email else searchedP?.email

    val profession = if (isSelfProfile) {
        session?.profession ?: "Membre CashPay"
    } else {
        targetConv?.contactProfession ?: searchedP?.profession ?: "Membre OnChat"
    }

    val activityDescription = if (isSelfProfile) {
        session?.activityDescription?.ifBlank { session?.bio } ?: ""
    } else {
        targetConv?.contactActivityDescription?.ifBlank { targetConv?.contactBio } ?: searchedP?.resolvedActivity ?: ""
    }

    val address = if (isSelfProfile) session?.address else targetConv?.contactAddress ?: searchedP?.address
    val city = if (isSelfProfile) session?.city else targetConv?.contactCity ?: searchedP?.city
    val province = if (isSelfProfile) session?.province else targetConv?.contactProvince ?: searchedP?.province
    val country = if (isSelfProfile) session?.country else targetConv?.contactCountry ?: searchedP?.country
    val nationality = if (isSelfProfile) session?.nationality else targetConv?.contactNationality ?: searchedP?.nationality
    val language = if (isSelfProfile) session?.language else targetConv?.contactLanguage ?: searchedP?.language
    val birthDate = if (isSelfProfile) session?.birthDate else targetConv?.contactBirthDate ?: searchedP?.resolvedBirthDate
    val birthPlace = if (isSelfProfile) session?.birthPlace else null
    
    val btcAddress = if (isSelfProfile) session?.bitcoinAddress else targetConv?.contactBitcoinAddress ?: searchedP?.resolvedBitcoinAddress

    val googleId = if (isSelfProfile) session?.googleId else null
    val zoomId = if (isSelfProfile) session?.zoomId else null
    val verificationStatus = if (isSelfProfile) session?.verificationStatus else targetConv?.contactVerificationStatus ?: searchedP?.resolvedVerificationStatus
    val availabilityStatus = if (isSelfProfile) session?.availabilityStatus else targetConv?.contactAvailabilityStatus ?: searchedP?.availabilityStatus

    val astroSign = if (isSelfProfile) session?.astroSign else targetConv?.contactAstroSign ?: searchedP?.astro?.resolvedSign
    val astroSymbol = if (isSelfProfile) session?.astroSymbol else targetConv?.contactAstroSymbol ?: searchedP?.astro?.resolvedSymbol
    val astroNameFr = if (isSelfProfile) session?.astroNameFr else targetConv?.contactAstroNameFr ?: searchedP?.astro?.zodiac?.nameFr
    val astroElement = if (isSelfProfile) session?.astroElement else targetConv?.contactAstroElement ?: searchedP?.astro?.element
    val astroLuckyNumber = if (isSelfProfile) session?.astroLuckyNumber else null
    val astroAscendant = if (isSelfProfile) session?.astroAscendant else null
    val astroDescription = if (isSelfProfile) session?.astroDescription else null

    val selectedRingtoneTitle by viewModel.selectedRingtoneTitle.collectAsState()
    var isMuted by remember { mutableStateOf(targetConv?.isMuted ?: false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }
    var showRingtoneModal by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }

    val profileMedia by viewModel.profileMedia.collectAsState()
    var selectedAlbumIndex by remember { mutableStateOf<Int?>(null) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var showAvatarOptionsDialog by remember { mutableStateOf(false) }
    var showCoverOptionsDialog by remember { mutableStateOf(false) }

    val effectiveMediaList = remember(profileMedia, avatarUrl, coverUrl) {
        if (profileMedia.isNotEmpty()) {
            profileMedia
        } else {
            val list = mutableListOf<ProfileMediaItem>()
            if (!avatarUrl.isNullOrBlank()) {
                list.add(
                    ProfileMediaItem(
                        id = 1,
                        type = "profile",
                        url = avatarUrl,
                        isProfile = true
                    )
                )
            }
            if (!coverUrl.isNullOrBlank() && coverUrl != avatarUrl) {
                list.add(
                    ProfileMediaItem(
                        id = 2,
                        type = "cover",
                        url = coverUrl,
                        isCover = true
                    )
                )
            }
            list
        }
    }

    val albumPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingMedia = true
            Toast.makeText(context, "Téléversement de la photo dans l'album...", Toast.LENGTH_SHORT).show()
            viewModel.uploadProfileMedia(
                uri = uri,
                mediaType = "album_image",
                visibility = "public"
            ) { success, err ->
                isUploadingMedia = false
                if (success) {
                    Toast.makeText(context, "Photo ajoutée à l'album avec succès !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, err ?: "Échec de l'envoi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingMedia = true
            Toast.makeText(context, "Mise à jour de la photo de profil...", Toast.LENGTH_SHORT).show()
            viewModel.uploadProfileMedia(
                uri = uri,
                mediaType = "profile",
                visibility = "public"
            ) { success, err ->
                isUploadingMedia = false
                if (success) {
                    Toast.makeText(context, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, err ?: "Échec de la mise à jour", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val coverPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingMedia = true
            Toast.makeText(context, "Mise à jour de la couverture...", Toast.LENGTH_SHORT).show()
            viewModel.uploadProfileMedia(
                uri = uri,
                mediaType = "cover",
                visibility = "public"
            ) { success, err ->
                isUploadingMedia = false
                if (success) {
                    Toast.makeText(context, "Photo de couverture mise à jour !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, err ?: "Échec de la mise à jour", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copié !", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = CashPayNavyDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Header Section with Cover / Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CashPayNavySurface, CashPayNavy, CashPayNavyDark)
                        )
                    )
            ) {
                // Background cover photo or avatar backdrop
                val bgImage = coverUrl ?: avatarUrl
                if (!bgImage.isNullOrBlank()) {
                    AsyncImage(
                        model = bgImage,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CashPayNavy)
                            .clickable {
                                if (isSelfProfile) {
                                    showCoverOptionsDialog = true
                                } else {
                                    val coverIdx = effectiveMediaList.indexOfFirst { it.isCover }
                                    selectedAlbumIndex = if (coverIdx >= 0) coverIdx else 0
                                }
                            },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.40f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        CashPayNavyDark.copy(alpha = 0.85f),
                                        CashPayNavyDark
                                    )
                                )
                            )
                    )
                }

                if (isSelfProfile) {
                    IconButton(
                        onClick = { showCoverOptionsDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 56.dp, end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Changer la couverture",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Top navigation & actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }

                    Row {
                        IconButton(onClick = { 
                            showShareModal = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager",
                                tint = Color.White
                            )
                        }
                        if (isSelfProfile) {
                            IconButton(onClick = { showScannerModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scanner",
                                    tint = OnChatCyan
                                )
                            }
                        }
                        IconButton(onClick = {
                            if (isSelfProfile) {
                                showEditProfileDialog = true
                            } else {
                                // Options menu for public profile
                            }
                        }) {
                            Icon(
                                imageVector = if (isSelfProfile) Icons.Default.Edit else Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Profile Identity (Avatar + Full Name + Status badges)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.clickable {
                            if (isSelfProfile) {
                                showAvatarOptionsDialog = true
                            } else {
                                val profIdx = effectiveMediaList.indexOfFirst { it.isProfile }
                                selectedAlbumIndex = if (profIdx >= 0) profIdx else 0
                            }
                        }
                    ) {
                        AvatarBadge(
                            name = fullName,
                            avatarUrl = avatarUrl,
                            size = 84.dp,
                            isGroup = false,
                            isOnline = availabilityStatus == "available" || availabilityStatus == "en_ligne" || availabilityStatus == null
                        )
                        if (isSelfProfile) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(OnChatViolet)
                                    .border(2.dp, CashPayNavyDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Changer l'avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fullName,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (verificationStatus == "verified" || verificationStatus == "certifie") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Vérifié",
                                tint = OnChatCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Availability status badge
                        val isAvail = availabilityStatus == "available" || availabilityStatus == "en_ligne" || availabilityStatus == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAvail) Color(0xFF25D366).copy(alpha = 0.2f) else CashPayOrange.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (availabilityStatus) {
                                    "busy" -> "Occupé"
                                    "away" -> "Absent"
                                    else -> "En ligne"
                                },
                                color = if (isAvail) Color(0xFF25D366) else CashPayOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!profession.isNullOrBlank()) {
                            Text(
                                text = "• $profession",
                                color = CashPayGrey,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Action Buttons Row (Message, Call, Edit / Settings)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelfProfile) {
                        ProfileActionButton(
                            icon = Icons.Default.Chat,
                            label = "Mon Chat",
                            onClick = { onBack() }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.EditNote,
                            label = "Modifier",
                            onClick = { showEditProfileDialog = true }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.Settings,
                            label = "Paramètres",
                            onClick = { onOpenSettings?.invoke() }
                        )
                    } else {
                        ProfileActionButton(
                            icon = Icons.Default.Public,
                            label = "Social",
                            onClick = {
                                if (phone != null) {
                                    onOpenPublicProfile?.invoke(phone)
                                }
                            }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.Chat,
                            label = "Chat",
                            onClick = {
                                if (conversationId != null) {
                                    onOpenChat?.invoke(conversationId)
                                } else {
                                    viewModel.openContactChat(
                                        name = fullName,
                                        avatarUrl = avatarUrl,
                                        phone = phone,
                                        walletId = walletId.takeIf { it.isNotBlank() },
                                        bitcoinAddress = btcAddress
                                    )
                                    onBack()
                                }
                            }
                        )
                        ProfileActionButton(
                            icon = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            label = "Notifs",
                            onClick = {
                                isMuted = !isMuted
                                Toast.makeText(
                                    context,
                                    if (isMuted) "Notifications désactivées" else "Notifications activées",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.Call,
                            label = "Audio",
                            onClick = {
                                viewModel.startCall(
                                    peerName = fullName,
                                    peerWalletId = walletId,
                                    peerAvatar = avatarUrl,
                                    isVideo = false
                                )
                            }
                        )
                        ProfileActionButton(
                            icon = Icons.Default.Videocam,
                            label = "Vidéo",
                            onClick = {
                                viewModel.startCall(
                                    peerName = fullName,
                                    peerWalletId = walletId,
                                    peerAvatar = avatarUrl,
                                    isVideo = true
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ALBUM & GALERIE PHOTOS (CashPay Media API & High-End Gallery)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = OnChatCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ALBUM & GALERIE PHOTOS",
                                    color = OnChatCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${effectiveMediaList.size} photo(s) au total",
                                    color = CashPayGrey,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isSelfProfile) {
                            Button(
                                onClick = {
                                    albumPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OnChatViolet.copy(alpha = 0.35f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isUploadingMedia) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = OnChatCyan,
                            trackColor = CashPayNavyDark
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (effectiveMediaList.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            itemsIndexed(effectiveMediaList) { index, item ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 100.dp, height = 130.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CashPayNavyDark)
                                        .clickable { selectedAlbumIndex = index }
                                ) {
                                    AsyncImage(
                                        model = item.displayUrl,
                                        contentDescription = "Photo ${item.id}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(45.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                )
                                            )
                                    )

                                    if (item.isProfile) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1D8AE4))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Profil", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (item.isCover) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF562EBF))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Couverture", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CashPayNavyDark.copy(alpha = 0.6f))
                                .clickable {
                                    if (isSelfProfile) {
                                        albumPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = CashPayGrey,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isSelfProfile) "Aucune photo dans votre album." else "Aucune photo disponible.",
                                    color = CashPayGrey,
                                    fontSize = 13.sp
                                )
                                if (isSelfProfile) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Touchez ici pour ajouter votre premier cliché !",
                                        color = OnChatCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Info Section Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isSelfProfile) "COMPTE & IDENTITÉ PRIVÉE" else "PROFIL & COORDONNÉES",
                        color = OnChatCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full Name (Nom complet)
                    ProfileInfoRow(
                        title = fullName,
                        subtitle = "",
                        icon = Icons.Default.Person,
                        onCopy = { copyToClipboard("Nom complet", fullName) }
                    )

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))

                    // Wallet ID CashPay (Displayed for both now as per user request to enable transfers)
                    if (walletId.isNotBlank()) {
                        ProfileInfoRow(
                            title = walletId,
                            subtitle = "ID Wallet CashPay",
                            icon = Icons.Default.AccountBalanceWallet,
                            isHighlight = true,
                            onCopy = { copyToClipboard("Wallet ID CashPay", walletId) }
                        )
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                    }

                    // Bitcoin Address (Displayed for both, but critical for Public Profile as per user request)
                    if (!btcAddress.isNullOrBlank()) {
                        if (isSelfProfile) Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = btcAddress,
                            subtitle = "",
                            icon = Icons.Default.CurrencyBitcoin,
                            onCopy = { copyToClipboard("Adresse Bitcoin", btcAddress) }
                        )
                    }


                    // Phone & Email ONLY displayed for Private Self Profile
                    if (isSelfProfile && !phone.isNullOrBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = phone,
                            subtitle = "",
                            icon = Icons.Default.Phone,
                            onCopy = { copyToClipboard("Numéro", phone) }
                        )
                    }

                    if (isSelfProfile && !email.isNullOrBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = email,
                            subtitle = "",
                            icon = Icons.Default.Email,
                            onCopy = { copyToClipboard("Email", email) }
                        )
                    }

                    // Profession & Activity Description
                    if (!profession.isNullOrBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = profession,
                            subtitle = "",
                            icon = Icons.Default.Work,
                            onCopy = { copyToClipboard("Profession", profession) }
                        )
                    }

                    if (!activityDescription.isNullOrBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = activityDescription,
                            subtitle = "",
                            icon = Icons.Default.Description,
                            onCopy = { copyToClipboard("Activité", activityDescription) }
                        )
                    }

                    // Location / Address / Province / Country
                    val locationDetails = listOfNotNull(address, city, province, country).filter { it.isNotBlank() }.joinToString(", ")
                    if (locationDetails.isNotBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = locationDetails,
                            subtitle = "",
                            icon = Icons.Default.LocationOn,
                            onCopy = { copyToClipboard("Localisation", locationDetails) }
                        )
                    }

                    // Nationality & Language
                    val natLang = listOfNotNull(nationality, language).filter { it.isNotBlank() }.joinToString(" • ")
                    if (natLang.isNotBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = natLang,
                            subtitle = "",
                            icon = Icons.Default.Language,
                            onCopy = { copyToClipboard("Nationalité et Langue", natLang) }
                        )
                    }

                    // BirthDate & BirthPlace
                    val birthString = buildString {
                        if (!birthDate.isNullOrBlank()) {
                            append(birthDate.take(10))
                        }
                        if (isSelfProfile && !birthPlace.isNullOrBlank()) {
                            if (isNotEmpty()) append(" à ")
                            append(birthPlace)
                        }
                    }
                    if (birthString.isNotBlank()) {
                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))
                        ProfileInfoRow(
                            title = birthString,
                            subtitle = "",
                            icon = Icons.Default.Cake,
                            onCopy = { copyToClipboard("Date de naissance", birthString) }
                        )
                    }
                }
            }

            // Astro Section (if available)
            val displaySign = astroNameFr?.ifBlank { null } ?: astroSign?.ifBlank { null }
            val displaySymbol = astroSymbol?.ifBlank { null } ?: ""
            if (!displaySign.isNullOrBlank() || displaySymbol.isNotBlank() || !astroElement.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OnChatViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROFIL ASTROLOGIQUE",
                                color = OnChatViolet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!displaySign.isNullOrBlank() || displaySymbol.isNotBlank()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Signe Zodiac", color = CashPayGrey, fontSize = 11.sp)
                                    Text("$displaySymbol ${displaySign ?: ""}".trim(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!astroElement.isNullOrBlank()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Élément", color = CashPayGrey, fontSize = 11.sp)
                                    Text(astroElement, color = OnChatCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (!astroLuckyNumber.isNullOrBlank()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Chiffre chance", color = CashPayGrey, fontSize = 11.sp)
                                    Text(astroLuckyNumber, color = CashPayOrange, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (!astroDescription.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = astroDescription,
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Services & Connected Accounts (Google & Zoom) for Self Profile
            if (isSelfProfile) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "COMPTES ASSOCIÉS & SERVICES",
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Compte Google", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (!googleId.isNullOrBlank()) "Lié ($googleId)" else "Non connecté",
                                        color = if (!googleId.isNullOrBlank()) Color(0xFF25D366) else CashPayGrey,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (!googleId.isNullOrBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                            } else {
                                TextButton(onClick = { Toast.makeText(context, "Liaison Google à venir", Toast.LENGTH_SHORT).show() }) {
                                    Text("Connecter", color = OnChatCyan, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 8.dp))

                        // Zoom Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Compte Zoom", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (!zoomId.isNullOrBlank()) "Lié ($zoomId)" else "Non connecté",
                                        color = if (!zoomId.isNullOrBlank()) Color(0xFF25D366) else CashPayGrey,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (!zoomId.isNullOrBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                            } else {
                                TextButton(onClick = { Toast.makeText(context, "Liaison Zoom à venir", Toast.LENGTH_SHORT).show() }) {
                                    Text("Connecter", color = OnChatCyan, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (isSelfProfile) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SERVICES & SÉCURITÉ",
                                    color = OnChatCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Accédez à votre historique complet et générez vos reçus depuis le menu principal OnChat > Transactions.",
                            color = CashPayGrey,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shared Content & Transactions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PARTAGES & SERVICES CASHPAY",
                        color = OnChatCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SharedItemRow(
                        icon = Icons.Default.CurrencyExchange,
                        title = "Transferts financiers CashPay",
                        count = "Historique & Reçus instantanés",
                        onClick = {
                            Toast.makeText(context, "Synchronisé avec le serveur CashPay All", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))

                    SharedItemRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Sonnerie des Appels & Chime",
                        count = selectedRingtoneTitle,
                        onClick = {
                            showRingtoneModal = true
                        }
                    )

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 10.dp))

                    SharedItemRow(
                        icon = Icons.Default.Lock,
                        title = "Chiffrement et Sécurité",
                        count = "Protection de bout en bout",
                        onClick = {
                            Toast.makeText(context, "Conversations et profils sécurisés", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // QR Code Modal (Wallet ID, Bitcoin, Direct chat)
    if (showQrModal) {
        QrCodeModal(
            title = if (isSelfProfile) "Mon QR Code OnChat" else "QR Code de $fullName",
            subtitle = if (isSelfProfile) 
                "Présentez ce QR Code pour recevoir des fonds ou démarrer un chat."
                else "Scannez ce QR Code pour identifier le Wallet ID de ce contact.",
            qrData = walletId,
            walletId = walletId,
            btcAddress = if (isSelfProfile) session?.bitcoinAddress else null,
            hideChatTab = !isSelfProfile,
            onDismiss = { showQrModal = false }
        )
    }

    // QR Scanner Modal
    if (showScannerModal) {
        QrScannerModal(
            onDismiss = { showScannerModal = false },
            onScanned = { result ->
                Toast.makeText(context, "Code scanné : $result", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Custom Ringtone Selection Modal
    if (showRingtoneModal) {
        RingtoneSelectionDialog(
            currentSelected = selectedRingtoneTitle,
            onDismiss = {
                RingtoneHelper.stopCallRingtone(context)
                showRingtoneModal = false
            },
            onSelect = { title, uri ->
                viewModel.setRingtone(title, uri)
                RingtoneHelper.stopCallRingtone(context)
                showRingtoneModal = false
                Toast.makeText(context, "Sonnerie appliquée : $title", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // PIN-Protected Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileWithPinDialog(
            currentFullName = fullName,
            currentProfession = profession,
            currentActivity = activityDescription,
            currentAddress = address ?: "",
            currentCity = city ?: "",
            currentProvince = province ?: "",
            currentCountry = country ?: "",
            currentNationality = nationality ?: "",
            currentLanguage = language ?: "",
            currentBirthDate = birthDate ?: "",
            currentAvailability = availabilityStatus ?: "available",
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { pin, newFullName, newProfession, newActivity, newAddress, newCity, newProvince, newCountry, newNationality, newLanguage, newBirthDate, newAvailability ->
                viewModel.updateProfileWithPin(
                    pin = pin,
                    fullName = newFullName,
                    profession = newProfession,
                    activityDescription = newActivity,
                    address = newAddress,
                    city = newCity,
                    province = newProvince,
                    country = newCountry,
                    nationality = newNationality,
                    language = newLanguage,
                    birthDate = newBirthDate,
                    availabilityStatus = newAvailability,
                    onSuccess = {
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profil mis à jour avec succès !", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Erreur: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // Share Contact Modal
    if (showShareModal) {
        ShareContactModal(
            fullName = fullName,
            phone = phone ?: contactPhone ?: "",
            walletId = walletId,
            viewModel = viewModel,
            onDismiss = { showShareModal = false }
        )
    }

    // Fullscreen Album Viewer (Matching user's screenshot)
    selectedAlbumIndex?.let { index ->
        ProfileAlbumViewer(
            mediaList = effectiveMediaList,
            initialIndex = index,
            isSelfProfile = isSelfProfile,
            onDismiss = { selectedAlbumIndex = null },
            onSetAsProfile = { mediaId ->
                viewModel.setProfileMedia(mediaId) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, err ?: "Erreur", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSetAsCover = { mediaId ->
                viewModel.setCoverMedia(mediaId) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Couverture mise à jour !", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, err ?: "Erreur", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Avatar Options Dialog
    if (showAvatarOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarOptionsDialog = false },
            title = { Text("Photo de profil", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAvatarOptionsDialog = false
                            val profIdx = effectiveMediaList.indexOfFirst { it.isProfile }
                            selectedAlbumIndex = if (profIdx >= 0) profIdx else 0
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = OnChatCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Agrandir la photo", color = Color.White)
                        }
                    }

                    if (isSelfProfile) {
                        TextButton(
                            onClick = {
                                showAvatarOptionsDialog = false
                                selectedAlbumIndex = 0
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Collections, contentDescription = null, tint = OnChatViolet)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Choisir depuis l'album", color = Color.White)
                            }
                        }

                        TextButton(
                            onClick = {
                                showAvatarOptionsDialog = false
                                profilePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF25D366))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Importer nouvelle photo", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarOptionsDialog = false }) {
                    Text("Fermer", color = CashPayGrey)
                }
            },
            containerColor = CashPayNavySurface
        )
    }

    // Cover Options Dialog
    if (showCoverOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showCoverOptionsDialog = false },
            title = { Text("Photo de couverture", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showCoverOptionsDialog = false
                            val coverIdx = effectiveMediaList.indexOfFirst { it.isCover }
                            selectedAlbumIndex = if (coverIdx >= 0) coverIdx else 0
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = OnChatCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Agrandir la couverture", color = Color.White)
                        }
                    }

                    if (isSelfProfile) {
                        TextButton(
                            onClick = {
                                showCoverOptionsDialog = false
                                selectedAlbumIndex = 0
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Collections, contentDescription = null, tint = OnChatViolet)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Choisir depuis l'album", color = Color.White)
                            }
                        }

                        TextButton(
                            onClick = {
                                showCoverOptionsDialog = false
                                coverPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Panorama, contentDescription = null, tint = Color(0xFF25D366))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Importer nouvelle couverture", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverOptionsDialog = false }) {
                    Text("Fermer", color = CashPayGrey)
                }
            },
            containerColor = CashPayNavySurface
        )
    }
}

@Composable
fun ShareContactModal(
    fullName: String,
    phone: String,
    walletId: String,
    viewModel: OnChatViewModel,
    onDismiss: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
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
                    Text(
                        text = "Partager ce contact",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choisissez une discussion pour partager le profil de $fullName.",
                    color = CashPayGrey,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(conversations.size) { idx ->
                        val conv = conversations[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val shareText = "Voici le contact de $fullName\nNuméro: $phone\nWallet ID: $walletId"
                                    viewModel.sendMessage(conv.id, shareText)
                                    Toast.makeText(context, "Contact partagé dans ${conv.name}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBadge(
                                name = conv.name,
                                avatarUrl = conv.avatarUrl,
                                size = 40.dp,
                                isGroup = conv.isGroup
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = conv.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (idx < conversations.size - 1) {
                            Divider(color = CashPayNavyBorder, thickness = 0.5.dp)
                        }
                    }
                }

                if (conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune discussion disponible", color = CashPayGrey, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileWithPinDialog(
    currentFullName: String,
    currentProfession: String,
    currentActivity: String,
    currentAddress: String,
    currentCity: String,
    currentProvince: String,
    currentCountry: String,
    currentNationality: String,
    currentLanguage: String,
    currentBirthDate: String,
    currentAvailability: String,
    onDismiss: () -> Unit,
    onConfirm: (
        pin: String,
        fullName: String,
        profession: String,
        activity: String,
        address: String,
        city: String,
        province: String,
        country: String,
        nationality: String,
        language: String,
        birthDate: String,
        availability: String
    ) -> Unit
) {
    var fullName by remember { mutableStateOf(currentFullName) }
    var profession by remember { mutableStateOf(currentProfession) }
    var activity by remember { mutableStateOf(currentActivity) }
    var address by remember { mutableStateOf(currentAddress) }
    var city by remember { mutableStateOf(currentCity) }
    var province by remember { mutableStateOf(currentProvince) }
    var country by remember { mutableStateOf(currentCountry) }
    var nationality by remember { mutableStateOf(currentNationality) }
    var language by remember { mutableStateOf(currentLanguage) }
    var birthDate by remember { mutableStateOf(currentBirthDate) }
    var availability by remember { mutableStateOf(currentAvailability) }

    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifier mon profil",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Full Name (Nom complet unique - pas de nom/prénom séparés)
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nom complet (Public & Privé)", color = CashPayGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OnChatCyan,
                            unfocusedBorderColor = CashPayNavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Profession
                    OutlinedTextField(
                        value = profession,
                        onValueChange = { profession = it },
                        label = { Text("Profession / Métier", color = CashPayGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OnChatCyan,
                            unfocusedBorderColor = CashPayNavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Description d'activité / Bio
                    OutlinedTextField(
                        value = activity,
                        onValueChange = { activity = it },
                        label = { Text("Description d'activité / Bio", color = CashPayGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OnChatCyan,
                            unfocusedBorderColor = CashPayNavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Location fields
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Ville", color = CashPayGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OnChatCyan,
                                unfocusedBorderColor = CashPayNavyBorder
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Pays", color = CashPayGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OnChatCyan,
                                unfocusedBorderColor = CashPayNavyBorder
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse complète", color = CashPayGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OnChatCyan,
                            unfocusedBorderColor = CashPayNavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nationality & Language
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nationality,
                            onValueChange = { nationality = it },
                            label = { Text("Nationalité", color = CashPayGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OnChatCyan,
                                unfocusedBorderColor = CashPayNavyBorder
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = language,
                            onValueChange = { language = it },
                            label = { Text("Langue", color = CashPayGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OnChatCyan,
                                unfocusedBorderColor = CashPayNavyBorder
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date de naissance
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("Date de naissance (AAAA-MM-JJ)", color = CashPayGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OnChatCyan,
                            unfocusedBorderColor = CashPayNavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Security PIN Section (Mandatory for PATCH /api/v1/auth/me)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CashPayNavy),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(OnChatViolet.copy(alpha = 0.5f))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Code PIN de sécurité (Obligatoire)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saisissez votre code PIN CashPay pour autoriser la modification.",
                                color = CashPayGrey,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pin = it },
                                label = { Text("Code PIN", color = CashPayGrey) },
                                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                trailingIcon = {
                                    IconButton(onClick = { pinVisible = !pinVisible }) {
                                        Icon(
                                            imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Afficher PIN",
                                            tint = CashPayGrey
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OnChatCyan,
                                    unfocusedBorderColor = CashPayNavyBorder
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = CashPayGrey)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (pin.length < 4) {
                                errorMessage = "Veuillez saisir votre code PIN CashPay (4 à 6 chiffres)."
                                return@Button
                            }
                            if (fullName.isBlank()) {
                                errorMessage = "Le nom complet ne peut pas être vide."
                                return@Button
                            }
                            errorMessage = null
                            onConfirm(
                                pin,
                                fullName.trim(),
                                profession.trim(),
                                activity.trim(),
                                address.trim(),
                                city.trim(),
                                province.trim(),
                                country.trim(),
                                nationality.trim(),
                                language.trim(),
                                birthDate.trim(),
                                availability
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enregistrer", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CashPayNavyBorder),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = OnChatCyan,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileInfoRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isHighlight: Boolean = false,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = subtitle,
                tint = if (isHighlight) OnChatCyan else CashPayGrey,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = if (isHighlight) OnChatCyan else Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = CashPayGrey,
                        fontSize = 12.sp
                    )
                }
            }
        }

        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copier",
                tint = CashPayGrey,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SharedItemRow(
    icon: ImageVector,
    title: String,
    count: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = OnChatCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = count,
                    color = CashPayGrey,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = CashPayGrey,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RingtoneSelectionDialog(
    currentSelected: String,
    onDismiss: () -> Unit,
    onSelect: (title: String, uri: String?) -> Unit
) {
    val context = LocalContext.current
    val ringtones = remember { RingtoneHelper.getAvailableRingtones(context) }
    var playingUri by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
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
                    Text(
                        text = "Sonnerie des Appels",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Text(
                    text = "Choisissez la sonnerie jouée lors d'un appel audio ou vidéo entrant.",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ringtones.size) { idx ->
                        val item = ringtones[idx]
                        val isSelected = item.title == currentSelected
                        val isPlaying = playingUri == item.uriString

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CashPayNavyDark else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, OnChatCyan) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playingUri = item.uriString
                                    RingtoneHelper.playCallRingtone(context, item.uriString)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) OnChatCyan else CashPayGrey,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.title,
                                        color = if (isSelected) Color.White else CashPayGrey,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Sélectionné",
                                            tint = OnChatCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Button(
                                        onClick = { onSelect(item.title, item.uriString) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Choisir", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

