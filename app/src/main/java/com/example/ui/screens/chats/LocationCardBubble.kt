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
fun LocationCardBubble(message: Message, timeFormatted:String) {
 val context=LocalContext.current
 val lat=message.metadata?.get("latitude")?.toDoubleOrNull()
 val lng=message.metadata?.get("longitude")?.toDoubleOrNull()
 val valid=lat!=null&&lng!=null&&lat.isFinite()&&lng.isFinite()&&lat in -90.0..90.0&&lng in -180.0..180.0
 Card(modifier=Modifier.widthIn(min=220.dp,max=300.dp),colors=CardDefaults.cardColors(containerColor=CashPayNavySurface)){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Icon(Icons.Default.LocationOn,null,tint=OnChatCyan)
   Text(message.metadata?.get("title") ?: "Position partagée",color=Color.White,fontWeight=FontWeight.Bold)
   Text(if(valid) "$lat, $lng" else "Coordonnées indisponibles pour ce message",color=CashPayGrey)
   if(valid)TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps?q=$lat,$lng")))}.onFailure{Toast.makeText(context,"Aucune application de cartes disponible",Toast.LENGTH_LONG).show()}}){Text("Ouvrir la position")}
   Text(timeFormatted,color=CashPayGrey,fontSize=11.sp)
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
