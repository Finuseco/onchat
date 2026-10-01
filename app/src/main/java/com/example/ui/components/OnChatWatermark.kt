package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OnChatCyan
import com.example.ui.theme.OnChatVioletLight

/**
 * Official OnChat Signature Watermark matching brand guidelines:
 * "On" in Vibrant Purple (#B660ED / #C084FC)
 * "Chat" in Vibrant Cyan (#38BDF8 / #44BBF5)
 *
 * Placed cleanly on top-left / opposite side of the screen,
 * never overlapping avatars or interaction action buttons.
 */
@Composable
fun OnChatSignatureWatermark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    withBackground: Boolean = true,
    animatedGlow: Boolean = false
) {
    Box(
        modifier = modifier
            .then(
                if (withBackground) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "On",
                color = OnChatVioletLight,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Chat",
                color = OnChatCyan,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
