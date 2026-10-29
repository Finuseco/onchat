package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Never send account credentials to a media URL controlled by another host. */
object AuthenticatedMedia {
    fun headers(url: String): Map<String, String> {
        val parsed = url.toHttpUrlOrNull() ?: return emptyMap()
        val base = ApiClient.BASE_URL.toHttpUrlOrNull() ?: return emptyMap()
        if (parsed.scheme != "https" || parsed.host != base.host || parsed.port != base.port) return emptyMap()
        val token = ApiClient.authToken?.takeIf { it.isNotBlank() } ?: return emptyMap()
        return mapOf("Authorization" to "Bearer $token")
    }

    val client: OkHttpClient = OkHttpClient.Builder().addNetworkInterceptor { chain ->
        val request = chain.request().newBuilder().removeHeader("Authorization").removeHeader("Cookie")
        headers(chain.request().url.toString()).forEach { (name, value) -> request.header(name, value) }
        chain.proceed(request.build())
    }.build()
}
