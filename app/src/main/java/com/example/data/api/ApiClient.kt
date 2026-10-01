package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://app.cashpay-all.com"

    var authToken: String? = null
    var activeWalletId: String = ""
    var currentUserId: Long? = null
    var onUnauthorized: (() -> Unit)? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
        
        authToken?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        
        if (activeWalletId.isNotBlank()) {
            requestBuilder.header("x-wallet-id", activeWalletId)
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        val path = original.url.encodedPath
        val isAuthEndpoint = path.contains("/auth/") || path.contains("/login")
        val hadToken = !authToken.isNullOrBlank()

        if (response.code == 401 && hadToken && !isAuthEndpoint) {
            onUnauthorized?.invoke()
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create())
        .build()

    val apiService: OnChatApiService = retrofit.create(OnChatApiService::class.java)
}
