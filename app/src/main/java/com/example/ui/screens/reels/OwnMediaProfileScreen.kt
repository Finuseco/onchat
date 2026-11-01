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
 val all by viewModel.profileVideos.collectAsState()
 val more by viewModel.profileVideoMore.collectAsState()
 val busy by viewModel.profileVideosLoading.collectAsState()
 val id=session?.userId?:return
 val context=LocalContext.current
 val loader=remember{ImageLoader.Builder(context.applicationContext).components{add(VideoFrameDecoder.Factory())}.build()}
 DisposableEffect(loader){onDispose{loader.shutdown()}}
 LaunchedEffect(id){viewModel.fetchProfileVideos(id)}
 Surface(Modifier.fillMaxSize()){
  Column(Modifier.fillMaxSize().statusBarsPadding()){
   TextButton(onClick=onBack){Text("Retour")}
   Text("Mon profil média",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(horizontal=16.dp))
   Text("Mes vidéos publiées",modifier=Modifier.padding(16.dp))
   if(id in busy)LinearProgressIndicator(Modifier.fillMaxWidth())
   LazyVerticalGrid(columns=GridCells.Fixed(3),modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp)){
    items(all[id].orEmpty(),key={it.id}){video->AsyncImage(model=video.thumbnailUrl?.takeIf{it.isNotBlank()}?:video.resolvedUrl,imageLoader=loader,contentDescription="Ouvrir ma vidéo",contentScale=ContentScale.Crop,modifier=Modifier.padding(2.dp).aspectRatio(.75f).clickable{onVideo(video.id)})}
   }
   if(more[id]==true)TextButton(enabled=id !in busy,onClick={viewModel.fetchProfileVideos(id,true)}){Text("Voir mes autres vidéos")}
   if(all[id].isNullOrEmpty()&&id !in busy)Text("Aucune vidéo publiée.",modifier=Modifier.padding(24.dp))
  }
 }
}
