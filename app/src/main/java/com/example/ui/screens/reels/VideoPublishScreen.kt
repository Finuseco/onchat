package com.example.ui.screens.reels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CashPayNavyDark
import com.example.ui.viewmodel.OnChatViewModel

/** Every publication passes through the same renderer and confirmed server upload. */
@Composable
fun VideoPublishScreen(viewModel: OnChatViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected = it }
    val uri = selected
    if (uri != null) {
        val type = context.contentResolver.getType(uri).orEmpty()
        VideoEditorScreen(mediaUri = uri.toString(), mediaType = if (type.startsWith("image/")) "photo" else "video", viewModel = viewModel, onBack = { selected = null }, onPublish = { _, _ -> viewModel.fetchVideosFeed(); onSuccess() })
    } else Column(Modifier.fillMaxSize().background(CashPayNavyDark).statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onBack) { Text("Retour") }
        Text("Créer une vidéo OnChat", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text("Montez vos images et vidéos, ajoutez une musique et du texte. Durée maximale : 10 minutes.", color = MaterialTheme.colorScheme.onBackground)
        Button(onClick = { picker.launch("video/*") }) { Text("Choisir une vidéo") }
        OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Composer à partir de photos") }
    }
}
