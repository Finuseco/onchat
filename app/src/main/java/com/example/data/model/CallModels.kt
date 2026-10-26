package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

data class CallRecord(
    val id: String = "0",
    val peerName: String,
    val peerWalletId: String? = null,
    val peerAvatar: String? = null,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val isMissed: Boolean = false,
    val status: String = "COMPLETED",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

data class MeetingRoom(
    val roomId: String,
    val title: String,
    val isVideo: Boolean,
    val participants: List<String>,
    val isHost: Boolean = true,
    val connectedTime: Long = 0
)

// --- Calls API DTOs (https://app.cashpay-all.com) ---

@JsonClass(generateAdapter = true)
data class InitiateCallRequest(
    @Json(name = "callerWalletId") val callerWalletId: String,
    @Json(name = "receiverWalletId") val receiverWalletId: String,
    @Json(name = "isVideo") val isVideo: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RespondCallRequest(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "status") val status: String // "ACCEPTED", "REJECTED", "BUSY"
)

@JsonClass(generateAdapter = true)
data class EndCallRequest(
    @Json(name = "walletId") val walletId: String
)

@JsonClass(generateAdapter = true)
data class ApiCallDto(
    @Json(name = "id") val id: String,
    @Json(name = "caller_id") val callerId: String? = null,
    @Json(name = "receiver_id") val receiverId: String? = null,
    @Json(name = "is_video") val isVideo: Boolean = false,
    @Json(name = "status") val status: String? = "COMPLETED",
    @Json(name = "duration_seconds") val durationSeconds: Int = 0,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "ended_at") val endedAt: String? = null,
    @Json(name = "caller_wallet_id") val callerWalletId: String? = null,
    @Json(name = "caller_name") val callerName: String? = null,
    @Json(name = "caller_photo") val callerPhoto: String? = null,
    @Json(name = "receiver_wallet_id") val receiverWalletId: String? = null,
    @Json(name = "receiver_name") val receiverName: String? = null,
    @Json(name = "receiver_photo") val receiverPhoto: String? = null
)

@JsonClass(generateAdapter = true)
data class CallActionResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "call") val call: ApiCallDto? = null
)

@JsonClass(generateAdapter = true)
data class CallsHistoryResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "calls") val calls: List<ApiCallDto> = emptyList()
)

// --- Live Stage DTOs ---

@JsonClass(generateAdapter = true)
data class CreateLiveRoomRequest(
    @Json(name = "hostWalletId") val hostWalletId: String,
    @Json(name = "title") val title: String,
    @Json(name = "roomType") val roomType: String = "VIDEO_STAGE",
    @Json(name = "maxSpectators") val maxSpectators: Int = 100
)

@JsonClass(generateAdapter = true)
data class ApiLiveRoomDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "room_type") val roomType: String = "VIDEO_STAGE",
    @Json(name = "host_wallet_id") val hostWalletId: String,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "max_spectators") val maxSpectators: Int = 100,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "ended_at") val endedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateLiveRoomResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "room") val room: ApiLiveRoomDto? = null
)

@JsonClass(generateAdapter = true)
data class LiveRoomUserActionRequest(
    @Json(name = "walletId") val walletId: String
)

@JsonClass(generateAdapter = true)
data class LiveSpeakerActionRequest(
    @Json(name = "hostWalletId") val hostWalletId: String,
    @Json(name = "userWalletId") val userWalletId: String
)

@JsonClass(generateAdapter = true)
data class EndLiveRoomRequest(
    @Json(name = "hostWalletId") val hostWalletId: String
)

@JsonClass(generateAdapter = true)
data class LiveRoomActionResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "room") val room: ApiLiveRoomDto? = null
)

data class StageUser(
    val walletId: String,
    val name: String,
    val avatarUrl: String? = null,
    val hasRequestedToSpeak: Boolean = false,
    val isSpeakerOnStage: Boolean = false
)

data class StageComment(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val senderWalletId: String,
    val avatarUrl: String? = null,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveStageRoom(
    val id: String,
    val title: String,
    val hostName: String,
    val hostWalletId: String,
    val hostAvatar: String? = null,
    val isPaid: Boolean = false,
    val entryFee: Double = 0.0,
    val currency: String = "USD",
    val spectatorCount: Int = 1,
    val likesCount: Int = 0,
    val isHost: Boolean = true,
    val isJoined: Boolean = true,
    val speakerOnStageName: String? = null,
    val speakerOnStageWalletId: String? = null,
    val speakerOnStageAvatar: String? = null,
    val hasRaisedHand: Boolean = false,
    val raisedHandRequests: List<StageUser> = emptyList(),
    val comments: List<StageComment> = emptyList(),
    val isCameraLandscape: Boolean = false,
    val isCameraFront: Boolean = true,
    val isMicMuted: Boolean = false,
    val isVideoMuted: Boolean = false,
    val isAudioOnly: Boolean = false,
    val activeSpeakers: List<StageUser> = emptyList(),
    val startedAt: Long = System.currentTimeMillis()
)

data class IncomingCallNotificationState(
    val callId: String,
    val callerName: String,
    val callerWalletId: String,
    val callerAvatar: String? = null,
    val isVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

