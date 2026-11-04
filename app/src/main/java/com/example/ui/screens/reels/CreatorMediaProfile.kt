package com.example.ui.screens.reels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.api.ApiClient
import com.example.data.model.VideoItem
import com.example.data.model.ReusableSound
import com.example.ui.components.AvatarBadge
import com.example.ui.viewmodel.OnChatViewModel
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable
internal fun MediaVideoThumbnail(video:VideoItem,modifier:Modifier=Modifier){
 val context=LocalContext.current
 val loader=remember{ImageLoader.Builder(context.applicationContext).components{add(VideoFrameDecoder.Factory())}.build()}
 DisposableEffect(loader){onDispose{loader.shutdown()}}
 var fallback by remember(video.id,video.resolvedThumbnailUrl){mutableStateOf(false)}
 val poster=video.resolvedThumbnailUrl?.takeIf{it.isNotBlank()}?:"${ApiClient.BASE_URL}api/v1/media/videos/${video.id}/thumbnail"
 val model=ImageRequest.Builder(context).data(if(fallback)video.resolvedUrl else poster).videoFrameMillis(1000).crossfade(true).build()
 SubcomposeAsyncImage(model=model,imageLoader=loader,contentDescription="Miniature vidéo",modifier=modifier,contentScale=ContentScale.Crop,
  loading={Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(Modifier.size(24.dp))}},
  error={if(!fallback){LaunchedEffect(video.id){fallback=true}}else Box(Modifier.fillMaxSize().background(Color(0xFF302044)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Videocam,contentDescription=null);Text("Ouvrir la vidéo",style=MaterialTheme.typography.labelSmall)}}})
}

