package com.example.ui.screens.chats

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    viewModel: OnChatViewModel,
    searchQuery: String,
    onStartNewChat: () -> Unit,
    onOpenProfile: ((String) -> Unit)? = null,
    onOpenLiveReels: (() -> Unit)? = null
) {
    val conversations by viewModel.conversations.collectAsState()
    val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsState()
    var selectedFilter by remember { mutableStateOf("Tous") }

    LaunchedEffect(Unit) {
        viewModel.fetchServerConversations()
    }

    val filtered = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchesQuery = searchQuery.isEmpty() ||
                    conv.name.contains(searchQuery, ignoreCase = true) ||
                    conv.lastMessage.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Non lus" -> conv.unreadCount > 0
                "Groupes" -> conv.isGroup
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Pills Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Tous", "Non lus", "Groupes")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected) Modifier.background(brush = OnChatPrimaryGradient)
                                else Modifier.background(CashPayNavySurface)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else CashPayGrey,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Chat List or Skeleton (zero flicker when conversations are already cached)
            if (isSkeletonLoading && conversations.isEmpty()) {
                SkeletonChatList(count = 8)
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = CashPayGrey.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Aucune discussion trouvée" else "Aucun message pour le moment",
                            color = CashPayGrey,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filtered, key = { it.id }) { conv ->
                        ChatItemRow(
                            conversation = conv,
                            onClick = { viewModel.openConversation(conv.id) },
                            onAvatarClick = { onOpenProfile?.invoke(conv.id) }
                        )
                        Divider(
                            color = CashPayNavyBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 78.dp)
                        )
                    }
                }
            }
        }

        // Dual Floating Action Buttons: Jojo AI + New Chat
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Live Reels FAB
            FloatingActionButton(
                onClick = { onOpenLiveReels?.invoke() },
                containerColor = CashPayNavySurface,
                contentColor = OnChatCyan,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, OnChatCyan, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Live & Vidéos",
                    tint = OnChatCyan
                )
            }

            // Primary Chat FAB
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = OnChatVioletDeep.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = OnChatPrimaryGradient)
                    .clickable { onStartNewChat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Nouvelle discussion",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null
) {
    val timeFormatted = remember(conversation.lastMessageTime) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(conversation.lastMessageTime))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clickable { onAvatarClick?.invoke() ?: onClick() }) {
            AvatarBadge(
                name = conversation.name,
                avatarUrl = conversation.avatarUrl,
                size = 52.dp,
                isGroup = conversation.isGroup,
                isOnline = conversation.isOnline
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = remember(conversation.name) {
                            val parts = conversation.name.trim().split(" ").filter { it.isNotBlank() }
                            if (parts.size > 2) "${parts[0]} ${parts[1]}" else conversation.name
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val isVerifiedAccount = conversation.contactVerificationStatus == "verified" ||
                            conversation.contactVerificationStatus == "certifie" ||
                            !conversation.contactWalletId.isNullOrBlank()
                    if (isVerifiedAccount) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Compte vérifié",
                            tint = OnChatCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    color = if (conversation.unreadCount > 0) OnChatCyan else CashPayGrey,
                    fontSize = 12.sp,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    color = if (conversation.unreadCount > 0) Color.White else CashPayGrey,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(brush = OnChatPrimaryGradient)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${conversation.unreadCount}",
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
