package com.example.ui.screens.chats

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.*
import kotlin.coroutines.resume

@Composable
fun CurrentLocationAttachment(onLocation:(Location)->Unit){
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var position by remember{mutableStateOf<Location?>(null)}
 var busy by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 fun locate(){
  if(busy)return
  scope.launch{
   busy=true;error=null;position=null
   try{
    val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    val provider=when {fine&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER)->LocationManager.GPS_PROVIDER;manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)->LocationManager.NETWORK_PROVIDER;else->kotlin.error("Activez la localisation dans les paramètres du téléphone.")}
    val fix=withTimeout(20000){suspendCancellableCoroutine<Location?>{continuation->
     val cancellation=androidx.core.os.CancellationSignal()
     continuation.invokeOnCancellation{cancellation.cancel()}
     LocationManagerCompat.getCurrentLocation(manager,provider,cancellation,ContextCompat.getMainExecutor(context)){value->if(continuation.isActive)continuation.resume(value)}
    }} ?: kotlin.error("Position indisponible. Placez-vous dans une zone dégagée et réessayez.")
    val age=(android.os.SystemClock.elapsedRealtimeNanos()-fix.elapsedRealtimeNanos)/1000000
    if(age !in 0..120000)kotlin.error("La position reçue est trop ancienne. Réessayez.")
    position=fix
   }catch(e:TimeoutCancellationException){error="Localisation trop longue. Vérifiez le GPS et réessayez."}
   catch(e:Exception){if(e is CancellationException)throw e;error=e.message?:"Position indisponible"}
   finally{busy=false}
  }
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->if(result.values.any{it})locate() else error="Autorisez la localisation pour partager votre position réelle."}
 Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Partager ma position",style=MaterialTheme.typography.titleLarge)
  Text("Votre position est envoyée une seule fois. Aucun suivi continu n’est activé.")
  if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
  if(busy)CircularProgressIndicator()
  position?.let{fix->Text("${fix.latitude}, ${fix.longitude}\nPrécision : ${fix.accuracy.toInt()} m");Button(onClick={onLocation(fix)}){Text("Envoyer cette position")}}
  OutlinedButton(enabled=!busy,onClick={if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)locate() else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))}){Text("Obtenir ma position actuelle")}
 }
}
