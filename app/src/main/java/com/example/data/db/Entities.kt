package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id: Int = 1,
    val token: String,
    val userId: Long,
    val walletId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val bio: String? = null,
    val profession: String? = null,
    val activityDescription: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val nationality: String? = null,
    val language: String? = null,
    val birthDate: String? = null,
    val birthPlace: String? = null,
    val googleId: String? = null,
    val zoomId: String? = null,
    val verificationStatus: String? = null,
    val availabilityStatus: String = "available",
    val astroSign: String? = null,
    val astroElement: String? = null,
    val astroSymbol: String? = null,
    val astroNameFr: String? = null,
    val astroDescription: String? = null,
    val astroLuckyNumber: String? = null,
    val astroAscendant: String? = null,
    val bitcoinAddress: String? = null,
    val biometricEnabled: Boolean = false,
    val lastLoginTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val currency: String? = null,
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

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderWalletId: String? = null,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isMine: Boolean,
    val status: String = "SENT",
    val isEdited: Boolean = false,
    val type: String = "TEXT",
    val metadataJson: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val durationSeconds: Int? = null,
    val fileSizeBytes: Long? = null,
    val transferAmount: Double? = null,
    val transferCurrency: String? = null,
    val transferReference: String? = null
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val currency: String = "USD",
    val description: String? = null,
    val photoUrl: String? = null,
    val membersCount: Int = 1
)

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val userName: String,
    val userAvatar: String? = null,
    val type: String = "text",
    val content: String? = null,
    val backgroundColor: String? = null,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null,
    val visibility: String = "public",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val id: String = "",
    val callerWalletId: String? = null,
    val receiverWalletId: String? = null,
    val peerName: String,
    val peerAvatar: String? = null,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val isMissed: Boolean = false,
    val status: String = "COMPLETED",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
    lastMessage = lastMessage,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isGroup = isGroup,
    currency = currency,
    contactPhone = contactPhone,
    contactWalletId = contactWalletId,
    contactEmail = contactEmail,
    contactBio = contactBio,
    contactProfession = contactProfession,
    contactActivityDescription = contactActivityDescription,
    contactAddress = contactAddress,
    contactCity = contactCity,
    contactProvince = contactProvince,
    contactCountry = contactCountry,
    contactNationality = contactNationality,
    contactLanguage = contactLanguage,
    contactBirthDate = contactBirthDate,
    contactVerificationStatus = contactVerificationStatus,
    contactAvailabilityStatus = contactAvailabilityStatus,
    contactAstroSign = contactAstroSign,
    contactAstroSymbol = contactAstroSymbol,
    contactAstroNameFr = contactAstroNameFr,
    contactAstroElement = contactAstroElement,
    contactBitcoinAddress = contactBitcoinAddress,
    isMuted = isMuted
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderWalletId = senderWalletId,
    senderName = senderName,
    content = content,
    timestamp = timestamp,
    isMine = isMine,
    status = status,
    isEdited = isEdited,
    type = type,
    metadataJson = metadataJson,
    fileUrl = fileUrl,
    fileName = fileName,
    durationSeconds = durationSeconds,
    fileSizeBytes = fileSizeBytes,
    transferAmount = transferAmount,
    transferCurrency = transferCurrency,
    transferReference = transferReference
)

fun CallEntity.toDomain(): CallRecord = CallRecord(
    id = id,
    peerName = peerName,
    peerWalletId = if (isIncoming) callerWalletId else receiverWalletId,
    peerAvatar = peerAvatar,
    isVideo = isVideo,
    isIncoming = isIncoming,
    isMissed = isMissed,
    status = status,
    timestamp = timestamp,
    durationSeconds = durationSeconds
)
