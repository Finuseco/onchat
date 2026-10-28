package com.example.ui.screens.chats

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Conversation
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ImageViewerDialog(
    imageUrl: String,
    fileName: String? = null,
    senderName: String? = null,
    timestamp: Long = System.currentTimeMillis(),
    conversations: List<Conversation> = emptyList(),
    onDismiss: () -> Unit,
    onForward: (targetConversationIds: List<String>) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showForwardSheet by remember { mutableStateOf(false) }
    val selectedConversations = remember { mutableStateListOf<String>() }

    val formattedDate = remember(timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Interactive Image with Pinch-to-Zoom & Pan & Double-tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.8f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = fileName ?: "Image en plein écran",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = senderName ?: "Photo partagée",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formattedDate,
                            color = CashPayGrey,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset zoom if zoomed in
                    if (scale > 1.05f) {
                        IconButton(onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Réinitialiser le zoom",
                                tint = OnChatCyan
                            )
                        }
                    }

                    // Forward Button
                    IconButton(
                        onClick = {
                            selectedConversations.clear()
                            showForwardSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward,
                            contentDescription = "Transférer l'image",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Information Bar
            if (!fileName.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fileName,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (scale > 1f) {
                            Text(
                                text = "Zoom: ${String.format(Locale.US, "%.1fx", scale)}",
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Forward to multiple conversations modal dialog
            if (showForwardSheet) {
                AlertDialog(
                    onDismissRequest = { showForwardSheet = false },
                    title = {
                        Text(
                            text = "Transférer l'image",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Sélectionnez les conversations de destination :",
                                color = CashPayGrey,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (conversations.isEmpty()) {
                                Text(
                                    text = "Aucune conversation disponible.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(conversations, key = { it.id }) { conv ->
                                        val isSelected = selectedConversations.contains(conv.id)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) CashPayNavyBorder else CashPayNavyDark,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) OnChatCyan else CashPayNavyBorder.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) selectedConversations.remove(conv.id)
                                                    else selectedConversations.add(conv.id)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AvatarBadge(
                                                    name = conv.name,
                                                    avatarUrl = conv.avatarUrl,
                                                    size = 36.dp,
                                                    isGroup = conv.isGroup
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = conv.name,
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (conv.isGroup) "Groupe" else "Contact CashPay",
                                                        color = CashPayGrey,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        if (checked) selectedConversations.add(conv.id)
                                                        else selectedConversations.remove(conv.id)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = OnChatCyan,
                                                        checkmarkColor = CashPayNavyDark
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (selectedConversations.isNotEmpty()) {
                                    onForward(selectedConversations.toList())
                                    Toast.makeText(
                                        context,
                                        "Image transférée à ${selectedConversations.size} conversation(s)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showForwardSheet = false
                                }
                            },
                            enabled = selectedConversations.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan)
                        ) {
                            Text(
                                text = "Transférer (${selectedConversations.size})",
                                color = CashPayNavyDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showForwardSheet = false }) {
                            Text("Annuler", color = CashPayGrey)
                        }
                    },
                    containerColor = CashPayNavySurface
                )
            }
        }
    }
}
