package com.example.ui.screens.chats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.ai.JojoAiEngine
import com.example.data.ai.JojoAiExpert
import com.example.data.ai.JojoAiMessage
import com.example.data.ai.JojoAiSender
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JojoAiChatScreen(
    onBack: () -> Unit,
    initialPrompt: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedExpert by remember { mutableStateOf(JojoAiExpert.GENERAL) }
    var inputText by remember { mutableStateOf(initialPrompt ?: "") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var isCorrectingVoice by remember { mutableStateOf(false) }
    var latestAiMessageId by remember { mutableStateOf<String?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    var messages by remember {
        mutableStateOf(
            listOf(
                JojoAiMessage(
                    sender = JojoAiSender.JOJO_AI,
                    text = "Bonjour ! Je suis **Jojo AI**, votre assistant virtuel intelligent pour **OnChat & CashPay All**.\n\n" +
                            "Je peux répondre à toutes vos questions, vous guider pour vos transferts, corriger vos messages vocaux, générer des images ou traduire vos textes.\n\n" +
                            "Choisissez un mode expert ci-dessous ou posez-moi directement votre question !",
                    expertMode = JojoAiExpert.GENERAL
                )
            )
        )
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Jojo AI", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Texte copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
    }

    fun sendMessage(promptText: String) {
        if (promptText.isBlank() || isThinking) return
        val wantsImage = selectedExpert == JojoAiExpert.IMAGE_GEN ||
                promptText.lowercase().contains("dessine") ||
                promptText.lowercase().contains("image") ||
                promptText.lowercase().contains("photo")
        val userMsg = JojoAiMessage(
            sender = JojoAiSender.USER,
            text = promptText.trim(),
            expertMode = selectedExpert
        )
        val updatedList = messages + userMsg
        messages = updatedList
        inputText = ""
        isThinking = true
        isGeneratingImage = wantsImage

        scope.launch {
            listState.animateScrollToItem(updatedList.size - 1)
            val aiResponse = JojoAiEngine.processUserPrompt(
                prompt = userMsg.text,
                expert = selectedExpert,
                history = updatedList
            )
            latestAiMessageId = aiResponse.id
            messages = messages + aiResponse
            isThinking = false
            isGeneratingImage = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashPayNavyDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar
            Surface(
                color = CashPayNavy,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(brush = OnChatPrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Jojo AI Assistant", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366))
                            )
                        }
                        Text(
                            text = "${selectedExpert.iconEmoji} ${selectedExpert.displayName} • En ligne",
                            color = OnChatCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = {
                        messages = listOf(
                            JojoAiMessage(
                                sender = JojoAiSender.JOJO_AI,
                                text = "Discussion réinitialisée ! Comment puis-je vous aider ?",
                                expertMode = selectedExpert
                            )
                        )
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Effacer", tint = CashPayGrey)
                    }
                }
            }

            // Expert Mode Selector Pills Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CashPayNavySurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(JojoAiExpert.values()) { expert ->
                    val isSelected = selectedExpert == expert
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected) Modifier.background(brush = OnChatPrimaryGradient)
                                else Modifier.background(CashPayNavyDark)
                            )
                            .clickable { selectedExpert = expert }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(expert.iconEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = expert.displayName,
                                color = if (isSelected) Color.White else CashPayGrey,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(messages) { index, msg ->
                    if (msg.sender == JojoAiSender.USER) {
                        // User Bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 16.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(CashPayNavySurface)
                                    .border(1.dp, OnChatCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    } else {
                        // Jojo AI Bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(brush = OnChatPrimaryGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.widthIn(max = 300.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 4.dp,
                                                topEnd = 16.dp,
                                                bottomStart = 16.dp,
                                                bottomEnd = 16.dp
                                            )
                                        )
                                        .background(CashPayNavy)
                                        .border(1.dp, OnChatViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                    ) {
                                    Column {
                                        // Mode Indicator Tag
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(msg.expertMode.iconEmoji, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = msg.expertMode.displayName,
                                                color = OnChatVioletLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Image if present with View, Save, Share actions
                                        if (msg.isImage && !msg.imageUrl.isNullOrBlank()) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                                                border = BorderStroke(1.dp, CashPayNavyBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp)
                                            ) {
                                                Column {
                                                    AsyncImage(
                                                        model = msg.imageUrl,
                                                        contentDescription = "Image Jojo AI",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                                            .clickable { previewImageUrl = msg.imageUrl }
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        TextButton(
                                                            onClick = { previewImageUrl = msg.imageUrl },
                                                            colors = ButtonDefaults.textButtonColors(contentColor = OnChatCyan)
                                                        ) {
                                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text("Voir", fontSize = 11.sp)
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                copyToClipboard(msg.imageUrl ?: "")
                                                                Toast.makeText(context, "Lien de l'image copié pour enregistrement !", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                                        ) {
                                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text("Sauvegarder", fontSize = 11.sp)
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                    type = "text/plain"
                                                                    putExtra(Intent.EXTRA_SUBJECT, "Visuel créé par Jojo AI")
                                                                    putExtra(Intent.EXTRA_TEXT, "Voici un visuel créé par Jojo AI OnChat :\n${msg.imageUrl}\n\nPrompt : ${msg.text}")
                                                                }
                                                                context.startActivity(Intent.createChooser(shareIntent, "Partager le visuel"))
                                                            },
                                                            colors = ButtonDefaults.textButtonColors(contentColor = OnChatVioletLight)
                                                        ) {
                                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text("Partager", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // AI Text Content with Typewriter effect on latest message
                                        val isThisMessageTyping = (msg.id == latestAiMessageId)
                                        JojoTypewriterText(
                                            fullText = msg.text,
                                            isTypingActive = isThisMessageTyping,
                                            onTypingFinished = {
                                                if (latestAiMessageId == msg.id) {
                                                    latestAiMessageId = null
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Press-Copy Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = CashPayNavySurface,
                                                modifier = Modifier.clickable { copyToClipboard(msg.text) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copier",
                                                        tint = OnChatCyan,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Copier",
                                                        color = OnChatCyan,
                                                        fontSize = 11.sp,
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

                if (isThinking) {
                    item {
                        JojoThinkingIndicator(isImageGen = isGeneratingImage)
                    }
                }
            }

            // Quick Suggestions Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = when (selectedExpert) {
                    JojoAiExpert.FINANCE -> listOf(
                        "💡 Comment transférer sur Wallet ID ?",
                        "📊 Taux USD/CDF du jour",
                        "🔒 Activer le PIN à 4 chiffres"
                    )
                    JojoAiExpert.VOICE_CORRECTOR -> listOf(
                        "🎙️ Reformule en français soigné",
                        "✍️ Corriger les fautes d'orthographe",
                        "💬 Version courtoise pour un transfert"
                    )
                    JojoAiExpert.IMAGE_GEN -> listOf(
                        "🎨 Dessine un logo CashPay futuriste",
                        "🖼️ Illustration 3D carte bancaire",
                        "✨ Visuel futuriste OnChat"
                    )
                    JojoAiExpert.TRANSLATOR -> listOf(
                        "🌍 Traduire 'Bonjour comment vas-tu' en Lingala",
                        "💬 Traduction Anglais ➔ Français",
                        "🗣️ Traduire en Swahili"
                    )
                    JojoAiExpert.GENERAL -> listOf(
                        "💡 C'est quoi CashPay All ?",
                        "📱 Comment inviter un contact ?",
                        "🌟 Rôle de Jojo AI"
                    )
                }

                items(suggestions) { sugg ->
                    SuggestionChip(
                        onClick = { sendMessage(sugg) },
                        label = { Text(sugg, fontSize = 11.sp, color = Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CashPayNavySurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OnChatCyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Voice Recording Bar Banner (if active)
            AnimatedVisibility(visible = isRecordingVoice) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CashPayNavySurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Dictée vocale active • Parlez clairement",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCorrectingVoice) {
                                Text(
                                    text = "Correction syntaxique Jojo AI en cours...",
                                    color = OnChatCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputText.isNotBlank()) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isCorrectingVoice = true
                                        val corrected = JojoAiEngine.correctTranscribedVoice(inputText)
                                        isCorrectingVoice = false
                                        if (corrected.isNotBlank()) {
                                            inputText = corrected
                                            Toast.makeText(context, "Texte vocal corrigé par Jojo AI !", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isCorrectingVoice,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isCorrectingVoice) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Corriger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        TextButton(onClick = {
                            isRecordingVoice = false
                            if (inputText.isBlank()) {
                                inputText = "Bonjour Jojo AI, peux-tu corriger mon message vocal ?"
                            }
                        }) {
                            Text("Terminer", color = OnChatCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = CashPayNavy,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isRecordingVoice = !isRecordingVoice
                        if (isRecordingVoice) {
                            selectedExpert = JojoAiExpert.VOICE_CORRECTOR
                        }
                    }) {
                        Icon(
                            imageVector = if (isRecordingVoice) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Dictée vocale",
                            tint = if (isRecordingVoice) Color.Red else OnChatCyan
                        )
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = when (selectedExpert) {
                                    JojoAiExpert.VOICE_CORRECTOR -> "Dictez ou collez votre texte à corriger..."
                                    JojoAiExpert.IMAGE_GEN -> "Décrivez l'image à générer..."
                                    JojoAiExpert.FINANCE -> "Question sur CashPay ou votre Wallet ID..."
                                    else -> "Posez votre question à Jojo AI..."
                                },
                                color = CashPayGrey,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CashPayNavySurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CashPayNavySurface,
                            unfocusedContainerColor = CashPayNavySurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OnChatCyan,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isThinking) OnChatViolet else CashPayNavySurface
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Envoyer",
                            tint = if (inputText.isNotBlank() && !isThinking) Color.White else CashPayGrey
                        )
                    }
                }
            }
        }

        // Full Screen Image Preview Modal
        if (previewImageUrl != null) {
            Dialog(onDismissRequest = { previewImageUrl = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                    border = BorderStroke(1.dp, CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aperçu visuel Jojo AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            IconButton(onClick = { previewImageUrl = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AsyncImage(
                            model = previewImageUrl,
                            contentDescription = "Plein écran",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CashPayNavy)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                copyToClipboard(previewImageUrl ?: "")
                                Toast.makeText(context, "Lien copié pour téléchargement !", Toast.LENGTH_SHORT).show()
                                previewImageUrl = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = CashPayNavyDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Télécharger / Enregistrer", color = CashPayNavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 4 small blinking/pulsing buttons in dark/white theme for Jojo AI thinking state
@Composable
fun JojoThinkingIndicator(isImageGen: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_anim")

    val dotScales = (0..3).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550, delayMillis = index * 140, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$index"
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CashPayNavy,
        border = BorderStroke(1.dp, if (isImageGen) OnChatCyan.copy(alpha = 0.5f) else OnChatViolet.copy(alpha = 0.4f)),
        modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isImageGen) "Jojo AI construit le visuel" else "Jojo AI réfléchit",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))
                // 4 small blinking/pulsing buttons in dark/white theme
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dotScales.forEach { anim ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .graphicsLayer {
                                    scaleX = anim.value
                                    scaleY = anim.value
                                    alpha = anim.value.coerceIn(0.35f, 1f)
                                }
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
            if (isImageGen) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(180.dp)
                        .height(3.dp),
                    color = OnChatCyan,
                    trackColor = CashPayNavySurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Génération neuronale haute résolution...",
                    color = CashPayGrey,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Progressive Typewriter animation for Jojo AI message streaming
@Composable
fun JojoTypewriterText(
    fullText: String,
    isTypingActive: Boolean,
    onTypingFinished: () -> Unit = {}
) {
    var displayedChars by remember(fullText, isTypingActive) {
        mutableStateOf(if (isTypingActive) 0 else fullText.length)
    }

    LaunchedEffect(fullText, isTypingActive) {
        if (isTypingActive && displayedChars < fullText.length) {
            val step = if (fullText.length > 250) 8 else 3
            while (displayedChars < fullText.length) {
                displayedChars = (displayedChars + step).coerceAtMost(fullText.length)
                delay(18)
            }
            onTypingFinished()
        }
    }

    val visibleText = if (isTypingActive) fullText.take(displayedChars) else fullText

    Text(
        text = visibleText + (if (isTypingActive && displayedChars < fullText.length) " ▌" else ""),
        color = Color.White,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.clickable {
            // Tap to finish typing immediately
            if (isTypingActive && displayedChars < fullText.length) {
                displayedChars = fullText.length
                onTypingFinished()
            }
        }
    )
}
