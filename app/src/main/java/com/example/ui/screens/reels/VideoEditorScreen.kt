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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
    var musicVolume by remember { mutableFloatStateOf(0.7f) }
    var selectedMusicTitle by remember { mutableStateOf("Son original") }

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
    var subtitleText by remember { mutableStateOf("") }
    var showSubtitles by remember { mutableStateOf(false) }

    // 6. AI Magic Prompt State
    var aiPrompt by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }

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
                Toast.makeText(context, "Échec de l'envoi vidéo au serveur. Vérifiez la connexion.", Toast.LENGTH_LONG).show()
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
                    text = "Éditeur Vidéo OnChat",
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
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Publier", tint = Color.White)
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

                // Subtitles Layer (Dynamic)
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
                            color = Color(0xFFFFEB3B),
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
            }

            // Timeline Scrub & Trimmer Bar
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
                    val startSec = String.format("%.1f", totalDurationSeconds * trimStartRatio)
                    Text(
                        text = "00:0$startSec / 00:0$formattedSec (${((trimEndRatio - trimStartRatio) * 100).toInt()}%)",
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(trimEndRatio - trimStartRatio)
                            .fillMaxHeight()
                            .background(OnChatViolet.copy(alpha = 0.75f))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(18.dp)
                            .fillMaxHeight()
                            .background(OnChatCyan)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newStart = (trimStartRatio + dragAmount.x / 400f).coerceIn(0f, trimEndRatio - 0.1f)
                                    trimStartRatio = newStart
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(18.dp)
                            .fillMaxHeight()
                            .background(OnChatMagenta)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newEnd = (trimEndRatio + dragAmount.x / 400f).coerceIn(trimStartRatio + 0.1f, 1f)
                                    trimEndRatio = newEnd
                                }
                            }
                    )
                }
            }

            // Caption Text Input Box
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Écrire une légende avec #hashtags...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = OnChatCyan,
                    unfocusedBorderColor = Color(0xFF3F3F46),
                    focusedContainerColor = Color(0xFF18181B),
                    unfocusedContainerColor = Color(0xFF18181B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                maxLines = 2
            )

            // Dynamic Tool Action Panels
            AnimatedVisibility(
                visible = selectedTool != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1F1F23)
                ) {
                    when (selectedTool) {
                        "Modifier" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vitesse:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                                    val isSelected = playbackSpeed == speed
                                    Button(
                                        onClick = { playbackSpeed = speed },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) OnChatCyan else Color(0xFF3F3F46)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("${speed}x", color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        "Son" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Piste audio active : $selectedMusicTitle", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Son original", "Afrobeat Mix", "Cinematic Bounce", "Future Chill").forEach { track ->
                                        FilterChip(
                                            selected = selectedMusicTitle == track,
                                            onClick = { selectedMusicTitle = track },
                                            label = { Text(track, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = OnChatCyan,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Volume voix:", color = Color.White, fontSize = 12.sp)
                                    Slider(
                                        value = audioVolume,
                                        onValueChange = { audioVolume = it },
                                        valueRange = 0f..2f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = OnChatCyan, activeTrackColor = OnChatCyan)
                                    )
                                    Text("${(audioVolume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                        "Texte" -> {
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
                                    Button(
                                        onClick = { showAddTextDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatMagenta),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ajouter du texte", fontSize = 12.sp)
                                    }
                                    if (textOverlays.isNotEmpty()) {
                                        TextButton(onClick = { textOverlays.clear() }) {
                                            Text("Effacer tout", color = Color.Red, fontSize = 12.sp)
                                        }
                                    }
                                }
                                if (textOverlays.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        textOverlays.forEachIndexed { idx, overlay ->
                                            AssistChip(
                                                onClick = { textOverlays.removeAt(idx) },
                                                label = { Text(overlay.text, fontSize = 11.sp) },
                                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            )
                                        }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = aiPrompt,
                                        onValueChange = { aiPrompt = it },
                                        placeholder = { Text("Instruction IA (ex: Couleurs vibrantes & légende pro)", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            selectedFilterName = "Vibrant"
                                            if (caption.isBlank()) {
                                                caption = "Création exclusive sur OnChat Shorts ✨ #OnChat #CashPay #Viral"
                                            }
                                            if (subtitleText.isBlank()) {
                                                subtitleText = "DÉCOUVREZ L'INNOVATION ONCHAT"
                                                showSubtitles = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Appliquer IA", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        "Sous-titres" -> {
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
                                    Text("Activer sous-titres :", color = Color.White, fontSize = 12.sp)
                                    Switch(
                                        checked = showSubtitles,
                                        onCheckedChange = { showSubtitles = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = OnChatCyan, checkedTrackColor = OnChatViolet)
                                    )
                                }
                                if (showSubtitles) {
                                    OutlinedTextField(
                                        value = subtitleText,
                                        onValueChange = { subtitleText = it },
                                        placeholder = { Text("Texte des sous-titres...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Tools Navigation Bar
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

        // Publishing Progress Overlay
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
                        text = "${(uploadProgress * 100).toInt()}% • Traitement & Encodage MP4",
                        color = OnChatCyan,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
