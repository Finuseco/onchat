package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

enum class OnChatTab(val title: String, val icon: ImageVector) {
    CHATS("Chat", Icons.Default.Chat),
    GROUPS("Groupes", Icons.Default.Groups),
    STATUS("Actu", Icons.Default.MotionPhotosOn),
    CALLS("Appels", Icons.Default.Call),
    CONTACTS("Contacts", Icons.Default.People)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewGroup: () -> Unit,
    onOpenNewStatus: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenMyProfile: (() -> Unit)? = null,
    onOpenLiveReels: (() -> Unit)? = null,
    onOpenAiChat: (() -> Unit)? = null
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CashPayNavy)
            .statusBarsPadding()
    ) {
        // App bar row
        if (isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isSearchActive = false
                    onSearchQueryChange("")
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Fermer recherche",
                        tint = Color.White
                    )
                }
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Rechercher discussions, groupes, contacts...",
                            color = CashPayGrey,
                            fontSize = 15.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = OnChatCyan,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "On",
                        color = OnChatVioletLight,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "Chat",
                        color = OnChatCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live / Video Conference Reel Button (Camera icon + Live text)
                    Surface(
                        onClick = { onOpenLiveReels?.invoke() },
                        shape = RoundedCornerShape(16.dp),
                        color = OnChatViolet.copy(alpha = 0.25f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Live",
                                tint = OnChatCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Live",
                                color = OnChatCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(CashPayNavySurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Assistant IA (Chatbot)", color = Color.White, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = OnChatCyan)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenAiChat?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Vidéos Live & Conférences", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = OnChatCyan)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenLiveReels?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mon Profil", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = OnChatCyan)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenMyProfile?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nouveau Groupe", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = OnChatCyan)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenNewGroup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Publier un Statut", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = OnChatVioletLight)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenNewStatus()
                                }
                            )
                            Divider(color = CashPayNavyBorder)
                            DropdownMenuItem(
                                text = { Text("Paramètres", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = CashPayGrey)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
