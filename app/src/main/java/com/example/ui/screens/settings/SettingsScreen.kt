package com.example.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AvatarBadge
import com.example.ui.components.ForgotPinSheet
import com.example.ui.components.GradientButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForgotPinFlowStep
import com.example.ui.viewmodel.OnChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OnChatViewModel,
    onBack: () -> Unit,
    onOpenProfile: (() -> Unit)? = null,
    onOpenTransactions: (() -> Unit)? = null
) {
    val session by viewModel.currentSession.collectAsState()
    val forgotPinStep by viewModel.forgotPinStep.collectAsState()
    var biometricEnabled by remember { mutableStateOf(session?.biometricEnabled ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres & Sécurité", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CashPayNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CashPayNavyDark)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Card (Clickable to view full Telegram-style profile)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProfile?.invoke() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarBadge(
                        name = session?.name ?: "Utilisateur CashPay",
                        avatarUrl = session?.avatarUrl,
                        size = 64.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session?.name ?: "Utilisateur CashPay",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = session?.phone.orEmpty(),
                            color = CashPayGrey,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!session?.walletId.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OnChatCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Wallet ID : ${session?.walletId}",
                                    color = OnChatCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("SÉCURITÉ & AUTHENTIFICATION", color = CashPayGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = OnChatCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Biométrie / Empreinte", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("Déverrouillage rapide", color = CashPayGrey, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                viewModel.toggleBiometric(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OnChatCyan,
                                uncheckedTrackColor = CashPayNavyBorder
                            )
                        )
                    }

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startForgotPin(session?.phone) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = OnChatCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Code PIN & Sécurité", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Modifier votre PIN ou PIN Oublié ?", color = CashPayGrey, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CashPayGrey)
                    }

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTransactions?.invoke() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = OnChatCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Historique des Transactions", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Voir vos dépôts, retraits et transferts", color = CashPayGrey, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CashPayGrey)
                    }

                    Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Chiffrement End-to-End", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Messages et appels chiffrés", color = SuccessGreen, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guide & Astuces d'utilisation
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        com.example.data.api.SessionTokenStore.setOnboardingCompleted(false)
                        onBack()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guide & Astuces d'utilisation",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Revoir la présentation en 3 étapes (Chat, Wallet, Appels)",
                            color = CashPayGrey,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CashPayGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A1E1E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Déconnexion", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App info footer with official logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onchat_logo),
                    contentDescription = "OnChat",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OnChat Mobile v1.0 • FINUSECO SARL",
                    color = CashPayGrey,
                    fontSize = 12.sp
                )
            }
        }

        // Forgot PIN Bottom Sheet Overlay
        if (forgotPinStep != ForgotPinFlowStep.OFF) {
            ForgotPinSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.closeForgotPin() }
            )
        }
    }
}
