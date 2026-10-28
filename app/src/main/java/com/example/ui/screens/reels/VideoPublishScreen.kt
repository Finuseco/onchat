package com.example.ui.screens.reels

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPublishScreen(
    viewModel: OnChatViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { videoUri = it }
    var caption by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("Public") }
    var allowComments by remember { mutableStateOf(true) }
    var allowDuet by remember { mutableStateOf(true) }
    var isPublishing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CashPayNavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Publier sur OnChat Shorts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                OutlinedButton(onClick = { picker.launch("video/*") }, enabled = !isPublishing) {
                    Text(if (videoUri == null) "Choisir une vidéo" else "Changer de vidéo")
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Texte sur la vidéo", color = CashPayGrey) },
                    placeholder = { Text("Texte incrusté dans la vidéo...", color = CashPayGrey.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        unfocusedBorderColor = CashPayNavyBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hashtags suggestions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("#OnChat", "#CashPay", "#Shorts", "#Musique").forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    caption = if (caption.isBlank()) tag else "$caption $tag"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = tag, color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Options list
                Card(
                    colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = OnChatCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Confidentialité", color = Color.White, fontSize = 14.sp)
                            }
                            Text(privacy, color = CashPayGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Comment, contentDescription = null, tint = OnChatVioletLight)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Commentaires (bientôt disponible)", color = Color.White, fontSize = 14.sp)
                            }
                            Switch(
                                checked = allowComments,
                                onCheckedChange = { allowComments = it },
                                enabled = false,
                                colors = SwitchDefaults.colors(checkedThumbColor = OnChatCyan)
                            )
                        }

                        HorizontalDivider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = CashPayGold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Réutilisation du son (bientôt disponible)", color = Color.White, fontSize = 14.sp)
                            }
                            Switch(
                                checked = allowDuet,
                                onCheckedChange = { allowDuet = it },
                                enabled = false,
                                colors = SwitchDefaults.colors(checkedThumbColor = OnChatCyan)
                            )
                        }
                    }
                }
            }

            // Publish Button
            Button(
                onClick = {
                    videoUri?.let { uri ->
                        isPublishing = true
                        viewModel.uploadVideo(uri, caption) { success, error ->
                            isPublishing = false
                            if (success) {
                                viewModel.fetchVideosFeed()
                                Toast.makeText(context, "Vidéo publiée", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, error ?: "Échec de la publication", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isPublishing && videoUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OnChatMagenta)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier maintenant", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
