package com.example.data.api

object VideoLinks {
 fun videoId(value:String?):Long?=runCatching {
  val uri=java.net.URI(value?:return null)
  val path=when {
   uri.scheme=="https"&&uri.host=="app.cashpay-all.com"&&uri.port in listOf(-1,443)&&uri.userInfo==null -> uri.path.removePrefix("/onchat/videos/").takeIf {uri.path.startsWith("/onchat/videos/")}
   uri.scheme=="onchat"&&uri.host=="videos"&&uri.userInfo==null -> uri.path.removePrefix("/")
   else -> null
  } ?: return null
  if(!path.matches(Regex("[0-9]+")))return null
  path.toLongOrNull()?.takeIf{it in 1..9007199254740991L}
 }.getOrNull()
}
