package com.example.ui.screens.meetings

import com.example.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal object NativeMeetingApi {
 private val client=OkHttpClient.Builder().callTimeout(20,TimeUnit.SECONDS).followRedirects(false).build()
 suspend fun request(code:String,operation:String,body:JSONObject?=null):JSONObject=withContext(Dispatchers.IO){
  require(code.matches(Regex("[A-Za-z0-9_-]{4,64}")))
  require(operation in setOf("join","connection","participants","messages","raise-hand","grant-speech","mute-all","kick","end","leave","billing"))
  val token=ApiClient.authToken?:error("Session expirée")
  val builder=Request.Builder().url(ApiClient.BASE_URL+"api/v1/meetings/$code/$operation").header("Authorization","Bearer $token").header("Cookie","cashpay-token=$token")
  if(body!=null)builder.post(body.toString().toRequestBody("application/json".toMediaType()))
  client.newCall(builder.build()).execute().use{response->
   val result=runCatching{JSONObject(response.body?.string().orEmpty())}.getOrNull()
   if(!response.isSuccessful||result?.optBoolean("success")!=true)error(result?.optString("error")?.takeIf{it.isNotBlank()}?:"Opération non confirmée. Réessayez.")
   result
  }
 }
}
internal data class SfuMeetingTicket(val code:String,val url:String,val token:String,val isHost:Boolean,val canPublish:Boolean,val topic:String,val withVideo:Boolean){
 companion object{fun parse(raw:String):SfuMeetingTicket{
  val data=JSONObject(raw);val code=data.getString("meetCode");require(code.matches(Regex("[A-Za-z0-9_-]{4,64}")))
  val url=data.getString("url");require(java.net.URI(url).scheme=="wss")
  return SfuMeetingTicket(code,url,data.getString("token"),data.optBoolean("isHost"),data.optBoolean("canPublish"),data.optString("topic","Réunion OnChat"),data.optBoolean("withVideo",true))
 }}
}
