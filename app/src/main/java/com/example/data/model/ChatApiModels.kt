package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContactDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "profilePhoto") val profilePhoto: String? = null,
    @Json(name = "profilePhotoUrl") val profilePhotoUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "profession") val profession: String? = null,
    @Json(name = "activityDescription") val activityDescription: String? = null,
    @Json(name = "activity_description") val activity_description: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "province") val province: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "nationality") val nationality: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "birthDate") val birthDate: String? = null,
    @Json(name = "availabilityStatus") val availabilityStatus: String? = "available",
    @Json(name = "verificationStatus") val verificationStatus: String? = null,
    @Json(name = "bitcoinAddress") val bitcoinAddress: String? = null,
    @Json(name = "astro") val astro: AstroInfo? = null
) {
    val displayName: String
        get() = fullName?.ifBlank { null } ?: name?.ifBlank { null } ?: phone ?: "Contact OnChat"

    val displayAvatar: String?
        get() = profilePhotoUrl?.ifBlank { null } ?: profilePhoto?.ifBlank { null }

    val resolvedActivity: String?
        get() = activityDescription?.ifBlank { null } ?: activity_description?.ifBlank { null } ?: bio?.ifBlank { null }
}

@JsonClass(generateAdapter = true)
data class ApiMessage(
    @Json(name = "id") val id: String,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "senderId") val senderId: Long? = null,
    @Json(name = "senderName") val senderName: String? = null,
    @Json(name = "senderAvatar") val senderAvatar: String? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "type") val type: String = "text", // text, audio, image, doc, product, in_chat_transfer, system
    @Json(name = "metadata") val metadata: Map<String, String>? = null,
    @Json(name = "status") val status: String? = "delivered", // sent, delivered, read
    @Json(name = "isRead") val isRead: Boolean? = false,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "imageId") val imageId: String? = null,
    @Json(name = "fileUrl") val fileUrl: String? = null,
    @Json(name = "fileName") val fileName: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiConversation(
    @Json(name = "id") val id: String,
    @Json(name = "contact") val contact: ContactDto? = null,
    @Json(name = "lastMessage") val lastMessage: ApiMessage? = null,
    @Json(name = "unreadCount") val unreadCount: Int = 0,
    @Json(name = "lastMessageTimestamp") val lastMessageTimestamp: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "isGroup") val isGroup: Boolean? = false,
    @Json(name = "groupName") val groupName: String? = null,
    @Json(name = "currency") val currency: String? = "USD"
)

@JsonClass(generateAdapter = true)
data class ConversationsListResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "conversations") val conversations: List<ApiConversation> = emptyList(),
    @Json(name = "count") val count: Int = 0,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationDetailDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "contact") val contact: ContactDto? = null,
    @Json(name = "messages") val messages: List<ApiMessage> = emptyList(),
    @Json(name = "isGroup") val isGroup: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ConversationDetailResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "conversation") val conversation: ConversationDetailDto? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "contact") val contact: ContactDto? = null,
    @Json(name = "messages") val messages: List<ApiMessage> = emptyList(),
    @Json(name = "isGroup") val isGroup: Boolean? = false,
    @Json(name = "error") val error: String? = null
) {
    val resolvedId: String?
        get() = conversation?.id ?: id

    val resolvedContact: ContactDto?
        get() = conversation?.contact ?: contact

    val resolvedMessages: List<ApiMessage>
        get() = conversation?.messages?.takeIf { it.isNotEmpty() } ?: messages
}

@JsonClass(generateAdapter = true)
data class SendMessageResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: ApiMessage? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class DeleteConversationResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "deleted") val deleted: Boolean = false,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "content") val content: String,
    @Json(name = "type") val type: String = "text",
    @Json(name = "metadata") val metadata: Map<String, String>? = null,
    @Json(name = "fileUrl") val fileUrl: String? = null,
    @Json(name = "fileName") val fileName: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateConversationRequest(
    @Json(name = "walletId") val walletId: String
)

@JsonClass(generateAdapter = true)
data class CreateConversationResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "conversation") val conversation: ApiConversation? = null,
    @Json(name = "error") val error: String? = null
) {
    val resolvedConversationId: String?
        get() = conversation?.id ?: conversationId
}

@JsonClass(generateAdapter = true)
data class TransferQuoteRequest(
    @Json(name = "amount") val amount: Double,
    @Json(name = "currency") val currency: String = "USD"
)

@JsonClass(generateAdapter = true)
data class TransferQuoteResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "totalDebit") val totalDebit: Double? = null,
    @Json(name = "fee") val fee: Double? = 0.0,
    @Json(name = "balance") val balance: Double? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "required") val required: Double? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class TransferExecuteRequest(
    @Json(name = "amount") val amount: Double,
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "pin") val pin: String
)

@JsonClass(generateAdapter = true)
data class TransferExecuteResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "transaction") val transaction: TransactionDto? = null,
    @Json(name = "transactionId") val transactionId: String? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "message") val message: ApiMessage? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "code") val code: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "birthDate") val birthDate: String? = null
)

