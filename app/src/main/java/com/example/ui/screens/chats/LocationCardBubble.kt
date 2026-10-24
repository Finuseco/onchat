package com.example.ui.screens.chats

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.ui.theme.*

@Composable
fun LocationCardBubble(
    message: Message,
    timeFormatted: String
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(OnChatCyan.copy(alpha = 0.6f), OnChatViolet.copy(alpha = 0.6f)))
        ),
        modifier = Modifier
            .widthIn(min = 250.dp, max = 290.dp)
            .clickable {
                try {
                    val gmmIntentUri = Uri.parse("geo:-4.3217,15.3126?q=Kinshasa")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    context.startActivity(mapIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Ouverture de la carte : Kinshasa", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Column {
            // Map Snapshot Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF1E2B45))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    drawRect(Color(0xFF19253C))

                    // Road paths
                    val roadColor = Color(0xFF2C3E63)
                    drawLine(roadColor, Offset(0f, h * 0.4f), Offset(w, h * 0.6f), strokeWidth = 10f)
                    drawLine(roadColor, Offset(w * 0.3f, 0f), Offset(w * 0.8f, h), strokeWidth = 8f)
                    drawLine(roadColor.copy(alpha = 0.5f), Offset(0f, h * 0.75f), Offset(w, h * 0.25f), strokeWidth = 5f)

                    // User pulse
                    drawCircle(OnChatCyan.copy(alpha = 0.2f), radius = 35f, center = Offset(w / 2f, h / 2f))
                }

                // Center Pin
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .offset(y = (-10).dp)
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "Google Maps",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Location Info & Actions
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content.substringBefore(" • ").ifBlank { "Position partagée" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.content.substringAfter(" • ").ifBlank { "Kinshasa, Gombe, RDC" },
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ouvrir l'itinéraire",
                        color = OnChatCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormatted,
                            color = CashPayGrey,
                            fontSize = 11.sp
                        )
                        if (message.isMine) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Délivré",
                                tint = OnChatCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerBubble(
    message: Message,
    timeFormatted: String
) {
    Column(
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CashPayNavySurface.copy(alpha = 0.5f))
                .border(1.dp, CashPayNavyBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                // Sticker large emoji/icon representation
                val emoji = when {
                    message.content.contains("🚀") -> "🚀"
                    message.content.contains("💰") -> "💰"
                    message.content.contains("🦁") -> "🦁"
                    message.content.contains("⚡") -> "⚡"
                    message.content.contains("💎") -> "💎"
                    message.content.contains("🔥") -> "🔥"
                    message.content.contains("👑") -> "👑"
                    message.content.contains("❤️") -> "❤️"
                    message.content.contains("🤝") -> "🤝"
                    message.content.contains("🥳") -> "🥳"
                    else -> "✨"
                }

                Text(text = emoji, fontSize = 56.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content.replace(emoji, "").trim(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(text = timeFormatted, color = CashPayGrey, fontSize = 10.sp)
            if (message.isMine) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
fun ContactCardBubble(
    message: Message,
    timeFormatted: String
) {
    val context = LocalContext.current

    val contactName = remember(message.content) {
        message.content.substringBefore(" (").ifBlank { "Contact OnChat" }
    }
    val contactPhone = remember(message.content) {
        message.content.substringAfter(" (").substringBefore(")").ifBlank { "" }
    }
    val walletId = remember(message.content) {
        if (message.content.contains("Wallet:")) {
            message.content.substringAfter("Wallet:").substringBefore(" • ").trim()
        } else ""
    }
    val avatarUrl = remember(message.content) {
        if (message.content.contains("Avatar:")) message.content.substringAfter("Avatar:").trim() else null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        modifier = Modifier.widthIn(min = 250.dp, max = 290.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(OnChatPrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(avatarUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contactName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = "Actif", tint = OnChatCyan, modifier = Modifier.size(16.dp))
                    }
                    if (contactPhone.isNotBlank()) {
                        Text(
                            text = contactPhone,
                            color = CashPayGrey,
                            fontSize = 13.sp
                        )
                    }
                    if (walletId.isNotBlank()) {
                        Text(
                            text = "Wallet: $walletId",
                            color = OnChatCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = CashPayNavyBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, contactName)
                            if (contactPhone.isNotBlank()) {
                                putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, contactPhone)
                            }
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Numéro : $contactPhone", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enregistrer dans le téléphone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(timeFormatted, color = CashPayGrey, fontSize = 10.sp)
                if (message.isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
fun PollCardBubble(
    message: Message,
    timeFormatted: String
) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    val options = remember(message.content) {
        if (message.content.contains("|")) {
            message.content.substringAfter("|").split(";").filter { it.isNotBlank() }
        } else {
            listOf("Gombe (Siège OnChat)", "En ligne / Visioconférence", "Grand Hôtel Kinshasa")
        }
    }
    val question = remember(message.content) {
        message.content.substringBefore("|").trim()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
        modifier = Modifier.widthIn(min = 260.dp, max = 300.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Poll, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sondage de groupe", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(question, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            options.forEachIndexed { index, option ->
                val isSelected = selectedOption == index
                val votePercent = when (index) {
                    0 -> 65
                    1 -> 25
                    else -> 10
                }

                Surface(
                    onClick = { selectedOption = index },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) OnChatViolet.copy(alpha = 0.25f) else CashPayNavyDark,
                    border = BorderStroke(1.dp, if (isSelected) OnChatCyan else CashPayNavyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option, color = Color.White, fontSize = 13.sp)
                        if (selectedOption != null) {
                            Text("$votePercent%", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedOption != null) "Vote enregistré" else "Touchez pour voter",
                    color = CashPayGrey,
                    fontSize = 11.sp
                )
                Text(timeFormatted, color = CashPayGrey, fontSize = 10.sp)
            }
        }
    }
}
