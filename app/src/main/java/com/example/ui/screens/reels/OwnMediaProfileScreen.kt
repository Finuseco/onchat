package com.example.ui.screens.reels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import androidx.compose.ui.platform.LocalContext
import com.example.ui.viewmodel.OnChatViewModel

@Composable
fun OwnMediaProfileScreen(viewModel:OnChatViewModel,onBack:()->Unit,onVideo:(Long)->Unit){
 val session by viewModel.currentSession.collectAsState()
 val id=session?.userId?:return
 CreatorMediaProfile(id,viewModel,onBack,onVideo)
}
