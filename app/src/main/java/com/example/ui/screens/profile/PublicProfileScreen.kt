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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SocialMediaItem
import com.example.ui.components.AvatarBadge
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
        }
    }

    // Resolve user data with cascading fallbacks
    val pFromPublic = profileResp?.profile
    val pFromSearch = searchedProfile?.profile

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
        ?: "Membre OnChat"

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // Header Zone (Cover + Avatar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Cover Photo
                AsyncImage(
                    model = coverUrl,
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
                        name = fullName,
                        avatarUrl = avatarUrl,
                        size = 120.dp,
                        isGroup = false,
                        isOnline = true
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
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
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
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Social Actions Row (Message, Call, Transfer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            viewModel.openContactChat(
                                name = fullName,
                                avatarUrl = avatarUrl,
                                phone = phone,
                                walletId = walletId,
                                recipientUserId = null,
                                bitcoinAddress = null
                            )
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.startCall(
                                peerName = fullName,
                                peerWalletId = walletId.ifBlank { phone },
                                peerAvatar = avatarUrl,
                                isVideo = false
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Appel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

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
                    text = "PHOTOS / MÉDIAS PUBLICS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (media.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF18181B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun média public publié pour le moment", color = CashPayGrey, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
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
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = CashPayGrey, fontSize = 12.sp)
    }
}
