package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OnChatCyan
import com.example.ui.viewmodel.OnChatViewModel

@Composable
fun LoginScreen(
    viewModel: OnChatViewModel,
    onLoginSuccess: () -> Unit
) {
    val currentPhone by viewModel.userPhone.collectAsState()
    val currentToken by viewModel.sessionToken.collectAsState()

    var phoneInput by remember(currentPhone) { mutableStateOf(currentPhone) }
    var tokenInput by remember(currentToken) { mutableStateOf(currentToken ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OnChat CashPay",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OnChatCyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connexion & Session API",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneInput,
            onValueChange = { phoneInput = it },
            label = { Text("Numéro de téléphone", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OnChatCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OnChatCyan,
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_login_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text("Jeton de Session (Bearer Token)", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OnChatCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OnChatCyan,
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("token_login_input")
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                viewModel.saveSession(phoneInput, tokenInput)
                onLoginSuccess()
            },
            colors = ButtonDefaults.buttonColors(containerColor = OnChatCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_session_button")
        ) {
            Text("Enregistrer & Continuer", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
