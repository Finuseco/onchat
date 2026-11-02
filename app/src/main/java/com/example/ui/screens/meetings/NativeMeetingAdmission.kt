package com.example.ui.screens.meetings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.api.ApiClient
import kotlinx.coroutines.*
import org.json.JSONObject

/** Admission, payment consent and media connection without downloading a web application. */
@Composable
internal fun NativeMeetingAdmission(url:String,onClose:()->Unit,onConnected:(Boolean)->Unit){
 val scope=rememberCoroutineScope()
 var ticket by remember(url){mutableStateOf<SfuMeetingTicket?>(null)}
 var info by remember(url){mutableStateOf<JSONObject?>(null)}
 var error by remember(url){mutableStateOf<String?>(null)}
 var busy by remember{mutableStateOf(false)}
 var revision by remember{mutableIntStateOf(0)}
 var pin by remember(url){mutableStateOf("")}
 var ticketCode by remember(url){mutableStateOf("")}
 var consent by remember(url){mutableStateOf(false)}
 var video by remember(url){mutableStateOf(true)}
 val code=remember(url){runCatching{val uri=Uri.parse(url);val expected=Uri.parse(ApiClient.BASE_URL);require(uri.scheme=="https"&&uri.host==expected.host&&uri.port in listOf(-1,443));val parts=uri.pathSegments;require(parts.size==3&&parts[0]=="feedback"&&parts[1]=="meet");parts[2].also{require(it.matches(Regex("[A-Za-z0-9_-]{4,64}")))}}.getOrNull()}
 BackHandler(onBack=onClose)
 LaunchedEffect(code,revision){
  if(code==null){error="Lien de réunion OnChat invalide.";return@LaunchedEffect}
  busy=true;error=null;info=null;consent=false;pin=""
  try{info=NativeMeetingApi.request(code,"billing")}catch(e:Exception){if(e is CancellationException)throw e;error=e.message}finally{busy=false}
 }
 if(ticket!=null){NativeSfuMeetingScreen(ticket!!,onClose,onConnected);return}
 val quote=info?.optJSONObject("quote")
 val total=quote?.optDouble("totalDebit",0.0)?:0.0
 val requiresTicket=quote?.optBoolean("requiresTicket")==true
 val nativeReady=info?.optString("mediaTransport")=="livekit"
 Surface(Modifier.fillMaxSize(),color=Color(0xFF10091E),contentColor=Color.White){Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF170B2A),Color(0xFF381251)))).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onClose){Text("Retour")};Spacer(Modifier.weight(1f));Image(painterResource(R.drawable.onchat_logo),contentDescription="OnChat",modifier=Modifier.size(48.dp))}
  Spacer(Modifier.height(24.dp));Text(info?.optString("topic")?:"Réunion OnChat",style=MaterialTheme.typography.headlineMedium)
  Text(code?:"Lien invalide",modifier=Modifier.padding(12.dp),color=Color.LightGray)
  Card(Modifier.fillMaxWidth().padding(vertical=16.dp),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF2B193E))){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(if(video)Icons.Default.Videocam else Icons.Default.Headset,contentDescription=null,modifier=Modifier.size(64.dp));Text("Préparez votre participation",modifier=Modifier.padding(top=12.dp));Text("Le micro et la caméra restent sous votre contrôle.",style=MaterialTheme.typography.bodySmall)}}
  Row(verticalAlignment=Alignment.CenterVertically){Switch(checked=video,onCheckedChange={video=it});Text("Rejoindre avec la caméra")}
  if(busy){CircularProgressIndicator(Modifier.padding(16.dp));Text("Connexion au service OnChat…")}
  error?.let{Text(it,color=Color(0xFFFFCC80),modifier=Modifier.padding(12.dp))}
  if(info!=null&&!nativeReady)Text("Le serveur média des réunions natives n’est pas encore disponible. Le backend et le serveur SFU doivent être configurés avant de rejoindre. Aucun paiement ne sera demandé ici.",color=Color(0xFFFFCC80),modifier=Modifier.padding(12.dp))
  if(nativeReady&&quote!=null){
   if(requiresTicket)OutlinedTextField(value=ticketCode,onValueChange={ticketCode=it},label={Text("Code de votre billet")},singleLine=true,modifier=Modifier.fillMaxWidth())
   else if(total>0){
    Text("Participation : ${quote.optDouble("amount")} ${quote.optString("currency")}")
    Text("Frais : ${quote.optDouble("fee")} ${quote.optString("currency")}")
    Text("Total à débiter : $total ${quote.optString("currency")}",style=MaterialTheme.typography.titleMedium)
    Text("Solde disponible : ${quote.optDouble("balance")} ${quote.optString("currency")}")
    if(!quote.optBoolean("sufficientBalance"))Text("Solde insuffisant. Approvisionnez votre portefeuille.",color=Color(0xFFFFCC80))
    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=consent,onCheckedChange={consent=it});Text("J’autorise ce débit pour rejoindre la réunion.",modifier=Modifier.weight(1f))}
    OutlinedTextField(value=pin,onValueChange={pin=it.filter(Char::isDigit).take(12)},label={Text("PIN du portefeuille")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true,modifier=Modifier.fillMaxWidth())
   }
   Button(enabled=!busy&&(!requiresTicket||ticketCode.isNotBlank())&&(total<=0||requiresTicket||(consent&&pin.isNotBlank()&&quote.optBoolean("sufficientBalance"))),modifier=Modifier.fillMaxWidth().padding(top=20.dp),onClick={scope.launch{
    busy=true;error=null
    try{
     val body=JSONObject().put("currency",quote.getString("currency"))
     if(requiresTicket)body.put("ticketCode",ticketCode.trim())
     else if(total>0)body.put("consent",consent).put("pin",pin).put("maxDebit",total)
     NativeMeetingApi.request(code!!,"join",body);pin=""
     val connection=NativeMeetingApi.request(code,"connection",JSONObject())
     if(connection.optString("transport")!="livekit")kotlin.error("Connexion média native indisponible. Réessayez après configuration du serveur.")
     ticket=SfuMeetingTicket.parse(connection.put("withVideo",video).toString())
    }catch(e:Exception){if(e is CancellationException)throw e;error=e.message}finally{busy=false}
   }}){Text(if(total>0&&!requiresTicket)"Confirmer et rejoindre" else "Rejoindre la réunion")}
  }
  if(!busy)TextButton(onClick={revision++}){Text("Actualiser les informations")}
 }}
}
