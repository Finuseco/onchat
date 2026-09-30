package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CashPayNavyBorder
import com.example.ui.theme.CashPayNavySurface

/**
 * Animated Shimmer Skeleton Modifier that makes elements pulse smoothly
 * like in high-performance modern apps (Flutter/React Native style).
 */
fun Modifier.shimmerSkeleton(
    isLoading: Boolean = true,
    cornerRadius: Dp = 8.dp
): Modifier = composed {
    if (!isLoading) return@composed this

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerColors = listOf(
        CashPayNavySurface.copy(alpha = 0.6f),
        CashPayNavyBorder.copy(alpha = 0.9f),
        CashPayNavySurface.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp
) {
    Box(
        modifier = modifier.shimmerSkeleton(cornerRadius = cornerRadius)
    )
}

@Composable
fun SkeletonCircle(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .shimmerSkeleton(cornerRadius = size / 2)
    )
}

/**
 * Skeleton placeholder row for WhatsApp-like Chat items
 */
@Composable
fun SkeletonChatRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 54.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(16.dp)
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(44.dp)
                        .height(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(13.dp)
            )
        }
    }
}

/**
 * Skeleton list for Chat conversations
 */
@Composable
fun SkeletonChatList(count: Int = 7) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(count) {
            SkeletonChatRow()
        }
    }
}

/**
 * Skeleton placeholder row for CashPay Groups with currency badges
 */
@Composable
fun SkeletonGroupRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 52.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                )
                // Currency tag skeleton
                SkeletonBox(
                    modifier = Modifier
                        .width(46.dp)
                        .height(20.dp),
                    cornerRadius = 10.dp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(12.dp)
            )
        }
    }
}

/**
 * Skeleton placeholder for WhatsApp-style Status / Stories preview bar
 */
@Composable
fun SkeletonStatusStoryBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                SkeletonCircle(size = 60.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(10.dp)
                )
            }
        }
    }
}

/**
 * Skeleton row for Status updates
 */
@Composable
fun SkeletonStatusRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 54.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
            )
        }
    }
}

/**
 * Skeleton bubble for Chat screen
 */
@Composable
fun SkeletonMessageBubble(
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 16.dp
                    )
                )
                .background(CashPayNavySurface)
                .padding(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(if (isOutgoing) 0.9f else 0.8f)
                    .height(14.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .width(40.dp)
                    .height(10.dp)
                    .align(Alignment.End)
            )
        }
    }
}

/**
 * Skeleton row for Call history
 */
@Composable
fun SkeletonCallRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 50.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
            )
        }
        SkeletonCircle(size = 36.dp)
    }
}
