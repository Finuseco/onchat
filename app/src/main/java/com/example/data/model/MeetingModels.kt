package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- 3.1 Créer une réunion (POST /api/v1/meetings/create) ---

@JsonClass(generateAdapter = true)
data class CreateMeetingRequest(
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = "video", // "audio" ou "video"
    @Json(name = "scheduledAt") val scheduledAt: String? = null,
    @Json(name = "durationMin") val durationMin: Int? = 60,
    @Json(name = "priceAmount") val priceAmount: Double? = 0.0,
    @Json(name = "priceCurrency") val priceCurrency: String? = "USD"
)

@JsonClass(generateAdapter = true)
data class MeetingBillingDto(
    @Json(name = "priceAmount") val priceAmount: Double = 0.0,
    @Json(name = "priceCurrency") val priceCurrency: String = "USD",
    @Json(name = "isPaid") val isPaid: Boolean = false,
    @Json(name = "hostUserId") val hostUserId: Long? = null
)

@JsonClass(generateAdapter = true)
data class CreateMeetingResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "meetCode") val meetCode: String? = null,
    @Json(name = "joinUrl") val joinUrl: String? = null,
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "scheduledAt") val scheduledAt: String? = null,
    @Json(name = "durationMin") val durationMin: Int? = null,
    @Json(name = "billing") val billing: MeetingBillingDto? = null,
    @Json(name = "error") val error: String? = null
)

// --- 3.2 Lister les réunions récentes (GET /api/v1/meetings/list) ---

@JsonClass(generateAdapter = true)
data class MeetingItemDto(
    @Json(name = "code") val code: String,
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = "video",
    @Json(name = "status") val status: String? = "active", // "active", "ended"
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MeetingsListResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "meetings") val meetings: List<MeetingItemDto> = emptyList(),
    @Json(name = "error") val error: String? = null
)

// --- 3.3 Obtenir le statut d'une réunion (GET /api/v1/meetings/{meetCode}/status) ---

@JsonClass(generateAdapter = true)
data class MeetingStatusResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "meetCode") val meetCode: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "participants") val participants: Int = 0,
    @Json(name = "startedAt") val startedAt: String? = null,
    @Json(name = "error") val error: String? = null
)

// --- 3.4 Rejoindre une réunion (POST /api/v1/meetings/{meetCode}/join) ---

@JsonClass(generateAdapter = true)
data class JoinMeetingResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "meetCode") val meetCode: String? = null,
    @Json(name = "joinUrl") val joinUrl: String? = null,
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "participants") val participants: Int = 0,
    @Json(name = "error") val error: String? = null
)

// --- 3.5 Consulter les participants (GET /api/v1/meetings/{meetCode}/participants) ---

@JsonClass(generateAdapter = true)
data class MeetingParticipantDto(
    @Json(name = "userId") val userId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "isHost") val isHost: Boolean = false,
    @Json(name = "role") val role: String = "viewer", // "host", "speaker", "viewer"
    @Json(name = "raiseHand") val raiseHand: Boolean = false,
    @Json(name = "canSpeak") val canSpeak: Boolean = true,
    @Json(name = "joinedAt") val joinedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MeetingParticipantsResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "meetingStatus") val meetingStatus: String? = "active",
    @Json(name = "hostUserId") val hostUserId: Long? = null,
    @Json(name = "participants") val participants: List<MeetingParticipantDto> = emptyList(),
    @Json(name = "error") val error: String? = null
)

// --- 3.8 Lever ou baisser la main (POST /api/v1/meetings/{meetCode}/raise-hand) ---

@JsonClass(generateAdapter = true)
data class RaiseHandRequest(
    @Json(name = "raised") val raised: Boolean = true
)

@JsonClass(generateAdapter = true)
data class RaiseHandResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "raised") val raised: Boolean = true,
    @Json(name = "error") val error: String? = null
)

// --- 3.9 Accorder ou retirer le droit de parole (POST /api/v1/meetings/{meetCode}/grant-speech) ---

@JsonClass(generateAdapter = true)
data class GrantSpeechRequest(
    @Json(name = "targetUserId") val targetUserId: Long,
    @Json(name = "canSpeak") val canSpeak: Boolean
)

@JsonClass(generateAdapter = true)
data class GrantSpeechResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "targetUserId") val targetUserId: Long? = null,
    @Json(name = "canSpeak") val canSpeak: Boolean? = null,
    @Json(name = "role") val role: String? = null, // "speaker", "viewer"
    @Json(name = "error") val error: String? = null
)

// --- 3.10 & 3.11 Facturation d'une réunion (GET & POST /api/v1/meetings/{meetCode}/billing) ---

@JsonClass(generateAdapter = true)
data class MeetingBillingResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "meetCode") val meetCode: String? = null,
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "billing") val billing: MeetingBillingDto? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateMeetingBillingRequest(
    @Json(name = "priceAmount") val priceAmount: Double = 0.0,
    @Json(name = "priceCurrency") val priceCurrency: String = "USD"
)

@JsonClass(generateAdapter = true)
data class UpdateMeetingBillingResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "priceAmount") val priceAmount: Double? = null,
    @Json(name = "priceCurrency") val priceCurrency: String? = null,
    @Json(name = "error") val error: String? = null
)

// --- Simple Action Response for leave & end ---

@JsonClass(generateAdapter = true)
data class MeetingActionSimpleResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "error") val error: String? = null
)

// --- Session active côté client Android ---

data class ActiveMeetingSession(
    val meetCode: String,
    val topic: String,
    val type: String = "video", // "audio", "video"
    val joinUrl: String,
    val isHost: Boolean = false,
    val status: String = "active", // "active", "ended"
    val canSpeak: Boolean = true,
    val isHandRaised: Boolean = false,
    val isMicMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isCameraFront: Boolean = true,
    val participants: List<MeetingParticipantDto> = emptyList(),
    val billing: MeetingBillingDto? = null,
    val joinedAt: Long = System.currentTimeMillis()
)
