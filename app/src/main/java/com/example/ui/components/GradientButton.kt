package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OnChatPrimaryGradient

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    gradient: Brush = OnChatPrimaryGradient, // Official OnChat Violet → Bleu → Cyan
    cornerRadius: Dp = 16.dp,
    height: Dp = 56.dp, // Increased default height for superior touch target & prominence
    contentColor: Color = Color.White
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth physics-based spring animations for a hyper-responsive premium physical feel
    val scale by animateFloatAsState(
        targetValue = if (!enabled || isLoading) 1f else if (isPressed) 0.95f else if (isHovered) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (!enabled || isLoading) 0.dp else if (isPressed) 2.dp else if (isHovered) 14.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "button_elevation"
    )

    // Dynamic border to add depth and make the button stand out in modern dark interfaces
    val borderStroke = if (!enabled || isLoading) {
        BorderStroke(1.dp, Color.Transparent)
    } else if (isHovered || isFocused) {
        BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color.White, Color.White.copy(alpha = 0.5f))))
    } else if (isPressed) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    }

    // Dynamic glass overlay to simulate reflection/light capture on hover
    val overlayColor = when {
        isPressed -> Color.Black.copy(alpha = 0.08f)
        isHovered -> Color.White.copy(alpha = 0.12f)
        isFocused -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(shape)
            .background(
                if (enabled && !isLoading) gradient
                else Brush.horizontalGradient(listOf(Color(0xFF1D2438), Color(0xFF141A29)))
            )
            .border(borderStroke, shape)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.25f))
            ) {
                onClick()
            }
            .testTag("login_submit_button"),
        contentAlignment = Alignment.Center
    ) {
        // Overlay for hover/press light state reflections
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(26.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    color = if (enabled) contentColor else Color.White.copy(alpha = 0.35f),
                    fontSize = 17.sp, // Sized up text for readability
                    fontWeight = FontWeight.ExtraBold, // Bolder presentation for high visual impact
                    letterSpacing = 0.75.sp // Elegant tracking
                )
            }
        }
    }
}
