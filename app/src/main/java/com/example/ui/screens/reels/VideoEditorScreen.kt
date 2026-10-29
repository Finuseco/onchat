package com.example.ui.screens.reels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.media.VideoExporter
import com.example.data.model.SubtitleCue
import com.example.data.model.ReusableSound
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
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
    reusedSound: ReusableSound? = null,
    onBack: () -> Unit,
    onPublish: (String, String) -> Unit // (uploadedUrl, caption)
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var selectedTool by remember { mutableStateOf<String?>("Modifier") }
    var isPublishing by remember { mutableStateOf(false) }
    val transferProgress = viewModel?.uploadProgress?.collectAsState()?.value ?: 0f
    var exportPhase by remember { mutableStateOf("Préparation du montage") }
    val editorScope = rememberCoroutineScope()
    val sourceIsPhoto = mediaType.contains("photo", true) || mediaType.contains("image", true)
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var secondsPerPhoto by remember { mutableFloatStateOf(3f) }
    var outputHeight by remember { mutableIntStateOf(720) }
    var allowReuse by remember { mutableStateOf(false) }
    var rightsConfirmed by remember { mutableStateOf(false) }
    var selectedSound by remember { mutableStateOf(reusedSound) }
    var previewPosition by remember { mutableLongStateOf(0L) }
    var renderedPreview by remember { mutableStateOf<java.io.File?>(null) }
    var renderingPreview by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { renderedPreview?.delete() } }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { photos = it.take(100) }

    // --- Editing State Variables ---
    // 1. Trim & Speed
    var trimStartRatio by remember { mutableFloatStateOf(0f) }
    var trimEndRatio by remember { mutableFloatStateOf(1f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var totalDurationSeconds by remember { mutableFloatStateOf(0f) }
    var previewSize by remember { mutableStateOf(IntSize(1, 1)) }
    LaunchedEffect(mediaUri) {
        try { totalDurationSeconds = if (sourceIsPhoto) secondsPerPhoto else VideoExporter.durationMs(context, Uri.parse(mediaUri)) / 1000f }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { Toast.makeText(context, "Impossible de lire la durée de la vidéo", Toast.LENGTH_LONG).show() }
    }
    val currentDuration = (if (sourceIsPhoto) secondsPerPhoto else totalDurationSeconds * (trimEndRatio - trimStartRatio) / playbackSpeed) + photos.size * secondsPerPhoto

    // 2. Audio & Sound
    var isAudioMuted by remember { mutableStateOf(false) }
    var audioVolume by remember { mutableFloatStateOf(1.0f) }
    var musicVolume by remember { mutableFloatStateOf(0.7f) }
    var selectedMusicTitle by remember { mutableStateOf(reusedSound?.let { "${it.title} • ${it.authorName}" } ?: "Son original") }
    var selectedMusicUri by remember { mutableStateOf<Uri?>(reusedSound?.let { Uri.parse(it.url) }) }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedSound = null
        rightsConfirmed = false
        selectedMusicUri = uri
        selectedMusicTitle = if (uri == null) "Son original" else "Fichier audio sélectionné"
    }

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
    var subtitleCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var showSubtitles by remember { mutableStateOf(false) }

    // 6. AI Magic Prompt State
    var aiPrompt by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }

    fun askJimmy(transcribe: Boolean) {
        if (isAiGenerating || viewModel == null) return
        editorScope.launch {
            isAiGenerating = true
            try {
                val clipSeconds = if (sourceIsPhoto) currentDuration else totalDurationSeconds * (trimEndRatio - trimStartRatio)
                check(clipSeconds in 0.001f..600f) { "Réduisez la sélection à 10 minutes maximum." }
                val audio = if (transcribe && !sourceIsPhoto) VideoExporter.audioForJimmy(context, Uri.parse(mediaUri), (totalDurationSeconds * trimStartRatio * 1000).toLong(), (totalDurationSeconds * trimEndRatio * 1000).toLong()) else null
                check(!transcribe || audio != null) { "Choisissez une vidéo avec une piste parlée." }
                val plan = viewModel.editorPlan(if (transcribe) "Transcris toutes les paroles en sous-titres synchronisés. Conserve le style naturel." else aiPrompt, clipSeconds.toDouble(), audio)
                selectedFilterName = plan.filter
                playbackSpeed = if (transcribe) 1f else plan.speed
                audioVolume = plan.voiceVolume
                if (plan.caption.isNotBlank()) caption = plan.caption
                if (audio != null) {
                    val offset = totalDurationSeconds * trimStartRatio
                    subtitleCues = plan.subtitles.map { it.copy(start = it.start + offset, end = it.end + offset) }
                    showSubtitles = true
                    subtitleText = ""
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(context, e.message ?: "Jimmy indisponible", Toast.LENGTH_LONG).show() }
            finally { isAiGenerating = false }
        }
    }

    fun currentEdits(): VideoExporter.Edits {
                val labels = textOverlays.map { label ->
                    VideoExporter.Label(label.text, label.color.toArgb(), label.backgroundColor.toArgb(),
                        2f * label.offsetX / previewSize.width, -2f * label.offsetY / previewSize.height)
                }.toMutableList()
                if (caption.isNotBlank()) labels.add(VideoExporter.Label(caption, android.graphics.Color.WHITE, 0x99000000.toInt(), 0f, -0.85f))
                if (showSubtitles && subtitleText.isNotBlank()) {
                    labels.add(VideoExporter.Label(subtitleText, android.graphics.Color.WHITE, 0x99000000.toInt(), 0f, -0.65f))
                }
        return VideoExporter.Edits(trimStartRatio, trimEndRatio, playbackSpeed, audioVolume, isAudioMuted,
                        selectedFilterName, labels, selectedMusicUri, musicVolume,
                        photos = photos, sourceIsPhoto = sourceIsPhoto, photoDurationMs = (secondsPerPhoto * 1000).toLong(), outputHeight = outputHeight, subtitles = if (showSubtitles) subtitleCues else emptyList())
    }

    LaunchedEffect(isPublishing) {
        if (isPublishing) {
            var exported: java.io.File? = null
            try {
                check(viewModel != null) { "Publication indisponible" }
                check(totalDurationSeconds > 0) { "Durée de vidéo indisponible" }
                check(currentDuration <= 600) { "Le montage dépasse 10 minutes. Réduisez la sélection ou les photos." }
                check(!allowReuse || rightsConfirmed) { "Confirmez les droits du son avant de proposer sa réutilisation." }
                exportPhase = "Export du montage réel"
                val output = VideoExporter.export(context, Uri.parse(mediaUri), (totalDurationSeconds * 1000).toLong(), currentEdits())
                exported = output
                check(output.length() in 1..(200L * 1024 * 1024)) { "La vidéo exportée dépasse 200 Mo" }
                exportPhase = "Envoi de la vidéo"
                val finalUrl = viewModel.publishEditedVideo(output, allowReuse && selectedSound == null, rightsConfirmed, selectedSound?.sourceId, selectedMusicTitle)
                onPublish(finalUrl, caption)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isPublishing = false
                Toast.makeText(context, e.message ?: "Échec de la publication", Toast.LENGTH_LONG).show()
            } finally { exported?.delete() }
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
                    enabled = !isPublishing && !renderingPreview && totalDurationSeconds > 0 && currentDuration <= 600,
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
                    .background(Color(0xFF18181B))
                    .onSizeChanged { previewSize = it },
                contentAlignment = Alignment.Center
            ) {
                // Base Video or Image Preview
                if (mediaType.contains("video", ignoreCase = true) || mediaUri.endsWith(".mp4", ignoreCase = true)) {
                    StatusVideoPlayer(
                        videoUrl = mediaUri,
                        onPosition = { previewPosition = it },
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

                if (showSubtitles && subtitleCues.isNotEmpty()) {
                    val cue = subtitleCues.firstOrNull { previewPosition / 1000.0 >= it.start && previewPosition / 1000.0 < it.end }
                    cue?.let { Text(it.text, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).background(Color.Black.copy(alpha = 0.7f)), textAlign = TextAlign.Center) }
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

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { photoPicker.launch("image/*") }, label = { Text("Photos (${photos.size})") })
                AssistChip(enabled = !renderingPreview && !isPublishing && currentDuration <= 600, onClick = {
                    editorScope.launch {
                        renderingPreview = true
                        try { renderedPreview?.delete(); renderedPreview = VideoExporter.export(context, Uri.parse(mediaUri), (totalDurationSeconds * 1000).toLong(), currentEdits()) }
                        catch (error: CancellationException) { throw error }
                        catch (error: Exception) { Toast.makeText(context, error.message ?: "Rendu impossible", Toast.LENGTH_LONG).show() }
                        finally { renderingPreview = false }
                    }
                }, label = { Text(if (renderingPreview) "Rendu…" else "Aperçu rendu") })
                FilterChip(selected = outputHeight == 720, onClick = { outputHeight = 720 }, label = { Text("720p • léger") })
                FilterChip(selected = outputHeight == 1080, onClick = { outputHeight = 1080 }, label = { Text("1080p") })
                Text("${currentDuration.toInt()} s / 600 s", color = if (currentDuration > 600) Color.Red else Color.White, modifier = Modifier.padding(12.dp))
            }
            if (sourceIsPhoto || photos.isNotEmpty()) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${secondsPerPhoto.toInt()} s/photo", color = Color.White, fontSize = 11.sp)
                Slider(value = secondsPerPhoto, onValueChange = { secondsPerPhoto = it }, valueRange = 1f..10f, steps = 8, modifier = Modifier.weight(1f))
            }
            if (selectedSound != null) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    coil.compose.AsyncImage(model = selectedSound?.authorAvatar, contentDescription = "Auteur du son", modifier = Modifier.size(28.dp).clip(CircleShape))
                    Text("Son : ${selectedSound?.title} • ${selectedSound?.authorName}", color = OnChatCyan, fontSize = 11.sp)
                }
            } else Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allowReuse, onCheckedChange = { allowReuse = it })
                Text("Autoriser la réutilisation de mon son avec crédit", color = Color.White, fontSize = 11.sp)
            }
            if (allowReuse && selectedSound == null) Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rightsConfirmed, onCheckedChange = { rightsConfirmed = it })
                Text("Je détiens les droits et autorise cette utilisation sur OnChat", color = Color.White, fontSize = 10.sp)
            }

            // Caption Text Input Box
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Texte à afficher sur la vidéo...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
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
                                    FilterChip(selected = selectedMusicUri == null,
                                        onClick = { selectedMusicUri = null; selectedMusicTitle = "Son original"; selectedSound = null },
                                        label = { Text("Son original") })
                                    FilterChip(selected = selectedMusicUri != null,
                                        onClick = { musicPicker.launch("audio/*") },
                                        label = { Text("Choisir une musique") })
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isAudioMuted, onCheckedChange = { isAudioMuted = it })
                                    Text("Couper le son original", color = Color.White, fontSize = 11.sp)
                                }
                                if (selectedMusicUri != null) Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Musique", color = Color.White, fontSize = 11.sp)
                                    Slider(value = musicVolume, onValueChange = { musicVolume = it }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
                                    Text("${(musicVolume * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
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
                                            askJimmy(false)
                                        },
                                        enabled = !isAiGenerating && aiPrompt.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (isAiGenerating) "Jimmy…" else "Appliquer Jimmy", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                OutlinedButton(onClick = { askJimmy(true) }, enabled = !sourceIsPhoto && !isAiGenerating) { Text(if (isAiGenerating) "Analyse Jimmy…" else "Transcrire la parole avec Jimmy") }
                                if (subtitleCues.isNotEmpty()) Text("${subtitleCues.size} sous-titres synchronisés • vérifiez le texte avant publication", color = OnChatCyan, fontSize = 11.sp)
                                if (showSubtitles) {
                                    OutlinedTextField(
                                        value = subtitleText,
                                        onValueChange = { subtitleText = it; subtitleCues = emptyList() },
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

        renderedPreview?.let { preview ->
            androidx.compose.ui.window.Dialog(onDismissRequest = { renderedPreview = null; preview.delete() }) {
                Column(Modifier.fillMaxWidth().background(Color.Black)) {
                    StatusVideoPlayer(videoUrl = Uri.fromFile(preview).toString(), modifier = Modifier.fillMaxWidth().height(440.dp), showWatermark = false)
                    TextButton(onClick = { renderedPreview = null; preview.delete() }) { Text("Revenir au montage") }
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
                        text = if (exportPhase == "Envoi de la vidéo") "${(transferProgress * 100).toInt()} % envoyés — confirmation serveur requise" else exportPhase,
                        color = OnChatCyan,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
