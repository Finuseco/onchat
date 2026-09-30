package com.example.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class EmojiPickerTab {
    EMOJIS,
    STICKERS,
    GIFS
}

data class StickerItem(
    val id: String,
    val title: String,
    val emojiRepresentation: String,
    val caption: String
)

@Composable
fun EmojiStickerPicker(
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (StickerItem) -> Unit,
    onGifSelected: (String) -> Unit,
    onBackspace: () -> Unit
) {
    var activeTab by remember { mutableStateOf(EmojiPickerTab.EMOJIS) }
    var selectedEmojiCategory by remember { mutableStateOf(0) }

    val emojiCategories = remember {
        listOf(
            "😀" to listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹",
                "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗",
                "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓",
                "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕",
                "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😮‍💨",
                "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨"
            ),
            "👍" to listOf(
                "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌",
                "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
                "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜",
                "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳",
                "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🫀", "🫁"
            ),
            "❤️" to listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
                "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
                "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️"
            ),
            "💰" to listOf(
                "💰", "🪙", "💵", "💸", "💳", "🧾", "💎", "👑", "📈", "📉",
                "🏦", "🏧", "💼", "💹", "💲", "🤑", "🚀", "⚡", "🔥", "✨",
                "🎯", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🔒", "🔑", "🛡️"
            ),
            "🇨🇩" to listOf(
                "🇨🇩", "🇫🇷", "🇺🇸", "🇨🇦", "🇧🇪", "🇨🇭", "🇿🇦", "🇨🇲", "🇨🇬", "🇨🇮",
                "🇸🇳", "🇬🇦", "🇦🇴", "🇲🇺", "🇷🇼", "🇧🇮", "🇰🇪", "🇹🇿", "🇺🇬", "🇳🇬"
            )
        )
    }

    val stickers = remember {
        listOf(
            StickerItem("st_1", "CashPay Rocket", "🚀", "En route vers la fortune !"),
            StickerItem("st_2", "Money Bag", "💰", "Paiement validé illico !"),
            StickerItem("st_3", "Congo Simba", "🦁", "Force et Dignité !"),
            StickerItem("st_4", "Flash Pay", "⚡", "Transfert instantané !"),
            StickerItem("st_5", "Diamond Hands", "💎", "Solide comme un diamant !"),
            StickerItem("st_6", "Big Smile", "🥳", "C'est la fête OnChat !"),
            StickerItem("st_7", "Fire Deal", "🔥", "C'est chaud aujourd'hui !"),
            StickerItem("st_8", "Certified", "👑", "Top VIP CashPay"),
            StickerItem("st_9", "Thumbs Up", "👍", "Validé à 100%"),
            StickerItem("st_10", "Heart Love", "❤️", "Avec tout mon amour"),
            StickerItem("st_11", "Kinshasa Night", "🌃", "Kin la belle !"),
            StickerItem("st_12", "Success Deal", "🤝", "Affaire conclue !")
        )
    }

    val gifs = remember {
        listOf(
            "🎉 Félicitations & Bravo !" to "GIF_CONGRATS",
            "🚀 To the Moon CashPay !" to "GIF_ROCKET",
            "💸 Pluie de Billets / Cash Rain" to "GIF_CASH",
            "🕺 Danse de la Victoire" to "GIF_DANCE",
            "😎 Lunettes de Boss" to "GIF_COOL",
            "☕ Pause Café Kinshasa" to "GIF_COFFEE"
        )
    }

    Surface(
        color = CashPayNavySurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Section Tabs (Emojis, Stickers, GIFs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeTab == EmojiPickerTab.EMOJIS,
                        onClick = { activeTab = EmojiPickerTab.EMOJIS },
                        label = { Text("😄 Émojis", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OnChatViolet,
                            selectedLabelColor = Color.White,
                            containerColor = CashPayNavyDark,
                            labelColor = CashPayGrey
                        ),
                        border = null
                    )
                    FilterChip(
                        selected = activeTab == EmojiPickerTab.STICKERS,
                        onClick = { activeTab = EmojiPickerTab.STICKERS },
                        label = { Text("✨ Stickers", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OnChatViolet,
                            selectedLabelColor = Color.White,
                            containerColor = CashPayNavyDark,
                            labelColor = CashPayGrey
                        ),
                        border = null
                    )
                    FilterChip(
                        selected = activeTab == EmojiPickerTab.GIFS,
                        onClick = { activeTab = EmojiPickerTab.GIFS },
                        label = { Text("🎬 GIFs", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OnChatViolet,
                            selectedLabelColor = Color.White,
                            containerColor = CashPayNavyDark,
                            labelColor = CashPayGrey
                        ),
                        border = null
                    )
                }

                // Backspace button
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Effacer",
                        tint = CashPayGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(color = CashPayNavyBorder, thickness = 1.dp)

            when (activeTab) {
                EmojiPickerTab.EMOJIS -> {
                    // Category Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        emojiCategories.forEachIndexed { index, (icon, _) ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedEmojiCategory == index) OnChatViolet.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { selectedEmojiCategory = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, fontSize = 18.sp)
                            }
                        }
                    }

                    val currentEmojis = emojiCategories[selectedEmojiCategory].second

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 42.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(currentEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEmojiSelected(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                EmojiPickerTab.STICKERS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stickers) { sticker ->
                            Card(
                                onClick = { onStickerSelected(sticker) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                                modifier = Modifier.height(90.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(sticker.emojiRepresentation, fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sticker.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                EmojiPickerTab.GIFS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gifs) { (title, id) ->
                            Card(
                                onClick = { onGifSelected(title) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
                                modifier = Modifier.height(75.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(OnChatViolet.copy(alpha = 0.4f), OnChatBlue.copy(alpha = 0.4f))))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
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
