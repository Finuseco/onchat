package com.example.ui.screens.reels

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

@SuppressLint("MissingPermission")
@Composable
fun ReelsCaptureScreen(onClose: () -> Unit, onMediaSelected: (String, String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var cameraAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var audioAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var photoMode by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var finalizing by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var limit by remember { mutableIntStateOf(60) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val photoCapture = remember { ImageCapture.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))).build()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        cameraAllowed = it[Manifest.permission.CAMERA] == true
        audioAllowed = it[Manifest.permission.RECORD_AUDIO] == true
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { val mime = context.contentResolver.getType(it).orEmpty(); if (mime.startsWith("video/") || mime.startsWith("image/")) onMediaSelected(it.toString(), if (mime.startsWith("image/")) "photo" else "video") }
    }
    LaunchedEffect(Unit) { if (!cameraAllowed) permission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
    DisposableEffect(owner, front, photoMode, cameraAllowed) {
        val future = ProcessCameraProvider.getInstance(context)
        var disposed = false
        ready = false
        if (cameraAllowed) future.addListener({
            if (!disposed) try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(owner, if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview, if (photoMode) photoCapture else videoCapture)
                ready = true
            } catch (error: Exception) { Toast.makeText(context, error.message ?: "Caméra indisponible", Toast.LENGTH_LONG).show() }
        }, executor)
        onDispose { disposed = true; if (future.isDone) runCatching { future.get().unbindAll() } }
    }
    DisposableEffect(Unit) { onDispose { recording?.close() } }
    LaunchedEffect(recording != null) {
        seconds = 0
        while (recording != null) { delay(1000); seconds++; if (seconds >= limit) { recording?.stop(); recording = null; finalizing = true } }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraAllowed) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onClose) { Text("Fermer", color = Color.White) }
                if (recording != null) Text("${seconds}s / ${limit}s", color = Color.Red)
                TextButton(enabled = recording == null && !finalizing, onClick = { front = !front }) { Text("Retourner", color = Color.White) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!cameraAllowed) Button(onClick = { permission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) { Text("Autoriser la caméra") }
                if (!audioAllowed && !photoMode) TextButton(onClick = { permission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) { Text("Micro désactivé : autoriser l’enregistrement sonore", color = Color.Yellow) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = photoMode, onClick = { if (recording == null) photoMode = true }, label = { Text("Photo") })
                    FilterChip(selected = !photoMode, onClick = { if (recording == null) photoMode = false }, label = { Text("Vidéo") })
                    if (!photoMode) listOf(15,60,600).forEach { value -> FilterChip(selected = limit == value, enabled = recording == null, onClick = { limit = value }, label = { Text(if (value == 600) "10 min" else "${value}s") }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(enabled = recording == null && !finalizing, onClick = { gallery.launch("*/*") }) { Text("Galerie") }
                    Button(enabled = ready && !finalizing, onClick = {
                        if (photoMode) {
                            val file = File(context.cacheDir, "photo_${java.util.UUID.randomUUID()}.jpg")
                            photoCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor, object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) { onMediaSelected(Uri.fromFile(file).toString(), "photo") }
                                override fun onError(error: ImageCaptureException) { file.delete(); Toast.makeText(context, error.message, Toast.LENGTH_LONG).show() }
                            })
                        } else if (recording != null) { recording?.stop(); recording = null; finalizing = true }
                        else {
                            val file = File(context.cacheDir, "capture_${java.util.UUID.randomUUID()}.mp4")
                            try {
                                val pending = videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
                                recording = (if (audioAllowed) pending.withAudioEnabled() else pending).start(executor) { event ->
                                    if (event is VideoRecordEvent.Finalize) {
                                        recording = null; finalizing = false
                                        if (!event.hasError() && file.length() > 0) onMediaSelected(Uri.fromFile(file).toString(), "video")
                                        else { file.delete(); Toast.makeText(context, "L’enregistrement vidéo a échoué (${event.error}).", Toast.LENGTH_LONG).show() }
                                    }
                                }
                            } catch (error: Exception) { file.delete(); Toast.makeText(context, error.message, Toast.LENGTH_LONG).show() }
                        }
                    }) { Text(if (photoMode) "Photographier" else if (recording != null) "Arrêter" else if (finalizing) "Finalisation…" else "Enregistrer") }
                }
            }
        }
    }
}
