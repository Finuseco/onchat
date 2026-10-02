package com.example.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import com.example.ui.viewmodel.OnChatViewModel

@Composable
fun ProfileScreen(
    phone: String?,
    viewModel: OnChatViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val session by viewModel.currentSession.collectAsState()

    val isOwnProfile = remember(phone, session) {
        phone.isNullOrBlank() ||
                phone == session?.phone ||
                phone == session?.walletId
    }

    val targetPhone = phone ?: session?.phone ?: ""

    if (isOwnProfile) {
        UserProfileScreen(
            conversationId = null,
            contactName = null,
            contactPhone = session?.phone,
            contactWalletId = null,
            contactAvatarUrl = null,
            viewModel = viewModel,
            isSelfProfile = true,
            onBack = onBack
        )
    } else {
        PublicProfileScreen(
            phone = targetPhone,
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

fun downloadVideoToStorage(context: android.content.Context, videoUrl: String, creatorName: String) {
    if (videoUrl.isBlank()) return
    try {
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(videoUrl))
            .setTitle("Vidéo de $creatorName")
            .setDescription("Téléchargement de la vidéo OnChat...")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_MOVIES,
                "OnChat_${System.currentTimeMillis()}.mp4"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        dm.enqueue(request)
        android.widget.Toast.makeText(context, "Téléchargement démarré dans Galerie/Vidéos 📥", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Erreur téléchargement: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