@Composable
internal fun CreatorMediaProfile(id:Long,viewModel:OnChatViewModel,onBack:()->Unit,onVideo:(Long)->Unit,onUseSound:((ReusableSound)->Unit)?=null){
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var summary by remember(id){mutableStateOf<JSONObject?>(null)}
 var videos by remember(id){mutableStateOf<List<VideoItem>>(emptyList())}
 var sounds by remember(id){mutableStateOf<List<JSONObject>>(emptyList())}
 var tab by remember(id){mutableIntStateOf(0)}
 var busy by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 var more by remember{mutableStateOf(false)}
 var offset by remember{mutableIntStateOf(0)}
 var revision by remember{mutableIntStateOf(0)}
 var following by remember(id){mutableStateOf(false)}
 var followBusy by remember{mutableStateOf(false)}
 var playing by remember{mutableStateOf<Long?>(null)}
 val player=remember{androidx.media3.exoplayer.ExoPlayer.Builder(context).build()}
 DisposableEffect(player){val listener=object:androidx.media3.common.Player.Listener{override fun onPlayerError(failure:androidx.media3.common.PlaybackException){error="Lecture du son impossible. Vérifiez votre connexion.";playing=null}};player.addListener(listener);onDispose{player.removeListener(listener)}}
 DisposableEffect(player){onDispose{player.release()}}
 BackHandler(onBack=onBack)
 suspend fun load(append:Boolean=false){
  busy=true;error=null
  try{
   val start=if(append)offset else 0
   val data=CreatorMediaApi.get(id,if(tab==2)"used" else "original",if(tab==0)0 else start)
   if(tab==0){val batch=viewModel.repository.fetchVideos(20,start,id,"explore").getOrThrow();videos=if(append)(videos+batch).distinctBy{it.id} else batch;offset=start+batch.size;more=batch.size==20}
   else{val array=data.getJSONArray("sounds");val batch=(0 until array.length()).map{array.getJSONObject(it)};sounds=if(append)sounds+batch else batch;offset=start+batch.size;more=data.getJSONObject("pagination").optBoolean("hasMore")}
   summary=data;following=data.optBoolean("isFollowing")
  }catch(e:Exception){if(e is CancellationException)throw e;error=e.message}finally{busy=false}
 }
 LaunchedEffect(id,tab,revision){player.stop();playing=null;offset=0;more=false;videos=emptyList();sounds=emptyList();load()}
 val stats=summary?.optJSONObject("stats")
 val creator=summary?.optJSONObject("creator")
 Surface(Modifier.fillMaxSize(),color=Color(0xFF0D0816),contentColor=Color.White){Column(Modifier.fillMaxSize().statusBarsPadding()){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack){Text("Retour")};Text("Profil média",style=MaterialTheme.typography.titleLarge)}
  Column(Modifier.fillMaxWidth().padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
   AvatarBadge(name=creator?.optString("name")?:"Créateur",avatarUrl=creator?.optString("avatarUrl")?.takeIf{it!="null"},size=72.dp)
   Text(creator?.optString("name")?:"Chargement du profil…",style=MaterialTheme.typography.titleLarge)
   Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceEvenly){for((key,label) in listOf("followers" to "Abonnés","following" to "Suivis","videos" to "Vidéos","media" to "Médias","likes" to "J’aime")){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(stats?.optInt(key)?.toString()?:"—");Text(label,style=MaterialTheme.typography.labelSmall)}}}
   if(id!=ApiClient.currentUserId)Button(enabled=!followBusy&&summary!=null,onClick={followBusy=true;viewModel.performSocialAction(id,if(following)"unfollow" else "follow",onComplete={ok->followBusy=false;if(ok)revision++ else error="Abonnement non confirmé. Réessayez."})}){Text(if(following)"Abonné ✓" else "S’abonner")}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){listOf("Vidéos","Sons originaux","Sons utilisés").forEachIndexed{index,label->TextButton(onClick={tab=index},enabled=!busy){Text(label,color=if(tab==index)Color(0xFFCB86FF)else Color.LightGray)}}}
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let{Text(it,color=Color(0xFFFFCC80),modifier=Modifier.padding(12.dp));TextButton(enabled=!busy,onClick={revision++}){Text("Réessayer")}}
  if(tab==0)LazyVerticalGrid(columns=GridCells.Fixed(3),modifier=Modifier.weight(1f),contentPadding=PaddingValues(3.dp)){
   items(videos,key={it.id}){video->Box(Modifier.padding(2.dp).aspectRatio(.75f).clickable{onVideo(video.id)}){MediaVideoThumbnail(video,Modifier.fillMaxSize());Icon(Icons.Default.PlayArrow,contentDescription="Lire",modifier=Modifier.align(Alignment.BottomStart).padding(6.dp))}}
   if(videos.isEmpty()&&!busy&&error==null)item(span={GridItemSpan(3)}){Text("Aucune vidéo publique.",modifier=Modifier.padding(24.dp))}
  }else LazyColumn(Modifier.weight(1f)){
   items(sounds,key={it.getLong("videoId")}){sound->Column(Modifier.fillMaxWidth().padding(12.dp)){
    Text(sound.optString("title"),style=MaterialTheme.typography.titleMedium)
    Text("♪ ${sound.optString("authorName")} · "+if(sound.optBoolean("allowReuse"))"Réutilisation autorisée avec crédit" else "Droits réservés à l’auteur",style=MaterialTheme.typography.bodySmall)
    Row{TextButton(onClick={try{if(playing==sound.getLong("videoId")){player.pause();playing=null}else{player.setMediaItem(androidx.media3.common.MediaItem.fromUri(sound.getString("url")));player.prepare();player.play();playing=sound.getLong("videoId")}}catch(e:Exception){error=e.message}}){Text(if(playing==sound.getLong("videoId"))"Pause" else "Écouter")}
     TextButton(onClick={onVideo(sound.getLong("videoId"))}){Text("Voir la vidéo")}
     if(onUseSound!=null)TextButton(enabled=sound.optBoolean("allowReuse"),onClick={onUseSound(ReusableSound(sound.getLong("sourceId"),sound.getString("title"),true,sound.getLong("authorId"),sound.optString("authorName"),sound.optString("authorAvatar").takeIf{it!="null"},sound.getString("url")))}){Text("Utiliser")}
    }
   }}
   if(sounds.isEmpty()&&!busy&&error==null)item{Text("Aucun son public dans cette section.",modifier=Modifier.padding(24.dp))}
  }
  if(more)TextButton(enabled=!busy,onClick={scope.launch{load(true)}}){Text("Afficher la suite")}
 }}
}