@JsonClass(generateAdapter = true)
data class TransactionParticipantDto(
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "profilePhoto") val profilePhoto: String? = null
)

@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = "Completed",
    @Json(name = "direction") val direction: String? = null,
    @Json(name = "amount") val amount: Any? = null,
    @Json(name = "currency") val currency: Any? = null,
    @Json(name = "fee") val fee: Any? = null,
    @Json(name = "senderWalletId") val senderWalletId: String? = null,
    @Json(name = "receiverWalletId") val receiverWalletId: String? = null,
    @Json(name = "senderNewBalance") val senderNewBalance: Any? = null,
    @Json(name = "receiverNewBalance") val receiverNewBalance: Any? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "txHash") val txHash: String? = null,
    @Json(name = "sender") val sender: TransactionParticipantDto? = null,
    @Json(name = "receiver") val receiver: TransactionParticipantDto? = null,
    @Json(name = "otherUser") val otherUser: TransactionParticipantDto? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "metadata") val metadata: Map<String, Any?>? = null
) {
    val resolvedCurrency: String
        get() = when (val c = currency) {
            is String -> c
            is Map<*, *> -> (c["code"] ?: c["currency"] ?: c["symbol"] ?: "CDF").toString()
            else -> "CDF"
        }

    val resolvedAmount: Double
        get() = when (val a = amount) {
            is Number -> a.toDouble()
            is String -> a.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    val resolvedFee: Double
        get() = when (val f = fee) {
            is Number -> f.toDouble()
            is String -> f.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
}

@JsonClass(generateAdapter = true)
data class TransactionsResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "limit") val limit: Int? = null,
    @Json(name = "offset") val offset: Int? = null,
    @Json(name = "transactions") val transactions: List<TransactionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TransferRequest(
    @Json(name = "receiverWalletId") val receiverWalletId: String? = null,
    @Json(name = "recipientAddress") val recipientAddress: String? = null,
    @Json(name = "amount") val amount: Double,
    @Json(name = "currency") val currency: String,
    @Json(name = "pin") val pin: String? = null,
    @Json(name = "preview") val preview: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class TransferResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: Any? = null,
    @Json(name = "transaction") val transaction: TransactionDto? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "receiverWalletId") val receiverWalletId: String? = null,
    @Json(name = "senderWalletId") val senderWalletId: String? = null,
    @Json(name = "recipientAddress") val recipientAddress: String? = null,
    @Json(name = "amount") val amount: Any? = null,
    @Json(name = "currency") val currency: Any? = null,
    @Json(name = "fee") val fee: Any? = null,
    @Json(name = "totalDebit") val totalDebit: Any? = null,
    @Json(name = "txHash") val txHash: String? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "referenceCode") val referenceCode: String? = null,
    @Json(name = "totalDebited") val totalDebited: Any? = null
) {
    val isInsufficientBalance: Boolean
        get() = error?.contains("Solde insuffisant", ignoreCase = true) == true ||
                message?.contains("Solde disponible", ignoreCase = true) == true ||
                message?.contains("Manquant", ignoreCase = true) == true

    val resolvedAmount: Double
        get() = when (val a = amount ?: transaction?.amount) {
            is Number -> a.toDouble()
            is String -> a.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    val resolvedFee: Double
        get() = when (val f = fee ?: transaction?.fee) {
            is Number -> f.toDouble()
            is String -> f.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    val resolvedTotalDebit: Double
        get() = when (val t = totalDebited ?: totalDebit) {
            is Number -> t.toDouble()
            is String -> t.toDoubleOrNull() ?: (resolvedAmount + resolvedFee)
            else -> (resolvedAmount + resolvedFee)
        }

    val resolvedCurrency: String
        get() = when (val c = currency ?: transaction?.currency) {
            is String -> c
            is Map<*, *> -> (c["code"] ?: c["currency"] ?: c["symbol"] ?: "CDF").toString()
            else -> transaction?.resolvedCurrency ?: "CDF"
        }

    val resolvedReference: String
        get() = referenceCode ?: reference ?: transaction?.reference ?: txHash ?: ""

    val isTransferSuccess: Boolean
        get() = success && (transaction != null || resolvedReference.isNotBlank()) && !status.toString().equals("Failed", true)
}

// --- OnChat Official Reference API DTOs (https://app.cashpay-all.com) ---

@JsonClass(generateAdapter = true)
data class ChatsListResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "chats") val chats: List<ApiChatDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiChatDto(
    @Json(name = "id") val id: String,
    @Json(name = "is_group") val isGroup: Boolean? = false,
    @Json(name = "title") val title: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "last_message") val lastMessage: ApiChatMessageDto? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ChatMessagesResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "messages") val messages: List<ApiChatMessageDto> = emptyList(),
    @Json(name = "hasMore") val hasMore: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ApiChatMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "conversation_id") val conversationId: String? = null,
    @Json(name = "sender_id") val senderId: String? = null,
    @Json(name = "sender_wallet_id") val senderWalletId: String? = null,
    @Json(name = "sender_name") val senderName: String? = null,
    @Json(name = "sender_avatar") val senderAvatar: String? = null,
    @Json(name = "type") val type: String = "TEXT",
    @Json(name = "content") val content: String? = "",
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "mediaUrl") val mediaUrlCamel: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrlCamel: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "durationSeconds") val durationSecondsCamel: Int? = null,
    @Json(name = "waveform") val waveform: List<Float>? = null,
    @Json(name = "file_size_bytes") val fileSizeBytes: Long? = null,
    @Json(name = "fileSizeBytes") val fileSizeBytesCamel: Long? = null,
    @Json(name = "transfer_amount") val transferAmount: Double? = null,
    @Json(name = "transfer_currency") val transferCurrency: String? = null,
    @Json(name = "transfer_transaction_id") val transferTransactionId: String? = null,
    @Json(name = "transfer_status") val transferStatus: String? = null,
    @Json(name = "status") val status: String? = "SENT",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "createdAt") val createdAtCamel: String? = null
) {
    val resolvedMediaUrl: String? get() = mediaUrl?.ifBlank { null } ?: mediaUrlCamel?.ifBlank { null }
    val resolvedThumbnailUrl: String? get() = thumbnailUrl?.ifBlank { null } ?: thumbnailUrlCamel?.ifBlank { null }
    val resolvedDurationSeconds: Int? get() = durationSeconds ?: durationSecondsCamel
    val resolvedFileSizeBytes: Long? get() = fileSizeBytes ?: fileSizeBytesCamel
    val resolvedCreatedAt: String? get() = createdAt ?: createdAtCamel
}

