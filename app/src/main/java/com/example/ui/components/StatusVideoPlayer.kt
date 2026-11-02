package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.theme.OnChatCyan
import com.example.ui.theme.OnChatMagenta
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun StatusVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    showWatermark: Boolean = true,
    thumbnailUrl: String? = null,
    onPosition: (Long) -> Unit = {},
    onVideoEnded: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val positionCallback by rememberUpdatedState(onPosition)
    var localIsPaused by remember { mutableStateOf(isPaused) }
    var buffering by remember(videoUrl) { mutableStateOf(true) }
    var firstFrame by remember(videoUrl) { mutableStateOf(false) }
    var playbackError by remember(videoUrl) { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                androidx.media3.datasource.DefaultDataSource.Factory(context,
                    androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(com.example.data.api.AuthenticatedMedia.client))
            )
        ).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE // Autoloop continuously
        }
    }

    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = !localIsPaused
    }

    LaunchedEffect(isPaused) {
        localIsPaused = isPaused
        exoPlayer.playWhenReady = !isPaused
    }

    // Keep localIsPaused state synchronized with exoplayer
    LaunchedEffect(localIsPaused) {
        exoPlayer.playWhenReady = !localIsPaused
    }

    // Tracking progress in real time (every 50ms)
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            delay(50)
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                positionCallback(currentPosition)
                duration = exoPlayer.duration
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { buffering = false; playbackError = true }
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
                if (playbackState == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                androidx.lifecycle.Lifecycle.Event.ON_START -> if (!localIsPaused) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Interactive Full Screen Box
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (playbackError) { playbackError = false; buffering = true; localIsPaused = false; exoPlayer.prepare() }
                else localIsPaused = !localIsPaused
            },
        contentAlignment = Alignment.Center
    ) {
        // Fullscreen Aspect Ratio Zoom Player
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!firstFrame && !thumbnailUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(model = thumbnailUrl, contentDescription = "Miniature de la vidéo", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (buffering && !localIsPaused) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.fillMaxSize(), color = OnChatCyan, strokeWidth = 4.dp)
                Icon(Icons.Default.PlayArrow, contentDescription = "Chargement de la vidéo", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        if (playbackError) Text("Lecture interrompue. Touchez pour réessayer.", color = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.7f)).padding(12.dp))
        // Subtle glowing Pause indicator
        AnimatedVisibility(
            visible = localIsPaused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Pause",
                    tint = OnChatCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Official Two-Tone OnChat Signature Watermark (Top-Left side, non-intrusive)
        if (showWatermark) {
            OnChatSignatureWatermark(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 88.dp),
                fontSize = 17.sp,
                withBackground = true,
                animatedGlow = false
            )
        }

        // Precise looping progress bar at the bottom of the video player (Real Progression)
        val progressPercent = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressPercent.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(
                            listOf(OnChatCyan, OnChatMagenta)
                        )
                    )
            )
        }
    }
}
