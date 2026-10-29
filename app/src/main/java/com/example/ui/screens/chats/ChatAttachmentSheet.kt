package com.example.ui.screens.chats

import com.example.data.model.assetBalance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CardDto
import com.example.data.model.WalletApiResponse
import com.example.ui.screens.contacts.PhoneBookContact
import com.example.ui.screens.contacts.fetchDeviceContacts
import com.example.ui.theme.*
import java.util.Locale

enum class AttachmentTab {
    WALLET,
    FILE,
    POSITION,
    LIST,
    CONTACT
}

data class NearbyPlace(
    val name: String,
    val address: String,
    val distance: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAttachmentSheet(
    recipientName: String,
    conversationCurrency: String,
    walletData: WalletApiResponse? = null,
    isWalletLoading: Boolean = false,
    onRefreshWallet: () -> Unit = {},
    onDismiss: () -> Unit,
    onSendMoneyClick: (selectedCurrency: String) -> Unit,
    onSendLocation: (title: String, subtitle: String, lat: Double, lng: Double) -> Unit,
    onSendFile: (fileName: String, fileType: String, sizeStr: String, uri: Uri?) -> Unit,
    onSendContact: (name: String, phone: String, walletId: String, avatar: String?) -> Unit,
    onSendPoll: (question: String, options: List<String>) -> Unit,
    searchProfile: suspend (String) -> com.example.data.model.ContactCheckResult? = { null }
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AttachmentTab.WALLET) }

    val nearbyPlaces = remember {
        listOf(
            NearbyPlace("Aéroport International de N'djili", "Boulevard Lumumba, Kinshasa", "12.4 km", Icons.Default.Flight),
            NearbyPlace("Boulevard du 30 Juin", "Gombe, Centre des Affaires, Kinshasa", "450 m", Icons.Default.LocationCity),
            NearbyPlace("Grand Hôtel de Kinshasa / Fleuve Congo", "Avenue des Aviateurs, Gombe", "1.1 km", Icons.Default.Hotel),
            NearbyPlace("Place de la Victoire", "Matonge, Kalamu, Kinshasa", "3.8 km", Icons.Default.Place),
            NearbyPlace("Palais du Peuple", "Avenue Triomphal, Lingwala", "2.2 km", Icons.Default.AccountBalance),
            NearbyPlace("Port de Kinshasa (Beach Ngobila)", "Beach Ngobila, Gombe", "1.9 km", Icons.Default.DirectionsBoat)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CashPayNavyDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CashPayGrey.copy(alpha = 0.4f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Main Content Area based on Selected Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    AttachmentTab.POSITION -> {
                        PositionAttachmentView(
                            onSendSelectedLocation = {
                                var realLat = -4.3217
                                var realLng = 15.3126
                                try {
                                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                                    val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                        ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                    if (loc != null) {
                                        realLat = loc.latitude
                                        realLng = loc.longitude
                                    }
                                } catch (_: Exception) { }

                                onSendLocation(
                                    "Position GPS GPS Real-Time",
                                    "Kinshasa, RDC ($realLat, $realLng)",
                                    realLat,
                                    realLng
                                )
                                onDismiss()
                            },
                            onSendLiveLocation = {
                                var realLat = -4.3217
                                var realLng = 15.3126
                                try {
                                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                                    val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                        ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                    if (loc != null) {
                                        realLat = loc.latitude
                                        realLng = loc.longitude
                                    }
                                } catch (_: Exception) { }

                                onSendLocation(
                                    "Position en direct (GPS Actif)",
                                    "Actualisée en temps réel ($realLat, $realLng)",
                                    realLat,
                                    realLng
                                )
                                onDismiss()
                            },
                            nearbyPlaces = nearbyPlaces,
                            onSelectPlace = { place ->
                                onSendLocation(
                                    place.name,
                                    place.address,
                                    -4.3217,
                                    15.3126
                                )
                                onDismiss()
                            }
                        )
                    }
                    AttachmentTab.WALLET -> {
                        WalletAttachmentView(
                            recipientName = recipientName,
                            walletData = walletData,
                            isWalletLoading = isWalletLoading,
                            onRefreshWallet = onRefreshWallet,
                            onSendMoney = { selectedCurrency ->
                                onDismiss()
                                onSendMoneyClick(selectedCurrency)
                            },
                            onRequestMoney = { amount, selectedCurrency ->
                                onSendFile("Demande de paiement : $amount $selectedCurrency", "payment_request", "1 req", null)
                                Toast.makeText(context, "Demande de $amount $selectedCurrency envoyée !", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                    }
                    AttachmentTab.FILE -> {
                        FileAttachmentView(
                            onSelectFile = { name, type, size, uri ->
                                onSendFile(name, type, size, uri)
                                onDismiss()
                            }
                        )
                    }
                    AttachmentTab.LIST -> {
                        ListAttachmentView(
                            onSendPoll = { question, options ->
                                onSendPoll(question, options)
                                onDismiss()
                            }
                        )
                    }
                    AttachmentTab.CONTACT -> {
                        ContactAttachmentView(
                            onSelectContact = { name, phone, walletId, avatar ->
                                onSendContact(name, phone, walletId, avatar)
                                onDismiss()
                            },
                            searchProfile = searchProfile
                        )
                    }
                }
            }

            // Bottom Navigation Bar matching Screenshot 2
            Surface(
                color = CashPayNavySurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttachmentNavTabItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Wallet",
                        isSelected = currentTab == AttachmentTab.WALLET,
                        onClick = { currentTab = AttachmentTab.WALLET }
                    )
                    AttachmentNavTabItem(
                        icon = Icons.Default.Description,
                        label = "Fichier",
                        isSelected = currentTab == AttachmentTab.FILE,
                        onClick = { currentTab = AttachmentTab.FILE }
                    )
                    AttachmentNavTabItem(
                        icon = Icons.Default.LocationOn,
                        label = "Position",
                        isSelected = currentTab == AttachmentTab.POSITION,
                        onClick = { currentTab = AttachmentTab.POSITION }
                    )
                    AttachmentNavTabItem(
                        icon = Icons.Default.PlaylistAddCheck,
                        label = "Liste",
                        isSelected = currentTab == AttachmentTab.LIST,
                        onClick = { currentTab = AttachmentTab.LIST }
                    )
                    AttachmentNavTabItem(
                        icon = Icons.Default.AccountCircle,
                        label = "Contact",
                        isSelected = currentTab == AttachmentTab.CONTACT,
                        onClick = { currentTab = AttachmentTab.CONTACT }
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentNavTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (isSelected) OnChatViolet.copy(alpha = 0.35f) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) OnChatCyan else CashPayGrey,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) OnChatCyan else CashPayGrey
        )
    }
}

