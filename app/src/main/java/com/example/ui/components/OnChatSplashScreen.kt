package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class FloatingHeroIcon(
    val icon: ImageVector,
    val color: Color,
    val initialOffsetX: Float,
    val initialOffsetY: Float,
    val targetOffsetX: Float,
    val targetOffsetY: Float,
    val delayMs: Int
)

@Composable
fun OnChatSplashScreen(
    onAnimationFinished: () -> Unit
) {
    // 1. Staggered letter animation progress
    val letters = listOf(
        Pair('O', OnChatVioletLight),
        Pair('n', OnChatVioletLight),
        Pair('C', OnChatCyan),
        Pair('h', OnChatCyan),
        Pair('a', OnChatCyan),
        Pair('t', OnChatCyan)
    )

    // Animation state triggers
    var startAnimation by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_star")
    val starGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Cosmic floating icons data
    val floatingIcons = remember {
        listOf(
            FloatingHeroIcon(Icons.Default.ChatBubble, OnChatCyan, -180f, -240f, -90f, -110f, 150),
            FloatingHeroIcon(Icons.Default.Videocam, OnChatVioletLight, 180f, -260f, 95f, -115f, 300),
            FloatingHeroIcon(Icons.Default.Call, Color(0xFF10B981), -190f, 140f, -100f, 75f, 450),
            FloatingHeroIcon(Icons.Default.Payments, Color(0xFFF59E0B), 190f, 150f, 105f, 80f, 600)
        )
    }

    LaunchedEffect(Unit) {
        startAnimation = true
        // Allow full visual presentation, then finish
        delay(2600)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CashPayNavySurface,
                        CashPayNavyDark,
                        Color(0xFF060914)
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background starry particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val starColors = listOf(
                Color(0x5544BBF5),
                Color(0x44B660ED),
                Color(0x33FFFFFF),
                Color(0x66F59E0B)
            )

            for (i in 0..30) {
                val seed = i * 1337
                val x = (sin(seed.toDouble()) * 0.5 + 0.5).toFloat() * w
                val y = (cos(seed.toDouble() * 1.5) * 0.5 + 0.5).toFloat() * h
                val r = (i % 3 + 1.5).toFloat()
                drawCircle(
                    color = starColors[i % starColors.size],
                    radius = r,
                    center = Offset(x, y)
                )
            }
        }

        // Radiant starburst beam / backlight behind the central logo
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(starGlowScale)
                .rotate(starRotation)
                .alpha(0.6f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OnChatCyan.copy(alpha = 0.55f),
                            OnChatVioletLight.copy(alpha = 0.35f),
                            Color(0x00B660ED)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Floating descending icons coming from the cosmos
        floatingIcons.forEach { item ->
            val animOffsetX by animateFloatAsState(
                targetValue = if (startAnimation) item.targetOffsetX else item.initialOffsetX,
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = item.delayMs,
                    easing = FastOutSlowInEasing
                ),
                label = "icon_x"
            )
            val animOffsetY by animateFloatAsState(
                targetValue = if (startAnimation) item.targetOffsetY else item.initialOffsetY,
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = item.delayMs,
                    easing = FastOutSlowInEasing
                ),
                label = "icon_y"
            )
            val animAlpha by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = tween(durationMillis = 700, delayMillis = item.delayMs),
                label = "icon_alpha"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = animOffsetX * density
                        translationY = animOffsetY * density
                        alpha = animAlpha
                    }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CashPayNavySurface.copy(alpha = 0.85f))
                    .border(1.2.dp, item.color.copy(alpha = 0.6f), CircleShape)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Central Composition: Logo + Animated Letter-by-Letter "OnChat"
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Central Logo with pulsing container
            val logoScale by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0.4f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "logo_scale"
            )

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .scale(logoScale)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.5.dp, OnChatCyan.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onchat_logo),
                    contentDescription = "Logo OnChat",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Letter by letter animated typography: "O", "n", "C", "h", "a", "t"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                letters.forEachIndexed { index, (char, color) ->
                    val letterDelay = 200 + (index * 90)
                    val letterOffset by animateFloatAsState(
                        targetValue = if (startAnimation) 0f else if (index < 2) -60f else 60f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "char_offset_$index"
                    )
                    val letterAlpha by animateFloatAsState(
                        targetValue = if (startAnimation) 1f else 0f,
                        animationSpec = tween(durationMillis = 400, delayMillis = letterDelay),
                        label = "char_alpha_$index"
                    )
                    val letterScale by animateFloatAsState(
                        targetValue = if (startAnimation) 1f else 0.2f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "char_scale_$index"
                    )

                    Text(
                        text = char.toString(),
                        color = color,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = letterOffset * density
                                alpha = letterAlpha
                                scaleX = letterScale
                                scaleY = letterScale
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle pill
            val subAlpha by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = tween(durationMillis = 600, delayMillis = 850),
                label = "sub_alpha"
            )

            Box(
                modifier = Modifier
                    .alpha(subAlpha)
                    .clip(RoundedCornerShape(50.dp))
                    .background(brush = OnChatPrimaryGradient)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Écosystème CashPay • Messagerie & Transferts",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Footer: "Integrated with Finisco SARL"
        val footerAlpha by animateFloatAsState(
            targetValue = if (startAnimation) 0.85f else 0f,
            animationSpec = tween(durationMillis = 600, delayMillis = 1000),
            label = "footer_alpha"
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .alpha(footerAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Integrated with Finisco SARL",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
        }
    }
}
