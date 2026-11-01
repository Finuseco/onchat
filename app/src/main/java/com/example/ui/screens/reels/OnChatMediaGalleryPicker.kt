package com.example.ui.screens.reels

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class GalleryMedia(val uri:Uri,val kind:String,val name:String,val added:Long,val folder:String)

/** Recent local media, with explicit type filters; system picker remains available on denied access. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnChatMediaGalleryPicker(onDismiss:()->Unit,onMediaSelected:(String,String)->Unit,audioOnly:Boolean=false,initialFilter:String="Tous"){
 val context=LocalContext.current
 var filter by remember {mutableStateOf(if(audioOnly)"Audios" else initialFilter)}
 var bucket by remember {mutableStateOf<String?>(null)}
 var folderMenu by remember {mutableStateOf(false)}
 var revision by remember {mutableIntStateOf(0)}
 var items by remember {mutableStateOf<List<GalleryMedia>>(emptyList())}
 var loading by remember {mutableStateOf(true)}
 val loader=remember {ImageLoader.Builder(context.applicationContext).components {add(VideoFrameDecoder.Factory())}.build()}
 DisposableEffect(loader){onDispose{loader.shutdown()}}
 val access=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){revision++}
 val visual=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null){val kind=if(context.contentResolver.getType(uri)?.startsWith("video/")==true)"video" else "image";onMediaSelected(uri.toString(),kind)}}
 val audio=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)onMediaSelected(uri.toString(),"audio")}
 fun browse(){if(audioOnly)audio.launch(arrayOf("audio/*")) else visual.launch(PickVisualMediaRequest(when(filter){"Photos"->ActivityResultContracts.PickVisualMedia.ImageOnly;"Vidéos"->ActivityResultContracts.PickVisualMedia.VideoOnly;else->ActivityResultContracts.PickVisualMedia.ImageAndVideo}))}
 fun permissions():Array<String> = if(Build.VERSION.SDK_INT>=33){if(audioOnly)arrayOf(Manifest.permission.READ_MEDIA_AUDIO) else if(Build.VERSION.SDK_INT>=34)arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) else arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO)} else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
 LaunchedEffect(revision){
  loading=true
  val result=withContext(Dispatchers.IO){
   val found=mutableListOf<GalleryMedia>()
   fun query(collection:Uri,kind:String,permission:String){
    val limitedVisual = kind != "audio" && Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(context,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)==PackageManager.PERMISSION_GRANTED
    if(ContextCompat.checkSelfPermission(context,permission)!=PackageManager.PERMISSION_GRANTED && !limitedVisual)return
    val folderColumn=if(kind=="audio")MediaStore.Audio.Media.ALBUM else MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    val projection=arrayOf(MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.DATE_ADDED,folderColumn)
    try{context.contentResolver.query(collection,projection,null,null,"${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use{cursor->
     val id=cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);val name=cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);val date=cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
     while(cursor.moveToNext()&&found.count{it.kind==kind}<300)found+=GalleryMedia(ContentUris.withAppendedId(collection,cursor.getLong(id)),kind,cursor.getString(name)?:"Média",cursor.getLong(date),cursor.getString(cursor.getColumnIndexOrThrow(folderColumn))?:"Appareil")
    }}catch(_:SecurityException){/* Access may be revoked while the picker is open. */}
   }
   val old=Manifest.permission.READ_EXTERNAL_STORAGE
   if(audioOnly)query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,"audio",if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_AUDIO else old)
   else {query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,"video",if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_VIDEO else old);query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image",if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_IMAGES else old)}
   found.sortedByDescending{it.added}
  }
  items=result;loading=false
 }
 val filtered=items.filter{bucket==null||it.folder==bucket}.filter{filter=="Tous"||(filter=="Vidéos"&&it.kind=="video")||(filter=="Photos"&&it.kind=="image")||(filter=="Audios"&&it.kind=="audio")}
 Scaffold(containerColor=CashPayNavyDark,topBar={TopAppBar(title={Text(if(audioOnly)"Choisir un son" else "Médias récents",color=Color.White)},navigationIcon={IconButton(onClick=onDismiss){Icon(Icons.Default.Close,"Fermer",tint=Color.White)}},actions={IconButton(onClick=::browse){Icon(Icons.Default.FolderOpen,"Sélecteur de l’appareil",tint=OnChatCyan)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=CashPayNavySurface))}){padding->
  Column(Modifier.fillMaxSize().padding(padding)){
   if(!audioOnly)Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Tous","Vidéos","Photos").forEach{kind->FilterChip(selected=filter==kind,onClick={filter=kind},label={Text(kind)})}}
   Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={access.launch(permissions())}){Text("Autoriser les médias récents")};TextButton(onClick=::browse){Text("Parcourir")}}
   Box(Modifier.padding(horizontal=12.dp)){
    TextButton(onClick={folderMenu=true}){Icon(Icons.Default.Folder,null);Text(bucket?:"Tous les dossiers")}
    DropdownMenu(expanded=folderMenu,onDismissRequest={folderMenu=false}){
     DropdownMenuItem(text={Text("Tous les dossiers")},onClick={bucket=null;folderMenu=false})
     items.map{it.folder}.distinct().sorted().forEach{folder->DropdownMenuItem(text={Text(folder)},onClick={bucket=folder;folderMenu=false})}
    }
   }
   if(loading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
   else if(filtered.isEmpty())Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text("Aucun média accessible. Autorisez l’accès ou utilisez le sélecteur de l’appareil.",color=Color.White);Button(onClick=::browse){Text("Choisir un fichier")}}
   else if(audioOnly)LazyColumn{items(filtered,key={it.uri.toString()}){item->ListItem(headlineContent={Text(item.name,maxLines=2,overflow=TextOverflow.Ellipsis)},leadingContent={Icon(Icons.Default.MusicNote,null)},modifier=Modifier.clickable{onMediaSelected(item.uri.toString(),"audio")})}}
   else LazyVerticalGrid(columns=GridCells.Fixed(3),contentPadding=PaddingValues(2.dp),horizontalArrangement=Arrangement.spacedBy(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
    items(filtered,key={it.uri.toString()}){item->Box(Modifier.aspectRatio(1f).background(CashPayNavySurface).clickable{onMediaSelected(item.uri.toString(),item.kind)}){
     AsyncImage(model=item.uri,imageLoader=loader,contentDescription=item.name,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
     if(item.kind=="video")Icon(Icons.Default.PlayCircle,"Vidéo",tint=Color.White,modifier=Modifier.align(Alignment.BottomEnd).padding(6.dp).size(24.dp))
    }}
   }
  }
 }
}
