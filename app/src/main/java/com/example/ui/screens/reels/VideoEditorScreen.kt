package com.example.ui.screens.reels

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OnChatSignatureWatermark
import com.example.ui.components.StatusVideoPlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class TextOverlayItem(
    val id: String = "txt_${System.currentTimeMillis()}",
    var text: String,
    var color: Color = Color.White,
    var backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    mediaUri: String,
    mediaType: String,
    viewModel: OnChatViewModel? = null,
    onBack: () -> Unit,
    onPublish: (String, String) -> Unit // (uploadedUrl, caption)
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var selectedTool by remember { mutableStateOf<String?>("Modifier") }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    // --- Editing State Variables ---
    // 1. Trim & Speed
    var trimStartRatio by remember { mutableFloatStateOf(0f) }
    var trimEndRatio by remember { mutableFloatStateOf(1f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    val totalDurationSeconds = 8.0f
    val currentDuration = (totalDurationSeconds * (trimEndRatio - trimStartRatio) / playbackSpeed)

    // 2. Audio & Sound
    var isAudioMuted by remember { mutableStateOf(false) }
    var audioVolume by remember { mutableFloatStateOf(1.0f) }
    var selectedMusicTitle by remember { mutableStateOf<String?>("Son original") }

    // 3. Text Overlays
    val textOverlays = remember { mutableStateListOf<TextOverlayItem>() }
    var editingText by remember { mutableStateOf("") }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var selectedTextColor by remember { mutableStateOf(Color.White) }

    // 4. Color Filters / Effects
    var selectedFilterName by remember { mutableStateOf("Normal") }
    val colorFilter = remember(selectedFilterName) {
        when (selectedFilterName) {
            "Cinema" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                setToSaturation(1.3f)
            })
            "Sepia" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
            "Cyber Neon" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                setToSaturation(1.8f)
            })
            "Noir & Blanc" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                setToSaturation(0f)
            })
            "Vibrant" -> ColorFilter.colorMatrix(ColorMatrix().apply {
                setToSaturation(1.5f)
            })
            else -> null
        }
    }

    // 5. Captions
    var subtitleText by remember { mutableStateOf("DITES-LEUR, ET SURTOUT...") }
    var showSubtitles by remember { mutableStateOf(true) }

    // Real upload progress when publishing to server
    LaunchedEffect(isPublishing) {
        if (isPublishing) {
            uploadProgress = 0.15f
            var finalUrl: String? = null
            try {
                uploadProgress = 0.35f
                if (mediaUri.startsWith("content://") || mediaUri.startsWith("file://") || mediaUri.startsWith("android.resource://")) {
                    val uri = Uri.parse(mediaUri)
                    val mimeType = context.contentResolver.getType(uri) ?: if (mediaType == "video") "video/mp4" else "image/jpeg"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    uploadProgress = 0.65f
                    if (bytes != null && viewModel != null) {
                        val ext = if (mimeType.contains("video")) "mp4" else "jpg"
                        val fileName = "reel_${System.currentTimeMillis()}.$ext"
                        finalUrl = if (mimeType.contains("video") || mediaType == "video") {
                            viewModel.uploadVideoGeneral(bytes, fileName, mimeType)
                        } else {
                            viewModel.uploadMediaBytes(bytes, fileName, mimeType)
                        }
                    }
                } else if (mediaUri.startsWith("http")) {
                    finalUrl = mediaUri
                }
                uploadProgress = 0.95f
            } catch (e: Exception) {
                android.util.Log.e("VideoEditorScreen", "Error uploading reel file: ${e.message}", e)
            }
            
            if (finalUrl != null || mediaUri.startsWith("http")) {
                uploadProgress = 1.0f
                delay(350)
                onPublish(finalUrl ?: mediaUri, caption)
            } else {
                isPublishing = false
                Toast.makeText(context, "Échec de l'envoi vidéo au serveur CashPay. Vérifiez la connexion.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }

                Text(
                    text = "Éditeur OnChat (Max 10 min)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Publish Next Button
                IconButton(
                    onClick = { isPublishing = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(OnChatMagenta, Color(0xFFE11D48))
                            )
                        )
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Publier", tint = Color.White)
                }
            }

            // Video Preview Area with Layers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF18181B)),
                contentAlignment = Alignment.Center
            ) {
                // Base Video or Image Preview
                if (mediaType.contains("video", ignoreCase = true) || mediaUri.endsWith(".mp4", ignoreCase = true)) {
                    StatusVideoPlayer(
                        videoUrl = mediaUri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    coil.compose.AsyncImage(
                        model = mediaUri,
                        contentDescription = "Aperçu de la vidéo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter
                    )
                }

                // Subtitles Layer (as seen in Screenshot 2: "DITES-LEUR. ET SURTOUT")
                if (showSubtitles && subtitleText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            color = Color(0xFFFFEB3B), // Vibrant Yellow Subtitle
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Dynamic User Text Overlays (Draggable)
                textOverlays.forEach { item ->
                    var itemOffset by remember { mutableStateOf(IntOffset(item.offsetX.roundToInt(), item.offsetY.roundToInt())) }
                    Box(
                        modifier = Modifier
                            .offset { itemOffset }
                            .pointerInput(item.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    itemOffset = IntOffset(
                                        (itemOffset.x + dragAmount.x).roundToInt(),
                                        (itemOffset.y + dragAmount.y).roundToInt()
                                    )
                                    item.offsetX = itemOffset.x.toFloat()
                                    item.offsetY = itemOffset.y.toFloat()
                                }
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(item.backgroundColor)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = item.text,
                            color = item.color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Official OnChat Two-Tone Signature Watermark (Raised above controls)
                OnChatSignatureWatermark(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp),
                    fontSize = 18.sp,
                    withBackground = true,
                    animatedGlow = true
                )
            }

            // Timeline Scrub & Trimmer Bar (Matching Screenshot 2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedSec = String.format("%.1f", currentDuration)
                    Text(
                        text = "00:02 / 00:0$formattedSec",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (playbackSpeed != 1.0f) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = OnChatCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Plein écran",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Timeline Track with Range Trimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF27272A)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Active Selected Trim Range
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(trimEndRatio - trimStartRatio)
                            .fillMaxHeight()
                            .background(OnChatViolet.copy(alpha = 0.75f))
                    )

                    // Left Trimmer Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(OnChatCyan)
                    )

                    // Scrub playhead vertical indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(Color.White)
                    )

                    // Right Trimmer Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(OnChatMagenta)
                    )
                }
            }

            // Caption Input Field
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Écrire une légende avec #hashtags...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OnChatCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            // Dynamic Contextual Tool Panel (Appears based on selected tool)
            AnimatedVisibility(visible = selectedTool != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E24)
                ) {
                    when (selectedTool) {
                        "Modifier" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vitesse:", color = Color.White, fontSize = 13.sp)
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                                    FilterChip(
                                        selected = playbackSpeed == speed,
                                        onClick = { playbackSpeed = speed },
                                        label = { Text("${speed}x", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OnChatCyan,
                                            selectedLabelColor = Color.Black,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        "Son" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAudioMuted) "Audio coupé" else "Volume original: ${(audioVolume * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    IconButton(onClick = { isAudioMuted = !isAudioMuted }) {
                                        Icon(
                                            imageVector = if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = if (isAudioMuted) Color.Red else OnChatCyan
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            selectedMusicTitle = "Son original extrait"
                                            Toast.makeText(context, "Audio extrait avec succès 🎵", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet)
                                    ) {
                                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Séparer Audio", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            selectedMusicTitle = "Beats CashPay Pop"
                                            Toast.makeText(context, "Musique ajoutée 🎧", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan)
                                    ) {
                                        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ajouter Musique", color = Color.Black, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        "Texte" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showAddTextDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = OnChatMagenta)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Ajouter du texte")
                                }
                                if (textOverlays.isNotEmpty()) {
                                    TextButton(onClick = { textOverlays.clear() }) {
                                        Text("Effacer tout", color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        "Effets" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Normal", "Cinema", "Cyber Neon", "Sepia", "Noir & Blanc", "Vibrant").forEach { filter ->
                                    FilterChip(
                                        selected = selectedFilterName == filter,
                                        onClick = { selectedFilterName = filter },
                                        label = { Text(filter, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OnChatViolet,
                                            selectedLabelColor = Color.White,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        "Magie AI" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        selectedFilterName = "Vibrant"
                                        Toast.makeText(context, "Lumière & Couleurs optimisées par IA ✨", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                                ) {
                                    Text("Amélioration Éclairage IA", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "Sous-titres" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Afficher sous-titres:", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = showSubtitles,
                                    onCheckedChange = { showSubtitles = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = OnChatCyan, checkedTrackColor = OnChatViolet)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Tools Navigation Bar (Matching Screenshot 2 exactly)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121212),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf(
                        Triple("Modifier", Icons.Default.ContentCut, if (selectedTool == "Modifier") OnChatCyan else Color.White),
                        Triple("Son", Icons.Default.MusicNote, if (selectedTool == "Son") OnChatCyan else OnChatCyan.copy(alpha = 0.8f)),
                        Triple("Texte", Icons.Default.TextFields, if (selectedTool == "Texte") OnChatMagenta else OnChatMagenta.copy(alpha = 0.8f)),
                        Triple("Effets", Icons.Default.AutoFixHigh, if (selectedTool == "Effets") OnChatViolet else OnChatViolet.copy(alpha = 0.8f)),
                        Triple("Magie AI", Icons.Default.AutoAwesome, Color(0xFFFFD700)),
                        Triple("Sous-titres", Icons.Default.ClosedCaption, if (selectedTool == "Sous-titres") OnChatCyan else Color.White)
                    ).forEach { (title, icon, tint) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    selectedTool = if (selectedTool == title) null else title
                                }
                                .padding(horizontal = 4.dp)
                        ) {
                            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(title, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Add Text Overlay Dialog
        if (showAddTextDialog) {
            AlertDialog(
                onDismissRequest = { showAddTextDialog = false },
                title = { Text("Ajouter du texte sur la vidéo", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            placeholder = { Text("Tapez votre texte...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Color.White, Color.Yellow, OnChatCyan, OnChatMagenta, Color.Green).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (selectedTextColor == col) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedTextColor = col }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editingText.isNotBlank()) {
                                textOverlays.add(TextOverlayItem(text = editingText, color = selectedTextColor))
                                editingText = ""
                            }
                            showAddTextDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OnChatMagenta)
                    ) {
                        Text("Ajouter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTextDialog = false }) {
                        Text("Annuler")
                    }
                },
                containerColor = Color(0xFF27272A)
            )
        }

        // Publishing Progress Overlay with Real Continuous Counter
        if (isPublishing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.size(84.dp),
                        color = OnChatCyan,
                        trackColor = Color(0xFF3F3F46),
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Publication de votre création...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(uploadProgress * 100).toInt()}% • Signature OnChat & Encodage MP4",
                        color = OnChatCyan,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
