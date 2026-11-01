package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RealtimeRecoveryResponse(
 val success: Boolean = false,
 val events: List<WsEventEnvelope> = emptyList(),
 val cursor: String? = null,
 val hasMore: Boolean = false
)
