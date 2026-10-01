package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AvailableChannel
import com.example.data.model.Country
import com.example.ui.components.GradientButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthMethod
import com.example.ui.viewmodel.LoginStep
import com.example.ui.viewmodel.OnChatViewModel

@Composable
fun LoginScreen(viewModel: OnChatViewModel) {
    val loginStep by viewModel.loginStep.collectAsState()

    val authMethod by viewModel.authMethod.collectAsState()
    val countries by viewModel.countries.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val phoneNumber by viewModel.phoneNumberInput.collectAsState()
    val walletId by viewModel.walletIdInput.collectAsState()
    val otp by viewModel.otpInput.collectAsState()
    val pin by viewModel.pinInput.collectAsState()
    val availableChannels by viewModel.availableChannels.collectAsState()
    val isGoogleLinked by viewModel.isGoogleLinked.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val loading by viewModel.authLoading.collectAsState()
    val error by viewModel.authError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val showPinForgotDialog by viewModel.showPinForgotDialog.collectAsState()
    var showCountryPicker by remember { mutableStateOf(false) }

    if (showCountryPicker) {
        CountryPickerDialog(
            countries = countries,
            selectedCountry = selectedCountry,
            onSelectCountry = { viewModel.onSelectCountry(it) },
            onDismiss = { showCountryPicker = false }
        )
    }

    if (showPinForgotDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closePinForgotDialog() },
            containerColor = CashPayNavySurface,
            titleContentColor = Color.White,
            textContentColor = CashPayGrey,
            icon = {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = OnChatCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Récupération du PIN CashPay",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Pour préserver la sécurité de vos avoirs et de vos échanges, la réinitialisation du code secret CashPay requiert une validation d'identité conforme.\n\nVeuillez vous rapprocher de votre agence CashPay agréée la plus proche ou contacter le support client officiel de FINUSECO SARL.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.closePinForgotDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatVioletDeep),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("J'ai compris", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Deep modern dark canvas background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CashPayNavyDark)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official OnChat Logo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onchat_logo),
                    contentDescription = "Logo officiel OnChat",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brand Wordmark: "On" in purple (#B660ED), "Chat" in cyan (#44BBF5)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "On",
                    color = OnChatVioletLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Chat",
                    color = OnChatCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle pill with official gradient
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(brush = OnChatPrimaryGradient)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Écosystème CashPay • Messagerie & Appels",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stepper indicators: 4 sequential steps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDot(number = 1, label = "Identifiant", active = loginStep == LoginStep.WALLET_ID, completed = loginStep.ordinal > 0)
                StepDivider(active = loginStep.ordinal > 0)
                StepDot(number = 2, label = "Méthode", active = loginStep == LoginStep.CHANNEL_SELECTION, completed = loginStep.ordinal > 1)
                StepDivider(active = loginStep.ordinal > 1)
                StepDot(number = 3, label = "OTP", active = loginStep == LoginStep.OTP_VERIFY, completed = loginStep.ordinal > 2)
                StepDivider(active = loginStep.ordinal > 2)
                StepDot(number = 4, label = "PIN", active = loginStep == LoginStep.PIN_VERIFY, completed = loginStep == LoginStep.AUTHENTICATED)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Success Message Banner
                    if (successMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SuccessGreen.copy(alpha = 0.16f))
                                .border(1.dp, SuccessGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage ?: "",
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearSuccessMessage() }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Strict Error Banner: Formatted Title + User-friendly message (Never raw JSON/HTTP errors)
                    if (error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3F1420))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = error!!.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = error!!.message,
                                        color = Color(0xFFFFD1D1),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    when (loginStep) {
                        // Étape 1 : Choix du mode d'identification (Téléphone ou Wallet ID)
                        LoginStep.WALLET_ID -> {
                            Text(
                                text = "Connexion CashPay All",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Identifiez votre compte pour poursuivre la connexion.",
                                color = CashPayGrey,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Dual Authentication Switcher (Phone vs Wallet ID)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CashPayNavyDark)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (authMethod == AuthMethod.PHONE) Modifier.background(brush = OnChatPrimaryGradient)
                                            else Modifier.clickable { viewModel.onAuthMethodChange(AuthMethod.PHONE) }
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Téléphone",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (authMethod == AuthMethod.PHONE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (authMethod == AuthMethod.WALLET_ID) Modifier.background(brush = OnChatPrimaryGradient)
                                            else Modifier.clickable { viewModel.onAuthMethodChange(AuthMethod.WALLET_ID) }
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Wallet ID",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (authMethod == AuthMethod.WALLET_ID) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (authMethod == AuthMethod.PHONE) {
                                // Country Selector Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CashPayNavyDark)
                                        .border(1.dp, CashPayNavyBorder, RoundedCornerShape(12.dp))
                                        .clickable { showCountryPicker = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = selectedCountry.flagEmoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Pays sélectionné",
                                                color = CashPayGrey,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = selectedCountry.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "+${selectedCountry.cleanDialCode}",
                                            color = OnChatCyan,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CashPayGrey)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Phone input field (national number without dial code, or full number with/without +)
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { viewModel.onPhoneNumberChange(it) },
                                    label = { Text("Numéro national", color = CashPayGrey) },
                                    placeholder = { Text("ex: 833248614", color = CashPayGrey.copy(alpha = 0.5f)) },
                                    leadingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { showCountryPicker = true }
                                                .padding(horizontal = 10.dp)
                                        ) {
                                            Text(text = selectedCountry.flagEmoji, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "+${selectedCountry.cleanDialCode}",
                                                color = OnChatCyan,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CashPayGrey, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = OnChatCyan,
                                        unfocusedBorderColor = CashPayNavyBorder,
                                        cursorColor = OnChatCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Saisissez votre numéro sans l'indicatif. Les numéros internationaux avec ou sans '+' sont également acceptés.",
                                    color = CashPayGrey,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                GradientButton(
                                    text = "Identifier le compte",
                                    onClick = { viewModel.submitPhone() },
                                    isLoading = loading,
                                    icon = Icons.Default.ArrowForward
                                )
                            } else {
                                // Wallet ID Mode
                                OutlinedTextField(
                                    value = walletId,
                                    onValueChange = { viewModel.onWalletIdChange(it) },
                                    label = { Text("Wallet ID", color = CashPayGrey) },
                                    placeholder = { Text("ex: CD0100000001", color = CashPayGrey.copy(alpha = 0.5f)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OnChatCyan)
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = OnChatCyan,
                                        unfocusedBorderColor = CashPayNavyBorder,
                                        cursorColor = OnChatCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                GradientButton(
                                    text = "Identifier le compte",
                                    onClick = { viewModel.submitWalletId() },
                                    isLoading = loading,
                                    icon = Icons.Default.ArrowForward
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            TextButton(
                                onClick = { com.example.data.api.SessionTokenStore.setOnboardingCompleted(false) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = CashPayGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Astuces d'installation & Guide d'utilisation",
                                    color = CashPayGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Étape 2 : Choix explicite du canal de réception (Canaux dynamiques réels)
                        LoginStep.CHANNEL_SELECTION -> {
                            Text(
                                text = "Comment souhaitez-vous recevoir votre code ?",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sélectionnez une méthode de vérification disponible sur votre compte.",
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Dynamic channel list from account truth
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                availableChannels.forEach { channel ->
                                    val (icon, title, desc, tintColor) = getChannelPresentation(channel.type)
                                    ChannelOptionCard(
                                        icon = icon,
                                        title = title,
                                        subtitle = desc,
                                        destination = channel.maskedDestination,
                                        tintColor = tintColor,
                                        isLoading = loading,
                                        onClick = { viewModel.selectChannelAndSendOtp(channel) }
                                    )
                                }

                                // Google Account option (Only if account has genuine Google linkage)
                                if (isGoogleLinked) {
                                    ChannelOptionCard(
                                        icon = Icons.Default.AccountCircle,
                                        title = "Compte Google",
                                        subtitle = "Continuer avec Google",
                                        destination = "Compte Google lié",
                                        tintColor = Color(0xFF4285F4),
                                        isLoading = loading,
                                        onClick = { viewModel.continueWithGoogle() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(onClick = { viewModel.restartLogin() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = CashPayGrey, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Changer d'identifiant", color = CashPayGrey, fontSize = 12.sp)
                            }
                        }

                        // Étape 3 : Saisie et vérification de l'OTP
                        LoginStep.OTP_VERIFY -> {
                            Text(
                                text = "Entrez le code de vérification",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Saisissez le code à 6 chiffres reçu sur votre méthode sélectionnée.",
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedTextField(
                                value = otp,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 6) {
                                        viewModel.onOtpChange(it)
                                    }
                                },
                                label = { Text("Code à 6 chiffres", color = CashPayGrey) },
                                placeholder = { Text("123456", color = CashPayGrey.copy(alpha = 0.5f)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Pin, contentDescription = null, tint = OnChatCyan)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OnChatCyan,
                                    unfocusedBorderColor = CashPayNavyBorder,
                                    cursorColor = OnChatCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            GradientButton(
                                text = "Vérifier le code",
                                onClick = { viewModel.submitOtp() },
                                isLoading = loading,
                                icon = Icons.Default.Check
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.resendOtp() }) {
                                    Text("Renvoyer le code", color = OnChatCyan, fontSize = 12.sp)
                                }
                                TextButton(onClick = { viewModel.changeVerificationMethod() }) {
                                    Text("Changer de méthode", color = CashPayGrey, fontSize = 12.sp)
                                }
                            }
                        }

                        // Étape 4 : PIN de connexion CashPay sécurisé
                        LoginStep.PIN_VERIFY -> {
                            Text(
                                text = "Entrez votre PIN",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Confirmez votre code secret CashPay pour valider la session.",
                                color = CashPayGrey,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Secret PIN masked indicators: ● ● ● ● (Never shown in clear text)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < pin.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isFilled) {
                                                    Modifier.background(brush = OnChatPrimaryGradient)
                                                } else {
                                                    Modifier
                                                        .background(CashPayNavyDark)
                                                        .border(1.5.dp, CashPayNavyBorder, CircleShape)
                                                }
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (loading) {
                                CircularProgressIndicator(
                                    color = OnChatCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Vérification sécurisée...", color = CashPayGrey, fontSize = 12.sp)
                            } else {
                                // Secure on-screen PIN Keypad (1 to 9, 0, Backspace)
                                PinKeypad(
                                    onDigit = { viewModel.onPinDigit(it) },
                                    onBackspace = { viewModel.onPinBackspace() }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.openPinForgotDialog() }) {
                                    Text("PIN oublié ?", color = OnChatCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                TextButton(onClick = { viewModel.restartLogin() }) {
                                    Text("Recommencer", color = CashPayGrey, fontSize = 12.sp)
                                }
                            }
                        }

                        LoginStep.PROFILE_INSTALLATING -> {
                            val installationProgress by viewModel.installationProgress.collectAsState()
                            val installationStatusText by viewModel.installationStatusText.collectAsState()
                            val session by viewModel.currentSession.collectAsState()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(brush = OnChatPrimaryGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = installationProgress,
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(52.dp),
                                        strokeWidth = 4.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Installation de votre profil",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = installationStatusText,
                                    color = OnChatCyan,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                LinearProgressIndicator(
                                    progress = installationProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (installationProgress >= 1f) SuccessGreen else OnChatCyan,
                                    trackColor = CashPayNavyDark
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${(installationProgress * 100).toInt()} %",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CashPayNavyDark)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Données du contexte utilisateur :",
                                            color = CashPayGrey,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        InstallDataItemRow(icon = Icons.Default.Person, label = "Nom complet", value = session?.name ?: "Chargement...")
                                        InstallDataItemRow(icon = Icons.Default.Wallet, label = "Wallet ID", value = session?.walletId ?: "Chargement...")
                                        InstallDataItemRow(icon = Icons.Default.Work, label = "Profession", value = session?.profession ?: "Membre OnChat")
                                        InstallDataItemRow(icon = Icons.Default.Phone, label = "Téléphone", value = session?.phone ?: "Vérifié")
                                        InstallDataItemRow(icon = Icons.Default.Email, label = "E-mail", value = session?.email ?: "Non spécifié")
                                        InstallDataItemRow(icon = Icons.Default.Public, label = "Pays / Ville", value = "${session?.city ?: "Kinshasa"}, ${session?.country ?: "CD"}")
                                    }
                                }
                            }
                        }

                        LoginStep.PROFILE_CONFIRMATION -> {
                            val session by viewModel.currentSession.collectAsState()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(SuccessGreen.copy(alpha = 0.2f))
                                        .border(1.dp, SuccessGreen, RoundedCornerShape(50.dp))
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Installation 100% terminée",
                                            color = SuccessGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, OnChatCyan, CircleShape)
                                        .background(CashPayNavyDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!session?.avatarUrl.isNullOrEmpty()) {
                                        coil.compose.AsyncImage(
                                            model = session?.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(40.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = session?.name ?: "Utilisateur OnChat",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = session?.walletId ?: "",
                                    color = OnChatCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CashPayNavyDark)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        ProfileDetailRow(label = "Profession", value = session?.profession ?: "Membre OnChat")
                                        ProfileDetailRow(label = "Téléphone", value = session?.phone ?: "Non spécifié")
                                        ProfileDetailRow(label = "Localisation", value = "${session?.city ?: "Kinshasa"}, ${session?.country ?: "CD"}")
                                        if (!session?.astroSign.isNullOrEmpty()) {
                                            ProfileDetailRow(label = "Astro", value = "${session?.astroSign} (${session?.astroElement ?: ""})")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelProfileInstallation() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CashPayNavyBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CashPayGrey)
                                    ) {
                                        Text("Annuler")
                                    }

                                    Button(
                                        onClick = { viewModel.confirmProfile() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OnChatVioletDeep)
                                    ) {
                                        Text("Continuer", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        LoginStep.AUTHENTICATED -> {
                            CircularProgressIndicator(color = OnChatCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryPickerDialog(
    countries: List<Country>,
    selectedCountry: Country,
    onSelectCountry: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery, countries) {
        if (searchQuery.isBlank()) countries
        else {
            val q = searchQuery.trim().lowercase()
            countries.filter {
                it.name.lowercase().contains(q) ||
                it.code.lowercase().contains(q) ||
                it.dialCode.contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CashPayNavySurface,
        titleContentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        title = {
            Column {
                Text(
                    text = "Sélectionner un pays",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher pays ou indicatif...", color = CashPayGrey.copy(alpha = 0.6f), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CashPayGrey, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        unfocusedBorderColor = CashPayNavyBorder,
                        cursorColor = OnChatCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                items(filteredCountries) { country ->
                    val isSelected = country.code.equals(selectedCountry.code, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) OnChatVioletDeep.copy(alpha = 0.35f) else Color.Transparent)
                            .clickable {
                                onSelectCountry(country)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = country.flagEmoji,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = country.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+${country.cleanDialCode}",
                            color = if (isSelected) OnChatCyan else CashPayGrey,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = CashPayNavyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = OnChatCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ChannelOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    destination: String?,
    tintColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!destination.isNullOrBlank()) {
                    Text(
                        text = destination,
                        color = OnChatCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = subtitle,
                        color = CashPayGrey,
                        fontSize = 10.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CashPayGrey,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class ChannelMeta(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val tintColor: Color
)

private fun getChannelPresentation(type: String): ChannelMeta {
    return when (type.lowercase()) {
        "sms" -> ChannelMeta(
            icon = Icons.Default.PhoneAndroid,
            title = "SMS",
            desc = "Code envoyé par SMS",
            tintColor = OnChatCyan
        )
        "whatsapp" -> ChannelMeta(
            icon = Icons.Default.Chat,
            title = "WhatsApp",
            desc = "Code reçu sur WhatsApp",
            tintColor = Color(0xFF25D366)
        )
        "email" -> ChannelMeta(
            icon = Icons.Default.Email,
            title = "Email",
            desc = "Code envoyé par email",
            tintColor = OnChatVioletLight
        )
        "in-app" -> ChannelMeta(
            icon = Icons.Default.Security,
            title = "App CashPay",
            desc = "Application CashPay Authenticator",
            tintColor = Color(0xFFF59E0B)
        )
        else -> ChannelMeta(
            icon = Icons.Default.Lock,
            title = type.replaceFirstChar { it.uppercase() },
            desc = "Code de vérification",
            tintColor = OnChatCyan
        )
    }
}

@Composable
private fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit
) {
    val digits = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(null, '0', '<')
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        null -> {
                            Spacer(modifier = Modifier.size(46.dp))
                        }
                        '<' -> {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CashPayNavyDark)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Effacer",
                                    tint = CashPayGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CashPayNavyDark)
                                    .border(1.dp, CashPayNavyBorder, CircleShape)
                                    .clickable { onDigit(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key.toString(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDot(number: Int, label: String, active: Boolean, completed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    when {
                        completed -> Modifier.background(SuccessGreen)
                        active -> Modifier.background(brush = OnChatPrimaryGradient)
                        else -> Modifier.background(CashPayNavyDark).border(1.dp, CashPayNavyBorder, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            } else {
                Text(
                    text = "$number",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (active || completed) Color.White else CashPayGrey,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StepDivider(active: Boolean) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(2.dp)
            .then(
                if (active) Modifier.background(brush = OnChatPrimaryGradient)
                else Modifier.background(CashPayNavyBorder)
            )
    )
}

@Composable
private fun InstallDataItemRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label :", color = CashPayGrey, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CashPayGrey, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
