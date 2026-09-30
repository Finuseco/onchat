package com.example.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Group
import com.example.ui.components.AvatarBadge
import com.example.ui.components.GradientButton
import com.example.ui.components.SkeletonGroupRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel

@Composable
fun GroupListScreen(
    viewModel: OnChatViewModel,
    searchQuery: String,
    onOpenGroupChat: (groupId: Long, groupName: String) -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsState()

    var showCreateModal by remember { mutableStateOf(false) }
    var selectedGroupForDetails by remember { mutableStateOf<Group?>(null) }

    val filtered = remember(groups, searchQuery) {
        if (searchQuery.isEmpty()) groups
        else groups.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.description?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
    ) {
        if (isSkeletonLoading && groups.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(6) {
                    SkeletonGroupRow()
                    Divider(color = CashPayNavyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = CashPayGrey.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucun groupe OnChat",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Créez un groupe avec portefeuille en USD, EUR ou CDF",
                        color = CashPayGrey,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.id }) { group ->
                    GroupItemRow(
                        group = group,
                        onClick = { selectedGroupForDetails = group }
                    )
                    Divider(
                        color = CashPayNavyBorder.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 78.dp)
                    )
                }
            }
        }

        // Create Group Floating Action Button with OnChat Gradient
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
                    .clickable { showCreateModal = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "Créer un groupe",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Modal Create Group
    if (showCreateModal) {
        CreateGroupDialog(
            onDismiss = { showCreateModal = false },
            onCreate = { name, currency, desc ->
                viewModel.createGroup(name, currency, desc)
                showCreateModal = false
            }
        )
    }

    // Modal Group Details & Members Management
    if (selectedGroupForDetails != null) {
        GroupDetailsDialog(
            group = selectedGroupForDetails!!,
            onDismiss = { selectedGroupForDetails = null },
            onOpenChat = {
                val g = selectedGroupForDetails!!
                selectedGroupForDetails = null
                onOpenGroupChat(g.id, g.name)
            },
            onStartConference = {
                val g = selectedGroupForDetails!!
                selectedGroupForDetails = null
                viewModel.startCall("${g.name} (Conférence)", isVideo = true)
            },
            onAddMember = { userId ->
                viewModel.addGroupMember(selectedGroupForDetails!!.id, userId)
            },
            onRemoveMember = { memberId ->
                viewModel.removeGroupMember(selectedGroupForDetails!!.id, memberId)
            }
        )
    }
}

@Composable
fun GroupItemRow(
    group: Group,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBadge(
            name = group.name,
            avatarUrl = group.photoUrl,
            size = 52.dp,
            isGroup = true
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Currency Badge (USD, CDF, EUR)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OnChatCyan.copy(alpha = 0.15f))
                        .border(1.dp, OnChatCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = group.currency,
                        color = OnChatCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = group.description ?: "${group.membersCount} membres • Portefeuille OnChat",
                color = CashPayGrey,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String, desc: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val currencies = listOf("USD", "CDF", "EUR")
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un groupe CashPay", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du groupe", color = CashPayGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Devise du portefeuille :", color = CashPayGrey, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { curr ->
                        val isSelected = selectedCurrency == curr
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.background(brush = OnChatPrimaryGradient)
                                    else Modifier.background(CashPayNavyBorder)
                                )
                                .clickable { selectedCurrency = curr }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = curr,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optionnelle)", color = CashPayGrey) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedCurrency, desc.trim().ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet)
            ) {
                Text("Créer le groupe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = CashPayGrey)
            }
        },
        containerColor = CashPayNavySurface
    )
}

@Composable
fun GroupDetailsDialog(
    group: Group,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit,
    onStartConference: () -> Unit,
    onAddMember: (userId: Long) -> Unit,
    onRemoveMember: (memberId: Long) -> Unit
) {
    var showAddMemberInput by remember { mutableStateOf(false) }
    var newMemberUserId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBadge(name = group.name, size = 46.dp, isGroup = true)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(group.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Devise : ${group.currency}", color = OnChatCyan, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column {
                if (!group.description.isNullOrEmpty()) {
                    Text(group.description, color = CashPayGrey, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Conference Call Button
                Button(
                    onClick = onStartConference,
                    colors = ButtonDefaults.buttonColors(containerColor = CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = null, tint = OnChatCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appel de groupe", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chat button
                Button(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir la discussion", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = CashPayNavyBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Members management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Membres (${group.membersCount})", color = Color.White, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showAddMemberInput = !showAddMemberInput }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter", color = OnChatCyan, fontSize = 13.sp)
                    }
                }

                if (showAddMemberInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMemberUserId,
                            onValueChange = { newMemberUserId = it },
                            placeholder = { Text("Identifiant membre", color = CashPayGrey, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OnChatCyan
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val id = newMemberUserId.toLongOrNull()
                                if (id != null) {
                                    onAddMember(id)
                                    showAddMemberInput = false
                                    newMemberUserId = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = CashPayGrey)
            }
        },
        containerColor = CashPayNavySurface
    )
}
