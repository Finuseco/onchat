package com.example.ui.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AvatarBadge
import com.example.ui.screens.meetings.MeetingWebScreen
import com.example.ui.viewmodel.CallSessionState
import com.example.ui.viewmodel.OnChatViewModel

/** Direct-call surface uses the authenticated WebRTC room, never a local camera placeholder. */
@Composable
fun ActiveCallScreen(callState: CallSessionState, viewModel: OnChatViewModel) {
    val url by viewModel.directCallUrl.collectAsState()
    if (url != null) {
        MeetingWebScreen(url = url!!, onClose = { viewModel.endCall() }, directCall = true, onConnected = viewModel::reportDirectCallConnected)
    } else {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF4768EF),Color(0xFF12ADD5)))).systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AvatarBadge(name = callState.peerName, avatarUrl = callState.peerAvatar, size = 160.dp)
            Spacer(Modifier.height(24.dp))
            Text(callState.peerName, color = Color.White, fontSize = 32.sp)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Color.White)
            Text("Connexion…", color = Color.White)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { viewModel.endCall() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Annuler") }
        }
    }
}
