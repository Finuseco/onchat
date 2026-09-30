package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateMessageRequest(
    @Json(name = "content") val content: String
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String = "1",
    val senderWalletId: String? = null,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean = false,
    val status: String = "SENT", // "SENT", "DELIVERED", "READ", "SENDING"
    val isEdited: Boolean = false,
    val type: String = "TEXT", // "TEXT", "AUDIO", "IMAGE", "STICKER", "LOCATION", "TRANSFER_RECEIPT"
    val metadataJson: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val durationSeconds: Int? = null,
    val fileSizeBytes: Long? = null,
    val transferAmount: Double? = null,
    val transferCurrency: String? = null,
    val transferReference: String? = null
)

data class Conversation(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val currency: String? = null,
    val isOnline: Boolean = false,
    val contactPhone: String? = null,
    val contactWalletId: String? = null,
    val contactEmail: String? = null,
    val contactBio: String? = null,
    val contactProfession: String? = null,
    val contactActivityDescription: String? = null,
    val contactAddress: String? = null,
    val contactCity: String? = null,
    val contactProvince: String? = null,
    val contactCountry: String? = null,
    val contactNationality: String? = null,
    val contactLanguage: String? = null,
    val contactBirthDate: String? = null,
    val contactVerificationStatus: String? = null,
    val contactAvailabilityStatus: String = "available",
    val contactAstroSign: String? = null,
    val contactAstroSymbol: String? = null,
    val contactAstroNameFr: String? = null,
    val contactAstroElement: String? = null,
    val contactBitcoinAddress: String? = null,
    val isMuted: Boolean = false
)
