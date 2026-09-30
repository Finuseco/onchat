package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- OnChat Official Brand Palette (Violet → Bleu → Cyan) ---
val OnChatVioletDeep = Color(0xFF562EBF)    // 86, 46, 191
val OnChatViolet = Color(0xFF7F46D3)        // 127, 70, 211
val OnChatVioletLight = Color(0xFFB660ED)   // 182, 96, 237
val OnChatBlue = Color(0xFF1F62D5)          // 31, 98, 213
val OnChatBluePrimary = Color(0xFF1D8AE4)   // 29, 138, 228
val OnChatCyan = Color(0xFF44BBF5)          // 68, 187, 245
val OnChatMagenta = Color(0xFFE91E63)
val OnChatNavyDark = Color(0xFF070A16)
val OnChatDarkBg = Color(0xFF121212)
val OnChatSurfaceDark = Color(0xFF1E1E1E)
val OnChatAccentGold = Color(0xFFFFD700)

// --- Supporting Surfaces & Canvas ---
val CashPayNavy = Color(0xFF0D1226)         // Modern Dark Canvas
val CashPayNavyDark = Color(0xFF070A16)     // Deepest background
val CashPayMidnightBlue = Color(0xFF0D1226)
val CashPayNavySurface = Color(0xFF141C38)  // Elevated Card / Surface
val CashPayNavyBorder = Color(0xFF233060)   // Subtle border with blue/violet tint

// --- Functional Accents ---
val SuccessGreen = Color(0xFF25D366)        // Online / Success
val ActionWhite = Color(0xFFFFFFFF)
val CashPayGreen = Color(0xFF25D366)
val CashPayGreenLight = Color(0xFF4ADE80)
val CashPayGold = Color(0xFFF59E0B)
val CashPayBlue = OnChatBluePrimary
val CashPayGrey = Color(0xFF94A3B8)
val CashPayLightGrey = Color(0xFFF1F5F9)

// Chat Bubbles
val CashPayReceivedBubble = Color(0xFF162044)
val CashPayReceivedBubbleLight = Color(0xFFE2E8F0)
val CashPaySentBubble = Color(0xFF1C357A)
val CashPaySentBubbleLight = Color(0xFFDBEAFE)

// Aliases mapped to brand gradient colors
val CashPayOrange = OnChatViolet
val CashPayOrangeLight = OnChatCyan
val CashPayOrangeDark = OnChatVioletDeep

// --- Official Brand Gradients (Violet → Bleu → Cyan) ---
val OnChatGradientColors = listOf(
    OnChatVioletDeep,
    OnChatViolet,
    OnChatVioletLight,
    OnChatBlue,
    OnChatBluePrimary,
    OnChatCyan
)

val OnChatPrimaryGradient = Brush.horizontalGradient(
    colors = listOf(OnChatVioletDeep, OnChatViolet, OnChatBluePrimary, OnChatCyan)
)

val OnChatDiagonalGradient = Brush.linearGradient(
    colors = listOf(OnChatVioletDeep, OnChatViolet, OnChatBluePrimary, OnChatCyan)
)

val OnChatAccentGradient = Brush.horizontalGradient(
    colors = listOf(OnChatViolet, OnChatBluePrimary, OnChatCyan)
)

val OnChatBubbleGradient = Brush.horizontalGradient(
    colors = listOf(OnChatVioletDeep.copy(alpha = 0.95f), OnChatBlue.copy(alpha = 0.95f))
)

// Main button gradient now directly uses the official OnChat Violet → Bleu → Cyan gradient
val OrangeButtonGradient = OnChatPrimaryGradient

val WhiteButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color.White, Color(0xFFF8FAFC))
)

val MidnightBlueGradient = Brush.verticalGradient(
    colors = listOf(CashPayNavy, CashPayNavyDark)
)

val ShimmerSkeletonColors = listOf(
    Color(0xFF162044),
    Color(0xFF253770),
    Color(0xFF162044)
)

val NavyAppHeaderGradient = Brush.verticalGradient(
    colors = listOf(CashPayNavy, CashPayNavyDark)
)
