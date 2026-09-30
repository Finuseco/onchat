package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CashPayGreen
import com.example.ui.theme.CashPayNavy
import com.example.ui.theme.CashPayOrange

@Composable
fun AvatarBadge(
    name: String,
    avatarUrl: String? = null,
    size: Dp = 50.dp,
    isGroup: Boolean = false,
    hasStatus: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    val colors = listOf(
        Pair(Color(0xFF1E3A8A), Color(0xFF3B82F6)),
        Pair(Color(0xFF9A3412), Color(0xFFEA580C)),
        Pair(Color(0xFF166534), Color(0xFF22C55E)),
        Pair(Color(0xFF6B21A8), Color(0xFFA855F7)),
        Pair(Color(0xFF831843), Color(0xFFEC4899))
    )
    val colorIndex = Math.abs(name.hashCode()) % colors.size
    val gradient = Brush.linearGradient(listOf(colors[colorIndex].first, colors[colorIndex].second))

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // WhatsApp / OnChat style status ring around avatar
        val ringModifier = if (hasStatus) {
            Modifier
                .size(size)
                .border(2.5.dp, brush = com.example.ui.theme.OnChatPrimaryGradient, shape = CircleShape)
                .padding(3.dp)
        } else {
            Modifier.size(size)
        }

        Box(
            modifier = ringModifier
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            val fullAvatarUrl = remember(avatarUrl) {
                if (avatarUrl.isNullOrBlank()) null
                else if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) avatarUrl
                else if (avatarUrl.startsWith("/")) "https://app.cashpay-all.com$avatarUrl"
                else "https://app.cashpay-all.com/$avatarUrl"
            }

            if (!fullAvatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = fullAvatarUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.onchat_logo),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Online indicator dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .border(2.dp, CashPayNavy, CircleShape)
                    .clip(CircleShape)
                    .background(CashPayGreen)
            )
        }
    }
}