// --- Tab 1: POSITION VIEW (Matches Screenshot 2) ---
@Composable
fun PositionAttachmentView(
    onSendSelectedLocation: () -> Unit,
    onSendLiveLocation: () -> Unit,
    nearbyPlaces: List<NearbyPlace>,
    onSelectPlace: (NearbyPlace) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stylized Vector Map Preview (Dark Blueprint Google-Maps Theme)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Color(0xFF1E2B45))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Background water / land
                drawRect(color = Color(0xFF1A2740))

                // Roads and Grid lines
                val roadColor = Color(0xFF2B3D66)
                val highwayColor = Color(0xFF3B568C)

                // Diagonal arterial roads
                drawLine(highwayColor, Offset(0f, h * 0.3f), Offset(w, h * 0.7f), strokeWidth = 12f)
                drawLine(highwayColor, Offset(w * 0.2f, 0f), Offset(w * 0.9f, h), strokeWidth = 10f)
                drawLine(roadColor, Offset(0f, h * 0.6f), Offset(w, h * 0.2f), strokeWidth = 6f)
                drawLine(roadColor, Offset(w * 0.6f, 0f), Offset(w * 0.4f, h), strokeWidth = 7f)
                drawLine(roadColor, Offset(0f, h * 0.85f), Offset(w, h * 0.85f), strokeWidth = 8f)
                drawLine(roadColor, Offset(w * 0.1f, 0f), Offset(w * 0.1f, h), strokeWidth = 5f)
                drawLine(roadColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 6f)

                // Secondary street network
                for (i in 1..8) {
                    val y = h * (i / 9f)
                    drawLine(roadColor.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), strokeWidth = 3f)
                }

                // Building blocks
                drawRect(Color(0xFF162035), Offset(w * 0.15f, h * 0.15f), androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.12f))
                drawRect(Color(0xFF162035), Offset(w * 0.5f, h * 0.12f), androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.15f))
                drawRect(Color(0xFF162035), Offset(w * 0.22f, h * 0.45f), androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.16f))
                drawRect(Color(0xFF162035), Offset(w * 0.65f, h * 0.5f), androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.18f))

                // User accuracy pulse ring
                drawCircle(
                    color = OnChatCyan.copy(alpha = 0.15f),
                    radius = 45f,
                    center = Offset(w / 2f, h / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(w / 2f, h / 2f)
                )
                drawCircle(
                    color = OnChatBluePrimary,
                    radius = 5f,
                    center = Offset(w / 2f, h / 2f)
                )
            }

            // Top landmarks badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CashPayNavySurface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CashPayNavyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BeachAccess, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Malibu Beach • Kinshasa", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Map Layers Button
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CashPayNavySurface.copy(alpha = 0.9f))
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Calques", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Red Location Pin Centered
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Position actuelle",
                tint = Color(0xFFE53935),
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.Center)
                    .offset(y = (-14).dp)
            )

            // Re-center / GPS target icon bottom right
            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CashPayNavySurface.copy(alpha = 0.95f))
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Ma position", tint = OnChatCyan, modifier = Modifier.size(22.dp))
            }

            // Google Maps attribution badge bottom left
            Text(
                text = "Google",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 8.dp)
            )
        }

        // Action Options List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(CashPayNavyDark),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Option 1: Send Selected Location
            item {
                Card(
                    onClick = onSendSelectedLocation,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(OnChatBluePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Envoyer la position sélectionnée",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kinshasa, Gombe, République Démocratique du Congo",
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Option 2: Live Location
            item {
                Card(
                    onClick = onSendLiveLocation,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Position en temps réel...",
                                color = Color(0xFF25D366),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Actualisée quand vous vous déplacez (15m, 1h, 8h)",
                                color = CashPayGrey,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Section Header: Ou choisissez un lieu
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "OU CHOISISSEZ UN LIEU PROCHE",
                    color = OnChatCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            items(nearbyPlaces) { place ->
                Card(
                    onClick = { onSelectPlace(place) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CashPayNavyBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(place.icon, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = place.address,
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = place.distance,
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

// --- Card Brand Badge (Visa / Mastercard) ---
@Composable
fun CardBrandBadge(brand: String, last4: String) {
    val isVisa = brand.contains("visa", ignoreCase = true)
    val isMastercard = brand.contains("master", ignoreCase = true)

    Surface(
        color = if (isVisa) Color(0xFF0D2561) else Color(0xFF1E1E24),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isVisa) {
                Text(
                    text = "VISA",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            } else if (isMastercard) {
                Box(modifier = Modifier.size(16.dp, 12.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFEB001B),
                            radius = 5.dp.toPx(),
                            center = Offset(5.dp.toPx(), 6.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFFF79E1B).copy(alpha = 0.85f),
                            radius = 5.dp.toPx(),
                            center = Offset(11.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = OnChatCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = "•••• $last4",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class CarouselCardData(
    val id: String,
    val typeTitle: String,
    val currencyCode: String,
    val balanceText: String,
    val subtitleText: String,
    val linkedCard: CardDto?,
    val gradientBrush: Brush,
    val accentColor: Color
)

// --- Tab 2: WALLET VIEW (CashPay All - 4 Floating Cards Carousel) ---
@Composable
fun WalletAttachmentView(
    recipientName: String,
    walletData: WalletApiResponse?,
    isWalletLoading: Boolean,
    onRefreshWallet: () -> Unit,
    onSendMoney: (selectedCurrency: String) -> Unit,
    onRequestMoney: (amount: String, selectedCurrency: String) -> Unit
) {
    val context = LocalContext.current
    var selectedCardIndex by remember { mutableStateOf(0) }
    var reqAmount by remember { mutableStateOf("20") }
    var showDepositDialog by remember { mutableStateOf(false) }
    var depositAmount by remember { mutableStateOf("50") }
    var depositPhone by remember { mutableStateOf("") }
    var selectedDepositProvider by remember { mutableStateOf("M-Pesa") }

    // Dynamic national currency
    val natCode = walletData?.nationalCurrency?.code ?: "CDF"
    val natName = walletData?.nationalCurrency?.name ?: "Franc congolais"
    val natSymbol = walletData?.nationalCurrency?.symbol ?: "FC"
    val natBalance = walletData?.balances?.fiat?.get(natCode) ?: 0.0
    val natLinkedCard = walletData?.cards?.find { it.currency.equals(natCode, ignoreCase = true) }
    val natFormatted = String.format(Locale.US, "%,.2f %s", natBalance, natCode)

    // USD
    val usdBalance = walletData?.balances?.fiat?.get("USD") ?: 0.0
    val usdLinkedCard = walletData?.cards?.find { it.currency.equals("USD", ignoreCase = true) }
    val usdFormatted = String.format(Locale.US, "$ %,.2f", usdBalance)

    // EUR
    val eurBalance = walletData?.balances?.fiat?.get("EUR") ?: 0.0
    val eurLinkedCard = walletData?.cards?.find { it.currency.equals("EUR", ignoreCase = true) }
    val eurFormatted = String.format(Locale.US, "€ %,.2f", eurBalance)

    // Bitcoin
    val btcBalance = walletData?.assetBalance("BTC") ?: 0.0
    val usdtBalance = walletData?.assetBalance("USDT") ?: 0.0
    val usdtAddress = walletData?.usdt?.address.orEmpty()
    val btcAddress = walletData?.bitcoin?.address ?: ""
    val btcFormatted = String.format(Locale.US, "%.8f BTC", btcBalance)
    val btcSubtitle = if (btcAddress.isNotBlank()) "Adresse: ${btcAddress.take(8)}...${btcAddress.takeLast(6)}" else "Adresse Bitcoin non activée"

    val cards = remember(walletData, natCode, natName, natSymbol, natBalance, usdBalance, eurBalance, btcBalance, usdtBalance) {
        listOf(
            CarouselCardData(
                id = "national",
                typeTitle = "DEVISE NATIONALE",
                currencyCode = natCode,
                balanceText = natFormatted,
                subtitleText = "$natName • $natSymbol",
                linkedCard = natLinkedCard,
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
                accentColor = OnChatCyan
            ),
            CarouselCardData(
                id = "usd",
                typeTitle = "DEVISE INTERNATIONALE",
                currencyCode = "USD",
                balanceText = usdFormatted,
                subtitleText = "Dollar américain • USD Référence",
                linkedCard = usdLinkedCard,
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF141E30), Color(0xFF243B55))),
                accentColor = OnChatViolet
            ),
            CarouselCardData(
                id = "eur",
                typeTitle = "DEVISE INTERNATIONALE",
                currencyCode = "EUR",
                balanceText = eurFormatted,
                subtitleText = "Euro • Zone Européenne EUR",
                linkedCard = eurLinkedCard,
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF1F1C2C), Color(0xFF381F4B))),
                accentColor = Color(0xFFB388FF)
            ),
            CarouselCardData(
                id = "btc",
                typeTitle = "CRYPTO CASHPAY",
                currencyCode = "BTC",
                balanceText = btcFormatted,
                subtitleText = btcSubtitle,
                linkedCard = null,
                gradientBrush = Brush.linearGradient(listOf(Color(0xFF1C1506), Color(0xFF3B2A0A))),
                accentColor = Color(0xFFF7931A)
            ),
            CarouselCardData(
                id = "usdt", typeTitle = "CRYPTO CASHPAY • TRC20", currencyCode = "USDT",
                balanceText = String.format(Locale.US, "%.6f USDT", usdtBalance),
                subtitleText = if (usdtAddress.isBlank()) "Adresse USDT TRC20 non activée" else "TRC20 • ${usdtAddress.take(8)}…${usdtAddress.takeLast(6)}",
                linkedCard = null, gradientBrush = Brush.linearGradient(listOf(Color(0xFF073D35), Color(0xFF126D5A))), accentColor = Color(0xFF26A17B)
            )
        )
    }

    val currentCard = cards.getOrElse(selectedCardIndex) { cards[0] }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with Live Sync Status & Refresh
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = OnChatCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PORTEFEUILLE CASHPAY ALL",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onRefreshWallet,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isWalletLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = OnChatCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualiser",
                            tint = OnChatCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Mini Tabs Selector (CDF, USD, EUR, BTC)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.forEachIndexed { index, card ->
                    val isSelected = selectedCardIndex == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedCardIndex = index },
                        color = if (isSelected) card.accentColor.copy(alpha = 0.25f) else CashPayNavySurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) card.accentColor else Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = card.currencyCode,
                                color = if (isSelected) card.accentColor else CashPayGrey,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Floating Card Container (Active Floating Card)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.5.dp, currentCard.accentColor.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(currentCard.gradientBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Card Top Row: Type Title + Card Brand Badge (Visa/Mastercard)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = currentCard.accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentCard.typeTitle,
                                color = currentCard.accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (currentCard.linkedCard != null) {
                            CardBrandBadge(
                                brand = currentCard.linkedCard.brand ?: "visa",
                                last4 = currentCard.linkedCard.last4 ?: "••••"
                            )
                        } else {
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = CashPayGrey,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (currentCard.currencyCode == "BTC") "Crypto Core" else "Solde Direct",
                                        color = CashPayGrey,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Balance Display
                    Text(
                        text = currentCard.balanceText,
                        color = Color.White,
                        fontSize = if (currentCard.currencyCode == "BTC") 22.sp else 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentCard.subtitleText,
                        color = CashPayGrey,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer Row: Wallet ID & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wallet: ${walletData?.wallet?.walletId ?: "CashPay"}",
                                color = CashPayGrey,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366))
                            )
                            Text(
                                text = "Actif • Serveur",
                                color = Color(0xFF25D366),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Pager / Carousel Indicator Dots & Prev/Next
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        selectedCardIndex = if (selectedCardIndex > 0) selectedCardIndex - 1 else cards.size - 1
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Précédent",
                        tint = CashPayGrey,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                cards.forEachIndexed { idx, card ->
                    val isDotSelected = idx == selectedCardIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isDotSelected) 24.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDotSelected) card.accentColor else Color.White.copy(alpha = 0.2f))
                            .clickable { selectedCardIndex = idx }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        selectedCardIndex = (selectedCardIndex + 1) % cards.size
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Suivant",
                        tint = CashPayGrey,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Action 1: Send Money using active card's currency
        item {
            Button(
                onClick = { onSendMoney(currentCard.currencyCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentCard.accentColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = CashPayNavyDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Envoyer de l'argent (${currentCard.currencyCode}) à $recipientName",
                    color = CashPayNavyDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Action: Deposit via Mobile Money / Card / Crypto
        item {
            OutlinedButton(
                onClick = { showDepositDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.dp, currentCard.accentColor.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CashPayNavySurface)
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = currentCard.accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dépôt via Mobile Money / Carte (${currentCard.currencyCode})",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Action 2: Request Money using active card's currency
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                border = BorderStroke(1.dp, CashPayNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DEMANDER UN PAIEMENT (${currentCard.currencyCode})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = reqAmount,
                            onValueChange = { reqAmount = it },
                            label = { Text("Montant (${currentCard.currencyCode})", color = CashPayGrey) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = currentCard.accentColor,
                                unfocusedBorderColor = CashPayNavyBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onRequestMoney(reqAmount, currentCard.currencyCode) },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text("Demander", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Dépôt Mobile Money / Carte
    if (showDepositDialog) {
        val providers = remember(currentCard.currencyCode) {
            when (currentCard.currencyCode) {
                "CDF" -> listOf("M-Pesa (Vodacom)", "Orange Money", "Airtel Money", "Africell Cash")
                "USD" -> listOf("M-Pesa USD", "Airtel Money USD", "Orange Money USD", "Carte Visa / Mastercard")
                "EUR" -> listOf("Carte Visa / Mastercard", "Virement Bancaire SEPA")
                "BTC" -> listOf("Dépôt Réseau Bitcoin (BTC)")
                "USDT" -> listOf("Dépôt réseau TRON (USDT TRC20)")
                else -> listOf("Mobile Money", "Carte Bancaire")
            }
        }
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            containerColor = CashPayNavyDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentCard.accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CashPayNavyDark, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Dépôt CashPay All", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Recharger solde : ${currentCard.currencyCode}", color = currentCard.accentColor, fontSize = 12.sp)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Moyen de paiement :", color = CashPayGrey, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    providers.forEach { provider ->
                        Card(
                            onClick = { selectedDepositProvider = provider },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDepositProvider == provider) currentCard.accentColor.copy(alpha = 0.15f) else CashPayNavySurface
                            ),
                            border = BorderStroke(1.dp, if (selectedDepositProvider == provider) currentCard.accentColor else CashPayNavyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(provider, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (selectedDepositProvider == provider) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = currentCard.accentColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentCard.currencyCode in setOf("BTC", "USDT")) {
                        Text(if (currentCard.currencyCode == "USDT") "Adresse USDT TRC20 (réseau TRON uniquement) :" else "Adresse Bitcoin (réseau Bitcoin uniquement) :", color = CashPayGrey, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                            border = BorderStroke(1.dp, CashPayNavyBorder)
                        ) {
                            Text(
                                text = (if (currentCard.currencyCode == "USDT") usdtAddress else btcAddress).ifBlank { "Adresse non activée" },
                                color = OnChatCyan,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = depositAmount,
                            onValueChange = { depositAmount = it },
                            label = { Text("Montant à recharger (${currentCard.currencyCode})", color = CashPayGrey) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = currentCard.accentColor,
                                unfocusedBorderColor = CashPayNavyBorder,
                                cursorColor = currentCard.accentColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = depositPhone,
                            onValueChange = { depositPhone = it },
                            label = { Text("Numéro Mobile Money (+243...)", color = CashPayGrey) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = currentCard.accentColor,
                                unfocusedBorderColor = CashPayNavyBorder,
                                cursorColor = currentCard.accentColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentCard.currencyCode in setOf("BTC", "USDT")) {
                            val address = if (currentCard.currencyCode == "USDT") usdtAddress else btcAddress
                            if (address.isNotBlank()) {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Adresse ${currentCard.currencyCode}", address))
                                Toast.makeText(context, "Adresse copiée. Utilisez uniquement le réseau indiqué.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://app.cashpay-all.com/deposit")))
                        }
                        showDepositDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentCard.accentColor)
                ) {
                    Text(if (currentCard.currencyCode in setOf("BTC", "USDT")) "Copier l’adresse de dépôt" else "Continuer dans CashPay", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("Annuler", color = CashPayGrey)
                }
            }
        )
    }
}

// --- Tab 3: FILE VIEW ---
@Composable
fun FileAttachmentView(
    onSelectFile: (name: String, type: String, size: String, uri: Uri?) -> Unit
) {
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "image_${System.currentTimeMillis()}.jpg"
            val fileSize = getFileSizeFromUri(context, it) ?: "Photo"
            onSelectFile(fileName, "image", fileSize, it)
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "document_${System.currentTimeMillis()}.pdf"
            val fileSize = getFileSizeFromUri(context, it) ?: "Doc"
            onSelectFile(fileName, "doc", fileSize, it)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "audio_${System.currentTimeMillis()}.mp3"
            val fileSize = getFileSizeFromUri(context, it) ?: "Audio"
            onSelectFile(fileName, "audio", fileSize, it)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "video_${System.currentTimeMillis()}.mp4"
            val fileSize = getFileSizeFromUri(context, it) ?: "Vidéo"
            onSelectFile(fileName, "video", fileSize, it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("SÉLECTIONNER UN FICHIER DEPUIS L'APPAREIL", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Quick file categories row with real system intents
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FileCategoryCard(Icons.Default.PhotoLibrary, "Galerie", OnChatVioletLight) {
                imagePickerLauncher.launch("image/*")
            }
            FileCategoryCard(Icons.Default.InsertDriveFile, "Document", OnChatBluePrimary) {
                docPickerLauncher.launch("*/*")
            }
            FileCategoryCard(Icons.Default.AudioFile, "Audio", Color(0xFFE91E63)) {
                audioPickerLauncher.launch("audio/*")
            }
            FileCategoryCard(Icons.Default.VideoLibrary, "Vidéo", Color(0xFFFF9800)) {
                videoPickerLauncher.launch("video/*")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = BorderStroke(1.dp, CashPayNavyBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = OnChatCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Choisissez un fichier sur votre téléphone",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Images, PDF, Reçus, Contrats, Audios ou Vidéos",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Button(
                    onClick = { docPickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parcourir les fichiers", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = it.getString(index)
            }
        }
    } catch (_: Exception) {}
    return name ?: uri.lastPathSegment
}

private fun getFileSizeFromUri(context: Context, uri: Uri): String? {
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val bytes = it.getLong(sizeIndex)
                    return when {
                        bytes > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
                        bytes > 1024 -> "${bytes / 1024} KB"
                        else -> "$bytes B"
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return null
}

@Composable
fun FileCategoryCard(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// --- Tab 4: LIST & POLL VIEW ---
@Composable
fun ListAttachmentView(
    onSendPoll: (question: String, options: List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("CRÉER UN SONDAGE / LISTE", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            placeholder = { Text("Posez votre question...", color = CashPayGrey) },
            label = { Text("Question", color = CashPayGrey) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OnChatCyan,
                unfocusedBorderColor = CashPayNavyBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = option1,
            onValueChange = { option1 = it },
            placeholder = { Text("Option 1", color = CashPayGrey) },
            label = { Text("Option 1", color = CashPayGrey) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OnChatCyan,
                unfocusedBorderColor = CashPayNavyBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = option2,
            onValueChange = { option2 = it },
            placeholder = { Text("Option 2", color = CashPayGrey) },
            label = { Text("Option 2", color = CashPayGrey) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OnChatCyan,
                unfocusedBorderColor = CashPayNavyBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = option3,
            onValueChange = { option3 = it },
            placeholder = { Text("Option 3 (Facultative)", color = CashPayGrey) },
            label = { Text("Option 3 (Facultative)", color = CashPayGrey) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OnChatCyan,
                unfocusedBorderColor = CashPayNavyBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val canSubmit = question.isNotBlank() && option1.isNotBlank() && option2.isNotBlank()

        Button(
            onClick = {
                val opts = listOf(option1, option2, option3).filter { it.isNotBlank() }
                onSendPoll(question.trim(), opts)
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Publier le sondage", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Tab 5: CONTACT VIEW ---
@Composable
fun ContactAttachmentView(
    onSelectContact: (name: String, phone: String, walletId: String, avatar: String?) -> Unit,
    searchProfile: suspend (String) -> com.example.data.model.ContactCheckResult? = { null }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var deviceContacts by remember { mutableStateOf<List<PhoneBookContact>>(emptyList()) }
    var verifiedStatus by remember { mutableStateOf<Map<String, com.example.data.model.ContactCheckResult>>(emptyMap()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            deviceContacts = fetchDeviceContacts(context)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            deviceContacts = fetchDeviceContacts(context)
        }
    }

    val filteredContacts = remember(deviceContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            deviceContacts
        } else {
            deviceContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("PARTAGER UN CONTACT DU RÉPERTOIRE", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))

        if (!hasPermission) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Accès aux contacts requis", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Autorisez l'application à lire vos contacts pour les partager instantanément.", color = CashPayGrey, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Autoriser l'accès", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher dans vos contacts...", color = CashPayGrey, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnChatCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = OnChatCyan,
                    unfocusedBorderColor = CashPayNavyBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (deviceContacts.isEmpty()) "Aucun contact trouvé dans le carnet d'adresses." else "Aucun résultat correspondant.",
                        color = CashPayGrey,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredContacts) { contact ->
                        val status = verifiedStatus[contact.phone]
                        
                        LaunchedEffect(contact.phone) {
                            if (!verifiedStatus.containsKey(contact.phone)) {
                                val res = searchProfile(contact.phone)
                                if (res != null) {
                                    verifiedStatus = verifiedStatus + (contact.phone to res)
                                }
                            }
                        }

                        Card(
                            onClick = {
                                onSelectContact(
                                    contact.name, 
                                    contact.phone, 
                                    status?.walletId ?: "",
                                    status?.avatar
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (status?.onChat == true) OnChatPrimaryGradient else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (status?.avatar != null) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(status.avatar),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = contact.name.take(1).uppercase(Locale.ROOT),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(contact.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (status?.onChat == true) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Verified, contentDescription = "Actif", tint = OnChatCyan, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(contact.phone, color = CashPayGrey, fontSize = 12.sp)
                                    if (status?.walletId != null) {
                                        Text("Wallet: ${status.walletId}", color = OnChatCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = OnChatCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
