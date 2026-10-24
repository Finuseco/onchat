package com.example.ui.screens.contacts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.ContactCheckResult
import com.example.ui.components.AvatarBadge
import com.example.ui.components.SkeletonChatRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel

data class PhoneBookContact(
    val name: String,
    val phone: String
)

fun fetchDeviceContacts(context: Context): List<PhoneBookContact> {
    val contactList = mutableListOf<PhoneBookContact>()
    val seenPhones = mutableSetOf<String>()

    try {
        val cr = context.contentResolver
        val cursor: Cursor? = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""

                val cleanNumber = number.filter { ch -> ch.isDigit() }
                if (cleanNumber.isNotEmpty() && !seenPhones.contains(cleanNumber)) {
                    seenPhones.add(cleanNumber)
                    contactList.add(PhoneBookContact(name = name.ifBlank { number }, phone = number))
                }
            }
        }
    } catch (e: Exception) {
        // Handle gracefully if security or permission exception occurs
    }

    return contactList
}

@Composable
fun ContactsCheckScreen(
    viewModel: OnChatViewModel,
    onStartChatWithContact: (name: String, avatarUrl: String?, phone: String?, walletId: String?, btcAddress: String?) -> Unit,
    onOpenPublicProfile: (phone: String) -> Unit
) {
    val context = LocalContext.current
    val results by viewModel.contactsResults.collectAsState()
    val isLoading by viewModel.isContactsLoading.collectAsState()

    var searchFilter by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Comptes OnChat (Vérifiés), 1: Tout le Répertoire
    var selectedContactForProfile by remember { mutableStateOf<ContactCheckResult?>(null) }
    var showDetailInfoDialog by remember { mutableStateOf<ContactCheckResult?>(null) }
    var manualPhoneInput by remember { mutableStateOf("") }

    // Local phonebook state
    var localDeviceContacts by remember { mutableStateOf<List<ContactCheckResult>>(emptyList()) }

    // Check system permission status for READ_CONTACTS
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedExplicitly by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            permissionDeniedExplicitly = false
            val deviceContacts = fetchDeviceContacts(context)
            if (deviceContacts.isNotEmpty()) {
                localDeviceContacts = deviceContacts.map {
                    ContactCheckResult(phone = it.phone, onChat = false, name = it.name, avatar = null, userId = null, walletId = null)
                }
                viewModel.checkContacts(deviceContacts.map { it.phone })
            }
        } else {
            permissionDeniedExplicitly = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val deviceContacts = fetchDeviceContacts(context)
            if (deviceContacts.isNotEmpty()) {
                localDeviceContacts = deviceContacts.map {
                    ContactCheckResult(phone = it.phone, onChat = false, name = it.name, avatar = null, userId = null, walletId = null)
                }
                if (results.isEmpty()) {
                    viewModel.checkContacts(deviceContacts.map { it.phone })
                }
            }
        }
    }

    val searchedProfile by viewModel.searchedProfile.collectAsState()

    LaunchedEffect(searchFilter) {
        val trimmed = searchFilter.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length >= 7) {
            kotlinx.coroutines.delay(350)
            viewModel.searchProfileByPhoneState(trimmed)
        }
    }

    val mergedList = remember(results, localDeviceContacts, searchedProfile) {
        val baseList = if (localDeviceContacts.isEmpty()) {
            results
        } else {
            val resultsBy9Digits = results.associateBy { it.phone?.filter { ch -> ch.isDigit() }?.takeLast(9) ?: "" }
            val updated = localDeviceContacts.map { local ->
                val key = local.phone?.filter { ch -> ch.isDigit() }?.takeLast(9) ?: ""
                val match = resultsBy9Digits[key]
                if (match != null) {
                    match.copy(name = if (match.name.isNullOrBlank() || match.name == match.phone) local.name else match.name)
                } else {
                    local
                }
            }
            val localKeys = localDeviceContacts.map { it.phone?.filter { ch -> ch.isDigit() }?.takeLast(9) ?: "" }.toSet()
            val extra = results.filter { !localKeys.contains(it.phone?.filter { ch -> ch.isDigit() }?.takeLast(9) ?: "") }
            updated + extra
        }

        val profile = searchedProfile
        if (profile != null && profile.onChat) {
            val pKey = profile.phone?.filter { ch -> ch.isDigit() }?.takeLast(9) ?: ""
            val alreadyPresent = baseList.any { (it.phone?.filter { ch -> ch.isDigit() }?.takeLast(9) ?: "") == pKey }
            if (!alreadyPresent) {
                listOf(profile) + baseList
            } else {
                baseList
            }
        } else {
            baseList
        }
    }

    val displayList = remember(mergedList, selectedTab, searchFilter) {
        try {
            val base = if (selectedTab == 0) {
                mergedList.filter { it.onChat }
            } else {
                mergedList
            }

            if (searchFilter.isBlank()) base
            else base.filter { item ->
                val nameMatch = item.name?.contains(searchFilter, ignoreCase = true) == true
                val phoneMatch = item.phone?.contains(searchFilter) == true
                val walletMatch = item.walletId?.contains(searchFilter, ignoreCase = true) == true
                nameMatch || phoneMatch || walletMatch
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Permission card if not granted yet
            if (!hasPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(brush = OnChatPrimaryGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Synchronisation du Répertoire",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Autorisez l'accès pour vérifier en temps réel quels numéros possèdent un compte OnChat actif.",
                            color = CashPayGrey,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            colors = ButtonDefaults.buttonColors(containerColor = CashPayOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchroniser les contacts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Search Bar & Direct Number Lookup
            OutlinedTextField(
                value = searchFilter,
                onValueChange = { searchFilter = it },
                placeholder = { Text("Rechercher un contact, numéro ou wallet...", color = CashPayGrey, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnChatCyan) },
                trailingIcon = {
                    if (searchFilter.isNotBlank()) {
                        IconButton(onClick = { searchFilter = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", tint = CashPayGrey)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CashPayNavySurface,
                    unfocusedContainerColor = CashPayNavySurface,
                    focusedBorderColor = OnChatCyan,
                    unfocusedBorderColor = CashPayNavyBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector: "Comptes OnChat" vs "Tous les contacts"
            val onChatCount = mergedList.count { it.onChat }
            val totalCount = mergedList.size

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CashPayNavySurface,
                contentColor = OnChatCyan,
                divider = {},
                indicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) OnChatViolet.copy(alpha = 0.4f) else Color.Transparent)
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (selectedTab == 0) OnChatCyan else CashPayGrey,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Inscrits OnChat ($onChatCount)",
                            color = if (selectedTab == 0) Color.White else CashPayGrey,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) OnChatViolet.copy(alpha = 0.4f) else Color.Transparent)
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = if (selectedTab == 1) OnChatCyan else CashPayGrey,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tous ($totalCount)",
                            color = if (selectedTab == 1) Color.White else CashPayGrey,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = OnChatCyan,
                    trackColor = CashPayNavySurface
                )
            }

            // List of Contacts
            if (isLoading && displayList.isEmpty()) {
                Column {
                    repeat(4) {
                        SkeletonChatRow()
                        Divider(color = CashPayNavyBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            } else if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.PersonOff else Icons.Default.Contacts,
                            contentDescription = null,
                            tint = CashPayGrey.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "Aucun contact inscrit trouvé" else "Répertoire vide ou non synchronisé",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == 0) "Consultez l'onglet 'Tous' pour inviter des proches via SMS." else "Cliquez sur le bouton de synchronisation ci-dessous pour analyser votre répertoire.",
                            color = CashPayGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = displayList,
                        key = { contact ->
                            val phoneStr = contact.phone.orEmpty()
                            val nameStr = contact.name.orEmpty()
                            val walletStr = contact.walletId.orEmpty()
                            val userStr = contact.userId?.toString().orEmpty()
                            "${phoneStr}_${nameStr}_${walletStr}_${userStr}_${contact.hashCode()}"
                        }
                    ) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onClick = {
                                selectedContactForProfile = contact
                            },
                            onChatClick = {
                                if (contact.onChat) {
                                    onStartChatWithContact(
                                        contact.name ?: contact.phone,
                                        contact.avatar,
                                        contact.phone,
                                        contact.resolvedWalletId ?: contact.walletId,
                                        contact.bitcoinAddress
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Ce numéro n'a pas encore de compte OnChat. Vous pouvez l'inviter.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onInviteClick = {
                                inviteContactViaSms(context, contact.phone)
                            }
                        )
                    }
                }
            }
        }

        // FAB to sync contacts
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = OnChatVioletDeep.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = OnChatPrimaryGradient)
                    .clickable {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            val deviceContacts = fetchDeviceContacts(context)
                            if (deviceContacts.isNotEmpty()) {
                                viewModel.checkContacts(deviceContacts.map { it.phone })
                                Toast.makeText(context, "Vérification de ${deviceContacts.size} contacts en cours...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Synchroniser les contacts",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // Modal / Dialog for Contact Profile
    selectedContactForProfile?.let { contact ->
        ContactProfileDialog(
            contact = contact,
            onDismiss = { selectedContactForProfile = null },
            onStartChat = {
                if (contact.onChat) {
                    selectedContactForProfile = null
                    onStartChatWithContact(
                        contact.name ?: contact.phone,
                        contact.avatar,
                        contact.phone,
                        contact.resolvedWalletId ?: contact.walletId,
                        contact.bitcoinAddress
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Ce numéro n'existe pas sur OnChat. Impossible d'ouvrir une discussion.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onStartVideoCall = {
                if (contact.onChat) {
                    selectedContactForProfile = null
                    viewModel.startCall(
                        peerName = contact.name ?: contact.phone,
                        peerWalletId = contact.resolvedWalletId ?: contact.walletId ?: "",
                        peerAvatar = contact.avatar ?: contact.profile?.resolvedAvatar,
                        isVideo = true
                    )
                }
            },
            onStartAudioCall = {
                if (contact.onChat) {
                    selectedContactForProfile = null
                    viewModel.startCall(
                        peerName = contact.name ?: contact.phone,
                        peerWalletId = contact.resolvedWalletId ?: contact.walletId ?: "",
                        peerAvatar = contact.avatar ?: contact.profile?.resolvedAvatar,
                        isVideo = false
                    )
                }
            },
            onOpenPublicProfile = { phone ->
                selectedContactForProfile = null
                onOpenPublicProfile(phone)
            },
            onShowInfo = {
                showDetailInfoDialog = contact
            },
            onInviteSms = {
                inviteContactViaSms(context, contact.phone)
            }
        )
    }

    // Info Profile Details Dialog
    showDetailInfoDialog?.let { contact ->
        ContactInfoDetailsDialog(
            contact = contact,
            onDismiss = { showDetailInfoDialog = null }
        )
    }
}

fun inviteContactViaSms(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phone")
            putExtra("sms_body", "Rejoins-moi sur OnChat pour discuter et envoyer de l'argent instantanément avec CashPay ! https://cashpay-all.com/app")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Lien d'invitation", "Rejoins-moi sur OnChat: https://cashpay-all.com/app")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Lien d'invitation copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ContactItemCard(
    contact: ContactCheckResult,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (contact.onChat) OnChatCyan.copy(alpha = 0.4f) else CashPayNavyBorder
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(
                name = contact.name ?: contact.phone,
                avatarUrl = contact.avatar,
                size = 50.dp,
                isOnline = contact.onChat
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name ?: contact.phone,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.onChat) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Vérifié",
                            tint = OnChatCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (contact.onChat) {
                    if (!contact.walletId.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OnChatViolet.copy(alpha = 0.25f))
                                    .border(0.8.dp, OnChatCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Wallet: ${contact.walletId}",
                                    color = OnChatCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Compte OnChat vérifié",
                            color = SuccessGreen,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = contact.phone,
                        color = CashPayGrey,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Non inscrit sur OnChat",
                        color = CashPayGrey.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            if (contact.onChat) {
                IconButton(onClick = onChatClick) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(brush = OnChatPrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Discuter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onInviteClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CashPayOrange),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CashPayOrange)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inviter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ContactProfileDialog(
    contact: ContactCheckResult,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit,
    onStartVideoCall: () -> Unit,
    onStartAudioCall: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onShowInfo: () -> Unit,
    onInviteSms: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                // Large Avatar
                AvatarBadge(
                    name = contact.name ?: contact.phone,
                    avatarUrl = contact.avatar,
                    size = 90.dp,
                    hasStatus = contact.onChat,
                    isOnline = contact.onChat
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                Text(
                    text = contact.name ?: contact.phone,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Phone number
                Text(
                    text = contact.phone,
                    color = CashPayGrey,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // Wallet ID pill if available
                if (contact.onChat && !contact.walletId.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OnChatViolet.copy(alpha = 0.25f))
                            .border(1.dp, OnChatCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Wallet ID", contact.walletId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Wallet ID copié !", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = OnChatCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = contact.walletId,
                            color = OnChatCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier",
                            tint = CashPayGrey,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = CashPayNavyBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(20.dp))

                if (contact.onChat) {
                    // Verified OnChat Actions: Chat, Video, Audio, Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileActionButton(
                            icon = Icons.Default.Chat,
                            label = "Discuter",
                            tint = OnChatCyan,
                            gradientBg = true,
                            onClick = onStartChat
                        )

                        ProfileActionButton(
                            icon = Icons.Default.Videocam,
                            label = "Vidéo",
                            tint = Color.White,
                            onClick = onStartVideoCall
                        )

                        ProfileActionButton(
                            icon = Icons.Default.Call,
                            label = "Audio",
                            tint = Color.White,
                            onClick = onStartAudioCall
                        )

                        ProfileActionButton(
                            icon = Icons.Default.Public,
                            label = "Social",
                            tint = OnChatCyan,
                            onClick = {
                                if (contact.phone != null) {
                                    onOpenPublicProfile(contact.phone)
                                }
                            }
                        )

                        ProfileActionButton(
                            icon = Icons.Default.Info,
                            label = "Infos",
                            tint = CashPayOrange,
                            onClick = onShowInfo
                        )
                    }
                } else {
                    // Non-registered contact state: no opening chat, show invite button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ce contact n'est pas encore inscrit sur OnChat.",
                            color = CashPayGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onInviteSms,
                            colors = ButtonDefaults.buttonColors(containerColor = CashPayOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inviter à rejoindre OnChat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    gradientBg: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (gradientBg) Modifier.background(brush = OnChatPrimaryGradient)
                    else Modifier.background(CashPayNavyDark).border(1.dp, CashPayNavyBorder, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (gradientBg) Color.White else tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ContactInfoDetailsDialog(
    contact: ContactCheckResult,
    onDismiss: () -> Unit
) {
    val p = contact.profile
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = OnChatCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Détails du Profil OnChat", color = Color.White, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRowItem(label = "Nom complet", value = contact.name ?: p?.resolvedFullName ?: "Non renseigné")
                if (!contact.walletId.isNullOrEmpty() || !p?.resolvedWalletId.isNullOrEmpty()) {
                    InfoRowItem(label = "Wallet ID CashPay", value = contact.walletId ?: p?.resolvedWalletId ?: "")
                }
                if (!p?.resolvedBitcoinAddress.isNullOrBlank()) {
                    InfoRowItem(label = "Adresse Bitcoin (BTC)", value = p!!.resolvedBitcoinAddress!!)
                }
                if (!p?.profession.isNullOrBlank()) {
                    InfoRowItem(label = "Profession / Métier", value = p!!.profession!!)
                }
                if (!p?.resolvedActivity.isNullOrBlank()) {
                    InfoRowItem(label = "Activité / Bio", value = p!!.resolvedActivity!!)
                }
                val location = listOfNotNull(p?.address, p?.city, p?.province, p?.country).filter { it.isNotBlank() }.joinToString(", ")
                if (location.isNotBlank()) {
                    InfoRowItem(label = "Localisation", value = location)
                }
                val natLang = listOfNotNull(p?.nationality, p?.language).filter { it.isNotBlank() }.joinToString(" • ")
                if (natLang.isNotBlank()) {
                    InfoRowItem(label = "Nationalité & Langue", value = natLang)
                }
                if (p?.astro != null && (!p.astro.sign.isNullOrBlank() || !p.astro.element.isNullOrBlank())) {
                    val astroText = listOfNotNull(p.astro.sign, p.astro.element).joinToString(" • Élément: ")
                    InfoRowItem(label = "Profil Astrologique", value = astroText)
                }
                InfoRowItem(
                    label = "Statut OnChat",
                    value = if (contact.onChat) "Compte actif et vérifié" else "Non inscrit"
                )
                InfoRowItem(label = "Sécurité", value = "Chiffrement de bout en bout")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet)
            ) {
                Text("Fermer")
            }
        },
        containerColor = CashPayNavySurface
    )
}

@Composable
fun InfoRowItem(label: String, value: String) {
    Column {
        Text(text = label, color = CashPayGrey, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
