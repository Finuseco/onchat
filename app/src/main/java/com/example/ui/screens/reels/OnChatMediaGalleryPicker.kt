package com.example.ui.screens.reels

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaGalleryItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationSeconds: Int = 0,
    val bucketName: String = "Appareil",
    val dateAdded: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnChatMediaGalleryPicker(
    onDismiss: () -> Unit,
    onMediaSelected: (String, String) -> Unit // (uriString, "video" | "image")
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Tous") } // "Tous", "Vidéos", "Photos", "Dossiers"
    var selectedBucket by remember { mutableStateOf<String?>(null) }
    var mediaItems by remember { mutableStateOf<List<MediaGalleryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load real media files from device MediaStore
    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val list = mutableListOf<MediaGalleryItem>()

            // 1. Query Videos
            try {
                val videoProjection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED
                )
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoProjection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                    while (cursor.moveToNext() && list.size < 150) {
                        val id = cursor.getLong(idCol)
                        val durMs = if (durCol >= 0) cursor.getLong(durCol) else 0L
                        val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Vidéos" else "Vidéos"
                        val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        list.add(
                            MediaGalleryItem(
                                id = id,
                                uri = contentUri,
                                isVideo = true,
                                durationSeconds = (durMs / 1000).toInt(),
                                bucketName = bucket,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryPicker", "Error querying videos: ${e.message}")
            }

            // 2. Query Images
            try {
                val imageProjection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                )
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageProjection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                    while (cursor.moveToNext() && list.size < 300) {
                        val id = cursor.getLong(idCol)
                        val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Photos" else "Photos"
                        val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        list.add(
                            MediaGalleryItem(
                                id = id,
                                uri = contentUri,
                                isVideo = false,
                                durationSeconds = 0,
                                bucketName = bucket,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryPicker", "Error querying images: ${e.message}")
            }

            // Sort all by dateAdded descending
            list.sortByDescending { it.dateAdded }
            mediaItems = list
        }
        isLoading = false
    }

    // Extracted Unique Folders (Buckets)
    val availableBuckets = remember(mediaItems) {
        mediaItems.map { it.bucketName }.distinct().sorted()
    }

    // Filtered list based on active tab and bucket
    val filteredItems = remember(mediaItems, selectedFilter, selectedBucket) {
        mediaItems.filter { item ->
            val matchesType = when (selectedFilter) {
                "Vidéos" -> item.isVideo
                "Photos" -> !item.isVideo
                else -> true
            }
            val matchesBucket = if (selectedBucket != null) item.bucketName == selectedBucket else true
            matchesType && matchesBucket
        }
    }

    Scaffold(
        containerColor = CashPayNavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gestionnaire de médias OnChat",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredItems.size} éléments trouvés",
                            color = CashPayGrey,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CashPayNavySurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Filter Tabs ("Tous", "Vidéos", "Photos", "Dossiers")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CashPayNavySurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tous", "Vidéos", "Photos", "Dossiers").forEach { tab ->
                    val isSelected = selectedFilter == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) OnChatVioletDeep else Color.White.copy(alpha = 0.05f))
                            .clickable {
                                selectedFilter = tab
                                if (tab != "Dossiers" && selectedBucket != null) {
                                    selectedBucket = null
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else CashPayGrey,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Folders (Buckets) horizontal chip bar if "Dossiers" or if multiple buckets exist
            if (selectedFilter == "Dossiers" || availableBuckets.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedBucket == null,
                            onClick = { selectedBucket = null },
                            label = { Text("Tous les dossiers") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OnChatCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                    items(availableBuckets) { bucket ->
                        FilterChip(
                            selected = selectedBucket == bucket,
                            onClick = { selectedBucket = if (selectedBucket == bucket) null else bucket },
                            label = { Text(bucket) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OnChatCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Media Grid View
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OnChatCyan)
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = null,
                            tint = CashPayGrey,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucun fichier multimédia trouvé",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Autorisez l'accès aux photos et vidéos ou enregistrez un média avec la caméra.",
                            color = CashPayGrey,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF27272A))
                                .clickable {
                                    val type = if (item.isVideo) "video" else "image"
                                    onMediaSelected(item.uri.toString(), type)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Overlay badge for video with duration
                            if (item.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        val min = item.durationSeconds / 60
                                        val sec = item.durationSeconds % 60
                                        Text(
                                            text = String.format("%02d:%02d", min, sec),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
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
}