@JsonClass(generateAdapter = true)
data class SendChatMessageRequest(
    @Json(name = "senderWalletId") val senderWalletId: String,
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "type") val type: String = "TEXT",
    @Json(name = "content") val content: String,
    @Json(name = "mediaUrl") val mediaUrl: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = null,
    @Json(name = "waveform") val waveform: List<Float>? = null,
    @Json(name = "fileSizeBytes") val fileSizeBytes: Long? = null
)

@JsonClass(generateAdapter = true)
data class SendChatMessageResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: ApiChatMessageDto? = null
)

@JsonClass(generateAdapter = true)
data class MarkMessageReadRequest(
    @Json(name = "walletId") val walletId: String
)

@JsonClass(generateAdapter = true)
data class MarkMessageReadResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class MediaUploadResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "mediaUrl") val mediaUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "fileUrl") val fileUrl: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "error") val error: String? = null
) {
    val resolvedUrl: String?
        get() = mediaUrl?.ifBlank { null }
            ?: url?.ifBlank { null }
            ?: fileUrl?.ifBlank { null }
            ?: path?.ifBlank { null }
}

// --- OnChat Real-Time WebSocket Event Models (wss://glob.cashpay-all.com/ws) ---

@JsonClass(generateAdapter = true)
data class WsEventEnvelope(
    @Json(name = "event") val event: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "conversation_id") val conversation_id: String? = null,
    @Json(name = "message") val message: ApiMessage? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "user_id") val user_id: Long? = null,
    @Json(name = "userName") val userName: String? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "wallet_id") val wallet_id: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "callId") val callId: String? = null,
    @Json(name = "call_id") val call_id: String? = null,
    @Json(name = "callerId") val callerId: Long? = null,
    @Json(name = "caller_id") val caller_id: Long? = null,
    @Json(name = "callerName") val callerName: String? = null,
    @Json(name = "callerAvatar") val callerAvatar: String? = null,
    @Json(name = "callerWalletId") val callerWalletId: String? = null,
    @Json(name = "callType") val callType: String? = "audio",
    @Json(name = "data") val data: Map<String, Any?>? = null,
    @Json(name = "timestamp") val timestamp: String? = null
) {
    val resolvedEvent: String
        get() = (event ?: type ?: "").lowercase()

    val resolvedConversationId: String?
        get() = conversationId ?: conversation_id ?: (data?.get("conversationId")?.toString()) ?: (data?.get("conversation_id")?.toString())

    val resolvedUserId: Long?
        get() = userId ?: user_id ?: (data?.get("userId") as? Number)?.toLong() ?: data?.get("userId")?.toString()?.toLongOrNull()

    val resolvedWalletId: String?
        get() = walletId ?: wallet_id ?: (data?.get("walletId")?.toString())

    val resolvedCallId: String?
        get() = callId ?: call_id ?: (data?.get("callId")?.toString())
}

@JsonClass(generateAdapter = true)
data class WsOutgoingTyping(
    @Json(name = "event") val event: String = "chat.typing",
    @Json(name = "conversationId") val conversationId: String
)

@JsonClass(generateAdapter = true)
data class WsOutgoingRead(
    @Json(name = "event") val event: String = "chat.read",
    @Json(name = "conversationId") val conversationId: String
)

@JsonClass(generateAdapter = true)
data class WsOutgoingCallSignaling(
    @Json(name = "event") val event: String,
    @Json(name = "callId") val callId: String,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "data") val data: Map<String, String>? = null
)

