package com.example.data.api

import android.util.Log
import com.example.data.model.AvailableChannelsAdapter
import com.squareup.moshi.Moshi
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://app.cashpay-all.com/"

    var authToken: String? = null
    var activeWalletId: String = ""
    var currentUserId: Long? = null
    var onUnauthorized: (() -> Unit)? = null

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    fun clearSession() {
        authToken = null
        activeWalletId = ""
        currentUserId = null
        cookieStore.clear()
    }

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                list.removeAll { existing -> cookies.any { it.name == existing.name } }
                list.addAll(cookies)
            }
            cookies.forEach { cookie ->
                if (cookie.name == "cashpay-token" || cookie.name == "token" || cookie.name == "jwt") {
                    if (authToken.isNullOrBlank()) {
                        authToken = cookie.value
                        SessionTokenStore.saveToken(cookie.value)
                    }
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = (cookieStore[url.host] ?: emptyList()).toMutableList()
            authToken?.let { tok ->
                if (list.none { it.name == "cashpay-token" }) {
                    try {
                        list.add(
                            Cookie.Builder()
                                .domain(url.host)
                                .path("/")
                                .name("cashpay-token")
                                .value(tok)
                                .build()
                        )
                    } catch (_: Exception) {}
                }
            }
            return list
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
        
        authToken?.let { tok ->
            requestBuilder.header("Authorization", "Bearer $tok")
            requestBuilder.header("Cookie", "cashpay-token=$tok; token=$tok")
        }
        
        if (activeWalletId.isNotBlank()) {
            requestBuilder.header("x-wallet-id", activeWalletId)
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        val path = original.url.encodedPath
        if (response.code == 401) {
            Log.w("ApiClient", "HTTP 401 on endpoint: $path")
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .add(AvailableChannelsAdapter())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: OnChatApiService = retrofit.create(OnChatApiService::class.java)
}
