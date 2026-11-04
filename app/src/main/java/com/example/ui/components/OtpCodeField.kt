package com.example.ui.components

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/** One input preserves paste, selection and accessibility; six underlines render its digits. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun OtpCodeField(value: String, enabled: Boolean, smsEnabled: Boolean, onValueChange: (String) -> Unit) {
 val context=LocalContext.current
 val clipboard=LocalClipboardManager.current
 val focus=remember { FocusRequester() }
 val currentChange by rememberUpdatedState(onValueChange)
 val autofill=LocalAutofill.current
 val tree=LocalAutofillTree.current
 fun accept(text:String?) { OtpInput.codeFrom(text)?.let(currentChange) }
 val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
 var lastClipboard by remember {mutableStateOf<String?>(null)}
 DisposableEffect(lifecycle){
  val manager=context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
  fun pasteCopiedCode(){
   if(!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))return
   val text=manager.primaryClip?.takeIf {it.itemCount>0}?.getItemAt(0)?.coerceToText(context)?.toString()?.trim() ?: return
   if(text==lastClipboard)return
   lastClipboard=text
   if(text.matches(Regex("[0-9]{6}")) || text.contains("onchat",true) || text.contains("cashpay",true)) accept(text)
  }
  val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME)pasteCopiedCode()}
  val listener=android.content.ClipboardManager.OnPrimaryClipChangedListener{pasteCopiedCode()}
  lifecycle.addObserver(observer);manager.addPrimaryClipChangedListener(listener)
  onDispose{lifecycle.removeObserver(observer);manager.removePrimaryClipChangedListener(listener)}
 }
 val node=remember { AutofillNode(autofillTypes=listOf(AutofillType.SmsOtpCode),onFill={ accept(it) }) }
 DisposableEffect(node){tree+=node;onDispose {tree.children.remove(node.id)}}
 val consent=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if(result.resultCode==Activity.RESULT_OK)accept(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))
 }
 DisposableEffect(smsEnabled){
  if(!smsEnabled)onDispose {} else {
   val receiver=object:BroadcastReceiver(){override fun onReceive(ctx:Context,intent:Intent){
    if(intent.action!=SmsRetriever.SMS_RETRIEVED_ACTION)return
    @Suppress("DEPRECATION") val status=intent.getParcelableExtra<Status>(SmsRetriever.EXTRA_STATUS)
    if(status?.statusCode==CommonStatusCodes.SUCCESS){
     @Suppress("DEPRECATION") val request=intent.getParcelableExtra<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
     if(request!=null)runCatching {consent.launch(request)}
    }
   }}
   ContextCompat.registerReceiver(context,receiver,android.content.IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),SmsRetriever.SEND_PERMISSION,null,ContextCompat.RECEIVER_EXPORTED)
   // User Consent accepts existing provider messages without reading the inbox or requiring SMS permission.
   SmsRetriever.getClient(context).startSmsUserConsent(null).addOnFailureListener { /* Paste and manual input remain available. */ }
   onDispose {runCatching {context.unregisterReceiver(receiver)}}
  }
 }
 LaunchedEffect(Unit){focus.requestFocus()}
 Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()){
  BasicTextField(value=value,onValueChange={next->if(next.all {it in '0'..'9'}&&next.length<=6)currentChange(next) else OtpInput.codeFrom(next)?.let(currentChange)},enabled=enabled,singleLine=true,
   keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),textStyle=TextStyle(color=Color.Transparent,fontSize=1.sp),
   modifier=Modifier.fillMaxWidth().focusRequester(focus).semantics{contentDescription="Code de vérification à six chiffres"}.onGloballyPositioned{node.boundingBox=it.boundsInWindow()}.onFocusChanged{if(it.isFocused)autofill?.requestAutofillForNode(node) else autofill?.cancelAutofillForNode(node)},
   decorationBox={inner->Box{
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
     repeat(6){index->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
      Text(value.getOrNull(index)?.toString() ?: " ",fontSize=30.sp,color=Color.White,modifier=Modifier.padding(vertical=8.dp))
      Box(Modifier.fillMaxWidth().height(2.dp).background(if(index==value.length&&enabled)Color(0xFF38BDF8) else Color(0xFF64748B)))
     }}
    }
    Box(Modifier.size(1.dp)){inner()}
   }})
  if(!enabled)CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp),strokeWidth=2.dp)
  else TextButton(onClick={accept(clipboard.getText()?.text)}){Text("Coller le code")}
 }
}

object OtpInput {
 /** Reject ambiguous messages rather than selecting a tracking number or a second code. */
 fun codeFrom(text:String?):String? {
  if(text==null)return null
  val matches=Regex("(?<![0-9])[0-9]{6}(?![0-9])").findAll(text).map{it.value}.toList()
  return matches.singleOrNull()
 }
}
