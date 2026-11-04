package com.example.ui.screens.reels

import com.example.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal object CreatorMediaApi {
 private val client=OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS).followRedirects(false).build()
 suspend fun get(id:Long,section:String="original",offset:Int=0):JSONObject=withContext(Dispatchers.IO){
  require(id>0&&section in listOf("original","used")&&offset>=0)
  val request=Request.Builder().url("${ApiClient.BASE_URL}api/v1/media/creators/$id?sounds=$section&offset=$offset")
  ApiClient.authToken?.let{request.header("Authorization","Bearer $it").header("Cookie","cashpay-token=$it")}
  client.newCall(request.build()).execute().use{response->
   val data=runCatching{JSONObject(response.body?.string().orEmpty())}.getOrNull()
   if(!response.isSuccessful||data?.optBoolean("success")!=true)error(data?.optString("error")?.takeIf{it.isNotBlank()}?:"Profil média indisponible (HTTP ${response.code}).")
   data
  }
 }
}
