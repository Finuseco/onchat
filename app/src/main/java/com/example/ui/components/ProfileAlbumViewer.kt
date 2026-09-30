package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ProfileMediaItem
import kotlinx.coroutines.launch

@Composable
fun ProfileAlbumViewer(
    mediaList: List<ProfileMediaItem>,
    initialIndex: Int = 0,
    isSelfProfile: Boolean = true,
    onDismiss: () -> Unit,
    onSetAsProfile: (Long) -> Unit = {},
    onSetAsCover: (Long) -> Unit = {}
) {
    if (mediaList.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, mediaList.size - 1)) }
    var showMenu by remember { mutableStateOf(false) }
    val thumbnailListState = rememberLazyListState()

    val currentItem = mediaList.getOrNull(currentIndex) ?: mediaList.first()

    // Scroll thumbnail list when index changes
    LaunchedEffect(currentIndex) {
        coroutineScope.launch {
            thumbnailListState.animateScrollToItem(
                (currentIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Main Full-Bleed Image with Horizontal Drag Detection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(mediaList.size) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag < -60f && currentIndex < mediaList.size - 1) {
                                    currentIndex++
                                } else if (totalDrag > 60f && currentIndex > 0) {
                                    currentIndex--
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "FullPhotoAnimation"
                ) { item ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.displayUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo ${item.id}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Subtle gradient scrim at bottom to ensure thumbnail strip visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Subtle gradient scrim at top for status bar and actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                )
            }

            // Top Bar: Back, Share, More Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (circle with translucent dark background)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Right Actions: Share + More options
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                try {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, currentItem.displayUrl)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Partager la photo"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lien copié : ${currentItem.displayUrl}", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Options Button (...)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            if (isSelfProfile) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Définir comme photo de profil",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF44BBF5)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSetAsProfile(currentItem.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Définir comme photo de couverture",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Panorama,
                                            contentDescription = null,
                                            tint = Color(0xFF562EBF)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSetAsCover(currentItem.id)
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Informations média (#${currentItem.id})",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    Toast.makeText(
                                        context,
                                        "ID: ${currentItem.id} • ${currentItem.type} • ${currentItem.visibility}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Horizontal Thumbnails Carousel (Exact look of the user's screenshot)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators: Badge if current photo is profile or cover
                if (currentItem.isProfile || currentItem.isCover) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        if (currentItem.isProfile) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1D8AE4))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Photo de Profil", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (currentItem.isCover) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF562EBF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Couverture", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Thumbnail Strip
                LazyRow(
                    state = thumbnailListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(mediaList) { index, item ->
                        val isSelected = index == currentIndex
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.DarkGray)
                                .clickable { currentIndex = index }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.displayUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Miniature ${item.id}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
