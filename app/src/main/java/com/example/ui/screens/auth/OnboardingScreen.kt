package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val drawableResId: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val steps = listOf(
        OnboardingStep(
            title = "Bienvenue sur OnChat & CashPay",
            subtitle = "Messagerie & Écosystème Financier Mondial",
            description = "Communiquez en temps réel avec vos proches et gérez vos finances en toute sécurité dans une application unique.",
            drawableResId = R.drawable.onboarding_transfer_1790523177522,
            icon = Icons.Default.Chat
        ),
        OnboardingStep(
            title = "Transferts d'Argent Instantanés",
            subtitle = "Envoyez des fonds en un clic",
            description = "Effectuez des transferts d'argent rapides et sécurisés à travers le monde avec suivi en direct et multi-devises.",
            drawableResId = R.drawable.onboarding_transfer_1790523177522,
            icon = Icons.Default.Send
        ),
        OnboardingStep(
            title = "Appels HD & Vidéoconférences",
            subtitle = "Restez connectés en face à face",
            description = "Profitez d'appels audio et vidéo haute définition et organisez des réunions multi-écrans fluides avec vos équipes et amis.",
            drawableResId = R.drawable.onboarding_video_1790523189152,
            icon = Icons.Default.Videocam
        ),
        OnboardingStep(
            title = "Sécurité Maximale & Prêt",
            subtitle = "Chiffrement et protection bancaire",
            description = "Vos données et transactions sont protégées par un chiffrement de pointe et une authentification sécurisée.",
            drawableResId = R.drawable.onboarding_security_1790523200832,
            icon = Icons.Default.Security
        )
    )

    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep = steps[currentStepIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CashPayNavyDark,
                        Color(0xFF131728),
                        Color(0xFF1A153B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = OnChatVioletLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Étape ${currentStepIndex + 1}/${steps.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text(
                        text = "Passer",
                        color = OnChatVioletLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Illustration Card / Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2238)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = currentStep.drawableResId),
                        contentDescription = currentStep.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay at bottom of image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF0F1223).copy(alpha = 0.85f)
                                    ),
                                    startY = 300f
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content Text Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OnChatVioletLight.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = currentStep.subtitle,
                        color = OnChatVioletLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = currentStep.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentStep.description,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Indicators & Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = index == currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) OnChatVioletLight
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Next / Finish Button
                Button(
                    onClick = {
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnChatVioletLight
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentStepIndex < steps.size - 1) "Suivant" else "Commencer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = if (currentStepIndex < steps.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
