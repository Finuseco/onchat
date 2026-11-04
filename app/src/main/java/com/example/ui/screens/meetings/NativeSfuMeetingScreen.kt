package com.example.ui.screens.meetings

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.AvatarBadge
import com.example.data.api.OnChatWebSocketManager
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.DisconnectReason
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalScreencastVideoTrack
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.audio.AudioSwitchHandler
import kotlinx.coroutines.*
import org.json.JSONObject

private data class NativeTile(val key:String,val id:String,val name:String,val avatar:String?,val track:VideoTrack?,val local:Boolean,val screen:Boolean,val speaking:Boolean,val mic:Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeSfuMeetingScreen(ticket:SfuMeetingTicket,onClose:()->Unit,onConnected:(Boolean)->Unit){
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var attempt by remember{mutableIntStateOf(0)}
 val room=remember(ticket,attempt){LiveKit.create(context.applicationContext,RoomOptions(adaptiveStream=true,dynacast=true))}
 var tiles by remember{mutableStateOf<List<NativeTile>>(emptyList())}
 var connected by remember{mutableStateOf(false)}
 var publish by remember{mutableStateOf(ticket.canPublish)}
 var mic by remember{mutableStateOf(false)}
 var camera by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 var grid by remember{mutableStateOf(false)}
 var pinned by remember{mutableStateOf<String?>(null)}
 var page by remember{mutableIntStateOf(0)}
 var panel by remember{mutableStateOf<String?>(null)}
 var price by remember{mutableStateOf("0")}
 var currency by remember{mutableStateOf("USD")}
 var saved by remember{mutableStateOf(false)}
 var revenue by remember{mutableStateOf<List<JSONObject>>(emptyList())}
 var roster by remember{mutableStateOf<List<JSONObject>>(emptyList())}
 var messages by remember{mutableStateOf<List<JSONObject>>(emptyList())}
 var text by remember{mutableStateOf("")}
 var busy by remember{mutableStateOf(false)}
 var hand by remember{mutableStateOf(false)}
 var pendingPermission by remember{mutableStateOf<String?>(null)}
 var screen by remember{mutableStateOf<LocalScreencastVideoTrack?>(null)}
 val ws=remember{OnChatWebSocketManager.getInstance()}
 fun snapshot(){
  fun tile(participant:Participant,local:Boolean,source:Track.Source):NativeTile{
   val id=participant.identity?.value?:"self";val metadata=runCatching{JSONObject(participant.metadata?:"{}").optString("avatar").takeIf{it.isNotBlank()&&it!="null"}}.getOrNull()
   val publication=participant.getTrackPublication(source)
   return NativeTile(id+":"+source.name,id,participant.name?:"Participant",metadata,publication?.takeIf{!it.muted}?.track as? VideoTrack,local,source==Track.Source.SCREEN_SHARE,participant.isSpeaking,participant.isMicrophoneEnabled())
  }
  val next=mutableListOf<NativeTile>()
  for(participant in listOf(room.localParticipant)+room.remoteParticipants.values){
   next+=tile(participant,participant===room.localParticipant,Track.Source.CAMERA)
   if(participant.isScreenShareEnabled())next.add(0,tile(participant,participant===room.localParticipant,Track.Source.SCREEN_SHARE))
  }
  tiles=next;publish=room.localParticipant.permissions?.canPublish==true;mic=room.localParticipant.isMicrophoneEnabled();camera=room.localParticipant.isCameraEnabled()
  onConnected(connected&&room.remoteParticipants.isNotEmpty())
 }
 fun operation(name:String,body:JSONObject=JSONObject(),after:()->Unit={}){scope.launch{try{NativeMeetingApi.request(ticket.code,name,body);after()}catch(e:Exception){if(e is CancellationException)throw e;error=e.message}}}
 suspend fun refresh(){
  val data=NativeMeetingApi.request(ticket.code,"participants")
  if(data.optString("meetingStatus")=="ended"){room.disconnect();onClose();return}
  val receipts=data.optJSONArray("revenue");revenue=(0 until (receipts?.length()?:0)).mapNotNull{receipts?.optJSONObject(it)}
  val list=data.optJSONArray("participants");roster=(0 until (list?.length()?:0)).mapNotNull{list?.optJSONObject(it)}
 }
 suspend fun refreshChat(){val data=NativeMeetingApi.request(ticket.code,"messages");val list=data.optJSONArray("messages");messages=(0 until (list?.length()?:0)).mapNotNull{list?.optJSONObject(it)}}
 fun stopScreen(){screen?.let{room.localParticipant.unpublishTrack(it);it.stop()};screen=null;snapshot()}
 val projection=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  val permission=result.data
  if(result.resultCode==Activity.RESULT_OK&&permission!=null)scope.launch{
   var track:LocalScreencastVideoTrack?=null
   try{
    if(room.localParticipant.permissions?.canPublish!=true)kotlin.error("L’organisateur doit vous autoriser à présenter.")
    val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("onchat_screen_share","Partage d’écran OnChat",NotificationManager.IMPORTANCE_LOW))
    val notification=NotificationCompat.Builder(context,"onchat_screen_share").setSmallIcon(R.drawable.ic_onchat_notification).setContentTitle("OnChat partage votre écran").setContentText("Arrêtez le partage depuis la réunion ou Android.").setOngoing(true).build()
    track=room.localParticipant.createScreencastTrack("Présentation",permission)
    track.startForegroundService(7401,notification);track.startCapture();room.localParticipant.publishVideoTrack(track);screen=track;snapshot()
   }catch(e:Exception){track?.stop();if(e is CancellationException)throw e;error=e.message?:"Partage d’écran indisponible"}
  }
 }
 fun media(kind:String){scope.launch{try{
  when(kind){"mic"->room.localParticipant.setMicrophoneEnabled(!mic);"camera"->room.localParticipant.setCameraEnabled(!camera);"initial"->{room.localParticipant.setMicrophoneEnabled(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED);if(ticket.withVideo)room.localParticipant.setCameraEnabled(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}}
  snapshot()
 }catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"Vérifiez les autorisations du micro et de la caméra."}}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->val kind=pendingPermission;pendingPermission=null;if(result.values.any{it}&&kind!=null)media(kind)else error="Autorisation refusée. Vous pouvez continuer à écouter la réunion."}
 fun requestMedia(kind:String){
  if(!publish)return
  val required=when(kind){"camera"->arrayOf(Manifest.permission.CAMERA);"mic"->arrayOf(Manifest.permission.RECORD_AUDIO);else->if(ticket.withVideo)arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)else arrayOf(Manifest.permission.RECORD_AUDIO)}
  val missing=required.filter{ContextCompat.checkSelfPermission(context,it)!=PackageManager.PERMISSION_GRANTED}
  if(missing.isEmpty())media(kind)else{pendingPermission=kind;permission.launch(missing.toTypedArray())}
 }
 LaunchedEffect(room){
  launch{room.events.collect{event->
   when(event){is RoomEvent.Connected,is RoomEvent.Reconnected->{connected=true;error=null};is RoomEvent.Reconnecting->{connected=false;error="Reconnexion en cours…"};is RoomEvent.Disconnected->{connected=false;if(event.reason in setOf(DisconnectReason.PARTICIPANT_REMOVED,DisconnectReason.ROOM_DELETED,DisconnectReason.ROOM_CLOSED,DisconnectReason.DUPLICATE_IDENTITY)){onClose()}else error="Connexion interrompue. Réessayez ou quittez la réunion."};is RoomEvent.ParticipantPermissionsChanged->if(event.newPermissions?.canPublish!=true){room.localParticipant.setMicrophoneEnabled(false);room.localParticipant.setCameraEnabled(false);stopScreen()};else->{}}
   snapshot()
  }}
  try{
   val fresh=if(attempt==0)ticket else SfuMeetingTicket.parse(NativeMeetingApi.request(ticket.code,"connection",JSONObject()).put("withVideo",ticket.withVideo).toString())
   withTimeout(30000){room.connect(fresh.url,fresh.token)};connected=true;snapshot();if(publish)requestMedia("initial")
  }catch(e:Exception){if(e is CancellationException&&e !is TimeoutCancellationException)throw e;room.disconnect();connected=false;error="Impossible de rejoindre le serveur média. Vérifiez votre connexion et réessayez."}
 }
 DisposableEffect(room){ws.watchMeeting(ticket.code);onDispose{screen?.stop();room.disconnect();room.release();ws.unwatchMeeting(ticket.code);runCatching{com.example.service.OnChatBackgroundService.mediaState(context,false)}}}
 LaunchedEffect(mic,camera){runCatching{com.example.service.OnChatBackgroundService.mediaState(context,mic||camera,camera)}}
 LaunchedEffect(ticket.code){while(isActive){try{refresh();if(panel=="chat")refreshChat()}catch(e:Exception){if(e is CancellationException)throw e};delay(5000)}}
 LaunchedEffect(ticket.code,panel){if(panel=="admin")runCatching{val data=NativeMeetingApi.request(ticket.code,"billing").getJSONObject("billing");price=data.optString("priceAmount","0");currency=data.optString("priceCurrency","USD");saved=false}.onFailure{error=it.message};if(panel=="chat")runCatching{refreshChat()};ws.meetingChatUpdates.collect{code->if(code==ticket.code&&panel=="chat")runCatching{refreshChat()}}}
 LaunchedEffect(screen){while(screen!=null){delay(500);if(screen?.enabled==false)stopScreen()}}
 val stage=tiles.find{it.key==pinned}?:tiles.firstOrNull{it.screen}?:tiles.firstOrNull{it.speaking}?:tiles.firstOrNull()
 val candidates=if(grid)tiles else tiles.filter{it.key!=stage?.key}
 val pageSize=if(grid)6 else 8;val pages=maxOf(1,(candidates.size+pageSize-1)/pageSize);val currentPage=page.coerceAtMost(pages-1)
 val shown=candidates.drop(currentPage*pageSize).take(pageSize)
 Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF180923),Color(0xFF2D1043)))).statusBarsPadding().navigationBarsPadding()){
  Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
   Image(painterResource(R.drawable.onchat_logo),contentDescription="OnChat",modifier=Modifier.width(80.dp).height(32.dp))
   Column(Modifier.weight(1f).padding(start=8.dp)){Text(ticket.topic,color=Color.White,maxLines=1);Text("${room.remoteParticipants.size+1} participants • ${if(connected)"En direct" else "Connexion"}",color=if(connected)Color(0xFF4ADE80)else Color.LightGray,style=MaterialTheme.typography.labelSmall)}
   IconButton(onClick={grid=!grid;page=0}){Icon(if(grid)Icons.Default.ViewAgenda else Icons.Default.GridView,"Changer la vue",tint=Color.White)}
   IconButton(onClick={panel="more"}){Icon(Icons.Default.MoreVert,"Plus",tint=Color.White)}
  }
  if(error!=null)Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text(error!!,color=Color(0xFFFDA4AF),style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f));if(!connected)TextButton(onClick={attempt++}){Text("Réessayer")}}
  if(!connected&&tiles.isEmpty())Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Color(0xFFA855F7))}
  else if(grid)LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f).padding(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   items(shown,key={it.key}){tile->NativeParticipantTile(room,tile,Modifier.fillMaxWidth().aspectRatio(.95f).clickable{pinned=tile.key;grid=false;page=0})}
  }else{
   stage?.let{tile->NativeParticipantTile(room,tile,Modifier.fillMaxWidth().weight(1f).padding(8.dp))}
   LazyRow(Modifier.fillMaxWidth().height(108.dp).padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
    items(shown,key={it.key}){tile->NativeParticipantTile(room,tile,Modifier.width(130.dp).fillMaxHeight().clickable{pinned=tile.key;page=0})}
   }
  }
  if(pages>1)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){IconButton(enabled=currentPage>0,onClick={page=currentPage-1}){Icon(Icons.Default.ChevronLeft,"Précédents",tint=Color.White)};Text("${currentPage+1} / $pages",color=Color.LightGray);IconButton(enabled=currentPage+1<pages,onClick={page=currentPage+1}){Icon(Icons.Default.ChevronRight,"Suivants",tint=Color.White)}}
  LazyRow(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   item{NativeControl(if(camera)"Caméra"else"Caméra coupée",if(camera)Icons.Default.Videocam else Icons.Default.VideocamOff,publish){requestMedia("camera")}}
   item{NativeControl(if(mic)"Micro"else"Micro coupé",if(mic)Icons.Default.Mic else Icons.Default.MicOff,publish){requestMedia("mic")}}
   item{NativeControl(if(screen!=null)"Arrêter partage"else"Partager",Icons.Default.ScreenShare,publish){if(screen!=null)stopScreen()else projection.launch((context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent())}}
   item{NativeControl("Participants",Icons.Default.Groups){panel="participants"}}
   item{NativeControl("Discussion",Icons.Default.Chat){panel="chat"}}
   item{NativeControl("Quitter",Icons.Default.CallEnd,danger=true){room.disconnect();onClose()}}
  }
 }
 if(panel!=null)ModalBottomSheet(onDismissRequest={panel=null},containerColor=Color(0xFF241135)){
  Column(Modifier.fillMaxWidth().fillMaxHeight(.8f).padding(16.dp)){
   Text(when(panel){"participants"->"Participants (${room.remoteParticipants.size+1})";"chat"->"Discussion de la réunion";"admin"->"Administration de la réunion";else->"Contrôles de la réunion"},color=Color.White,style=MaterialTheme.typography.titleLarge)
   if(error!=null)Text(error!!,color=Color(0xFFFDA4AF),style=MaterialTheme.typography.labelSmall)
   if(panel=="participants")LazyColumn(Modifier.weight(1f)){
    items(roster.filter{person->tiles.any{it.id==person.optLong("userId").toString()}},key={it.optLong("userId")}){person->Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){AvatarBadge(person.optString("name"),person.optString("avatar").takeIf{it.isNotBlank()&&it!="null"},size=40.dp);Text(person.optString("name"),color=Color.White,modifier=Modifier.weight(1f).padding(horizontal=8.dp));if(person.optBoolean("raiseHand"))Icon(Icons.Default.PanTool,"Main levée",tint=Color.Yellow);if(ticket.isHost&&!person.optBoolean("isHost")){TextButton(onClick={operation("grant-speech",JSONObject().put("targetUserId",person.optLong("userId")).put("canSpeak",!person.optBoolean("canSpeak")))}){Text(if(person.optBoolean("canSpeak"))"Retirer"else"Autoriser")};IconButton(onClick={operation("kick",JSONObject().put("targetUserId",person.optLong("userId")))}){Icon(Icons.Default.PersonRemove,"Retirer de la réunion",tint=Color(0xFFFDA4AF))}}}}
   }else if(panel=="chat"){
    LazyColumn(Modifier.weight(1f)){items(messages,key={it.optString("id")}){message->Column(Modifier.fillMaxWidth().padding(vertical=8.dp)){Text(message.optString("name"),color=Color(0xFFD8B4FE));Text(message.optString("content"),color=Color.White)}}}
    OutlinedTextField(value=text,onValueChange={if(it.length<=2000)text=it},label={Text("Écrire un message")},modifier=Modifier.fillMaxWidth())
    Button(enabled=!busy&&text.isNotBlank(),onClick={busy=true;scope.launch{try{NativeMeetingApi.request(ticket.code,"messages",JSONObject().put("content",text.trim()));text="";refreshChat()}catch(e:Exception){if(e is CancellationException)throw e;error=e.message}finally{busy=false}}}){Text("Envoyer")}
   }else if(panel=="admin"&&ticket.isHost){
    OutlinedTextField(value=price,onValueChange={price=it;saved=false},label={Text("Tarif des prochaines participations")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(value=currency,onValueChange={currency=it.uppercase().take(10);saved=false},label={Text("Devise")},modifier=Modifier.fillMaxWidth())
    Button(onClick={val amount=price.toDoubleOrNull();if(amount==null||!amount.isFinite()||amount<0)error="Montant invalide" else operation("billing",JSONObject().put("priceAmount",amount).put("priceCurrency",currency)){saved=true}}){Text("Enregistrer le tarif")}
    if(saved)Text("Tarif enregistré",color=Color(0xFF4ADE80))
    Text("Revenus bruts reçus",color=Color.White,modifier=Modifier.padding(top=12.dp))
    revenue.forEach{receipt->Text("${receipt.optString("amount")} ${receipt.optString("currency")} • ${receipt.optInt("payments")} paiement(s)",color=Color(0xFFD8B4FE))}
   }else{
    TextButton(onClick={val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OnChat","https://app.cashpay-all.com/feedback/meet/${ticket.code}"));panel=null}){Text("Copier le lien d’invitation")}
    TextButton(enabled=publish&&camera,onClick={(room.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack)?.switchCamera()}){Text("Retourner la caméra")}
    TextButton(onClick={operation("raise-hand",JSONObject().put("raised",!hand)){hand=!hand}}){Text(if(hand)"Baisser la main"else"Lever la main")}
    val audio=room.audioHandler as? AudioSwitchHandler
    audio?.availableAudioDevices?.forEach{device->TextButton(onClick={audio.selectDevice(device)}){Text("Son : ${device.name}")}}
    if(ticket.isHost){TextButton(onClick={panel="admin"}){Text("Tarifs et revenus")};TextButton(onClick={operation("mute-all")}){Text("Couper les micros des participants")};TextButton(onClick={operation("end"){room.disconnect();onClose()}}){Text("Terminer pour tous",color=Color(0xFFFDA4AF))}}
   }
  }
 }
}

@Composable
private fun NativeParticipantTile(room:Room,tile:NativeTile,modifier:Modifier){
 val context=LocalContext.current
 Surface(modifier=modifier,shape=RoundedCornerShape(18.dp),color=Color(0xFF2C1C40),border=BorderStroke(if(tile.speaking)2.dp else 1.dp,if(tile.speaking)Color(0xFFA855F7)else Color(0xFF49345F))){
  Box(Modifier.fillMaxSize()){
   if(tile.track!=null){
    val renderer=remember(room,tile.track){TextureViewRenderer(context).apply{room.initVideoRenderer(this);setMirror(tile.local&&!tile.screen)}}
    DisposableEffect(renderer,tile.track){tile.track.addRenderer(renderer);onDispose{tile.track.removeRenderer(renderer);renderer.release()}}
    AndroidView(factory={renderer},modifier=Modifier.fillMaxSize())
   }else AvatarBadge(tile.name,tile.avatar,size=80.dp,modifier=Modifier.align(Alignment.Center))
   Text(if(tile.screen)"Présentation • ${tile.name}"else tile.name,color=Color.White,style=MaterialTheme.typography.labelSmall,modifier=Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha=.5f)).padding(6.dp))
   Icon(if(tile.mic)Icons.Default.Mic else Icons.Default.MicOff,null,tint=if(tile.mic)Color(0xFF4ADE80)else Color.LightGray,modifier=Modifier.align(Alignment.BottomEnd).padding(6.dp).size(18.dp))
  }
 }
}
@Composable
private fun NativeControl(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,enabled:Boolean=true,danger:Boolean=false,onClick:()->Unit){
 Column(horizontalAlignment=Alignment.CenterHorizontally){FilledIconButton(onClick=onClick,enabled=enabled,colors=IconButtonDefaults.filledIconButtonColors(containerColor=if(danger)Color(0xFFEF183E)else Color(0xFF4A1C68)),modifier=Modifier.size(46.dp)){Icon(icon,label,tint=Color.White)};Text(label,color=Color.White,style=MaterialTheme.typography.labelSmall)}
}
