package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateStatusRequest(
    @Json(name = "type") val type: String, // "text", "image", "video"
    @Json(name = "content") val content: String? = null,
    @Json(name = "backgroundColor") val backgroundColor: String? = null,
    @Json(name = "mediaUrl") val mediaUrl: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "visibility") val visibility: String = "public" // "public", "friends", "private"
)

@JsonClass(generateAdapter = true)
data class CreateStatusResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "type") val type: String,
    @Json(name = "mediaUrl") val mediaUrl: String?,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String?,
    @Json(name = "visibility") val visibility: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "expiresAt") val expiresAt: String
)

@JsonClass(generateAdapter = true)
data class StatusesResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "statusGroups") val statusGroups: List<StatusGroup> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class StatusGroup(
    @Json(name = "userId") val userId: Long,
    @Json(name = "userName") val userName: String,
    @Json(name = "userAvatar") val userAvatar: String? = null,
    @Json(name = "statuses") val statuses: List<OnChatStatusItem> = emptyList(),
    @Json(name = "lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class OnChatStatusItem(
    @Json(name = "id") val id: Long,
    @Json(name = "type") val type: String, // "text", "image", "video"
    @Json(name = "content") val content: String? = null,
    @Json(name = "backgroundColor") val backgroundColor: String? = null,
    @Json(name = "mediaUrl") val mediaUrl: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "fileSize") val fileSize: Long? = null,
    @Json(name = "visibility") val visibility: String = "public",
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "expiresAt") val expiresAt: String,
    @Json(name = "viewsCount") val viewsCount: Int = 0,
    @Json(name = "likesCount") val likesCount: Int = 0,
    @Json(name = "isLiked") val isLiked: Boolean = false,
    @Json(name = "viewedByMe") val viewedByMe: Boolean = false,
    @Json(name = "viewers") val viewers: List<StatusViewerUser> = emptyList(),
    @Json(name = "likers") val likers: List<StatusViewerUser> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StatusViewerUser(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "viewedAt") val viewedAt: String? = null
) {
    val avatar: String? get() = avatarUrl
}

@JsonClass(generateAdapter = true)
data class CheckStatusesRequest(
    @Json(name = "phoneNumbers") val phoneNumbers: List<String>
)

@JsonClass(generateAdapter = true)
data class CheckStatusesResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "contacts") val contacts: List<OnChatStatusContact>
)

@JsonClass(generateAdapter = true)
data class OnChatStatusContact(
    @Json(name = "phone") val phone: String,
    @Json(name = "normalizedPhone") val normalizedPhone: String,
    @Json(name = "onChat") val onChat: Boolean,
    @Json(name = "user") val user: OnChatStatusUser?,
    @Json(name = "hasActiveStatus") val hasActiveStatus: Boolean,
    @Json(name = "statuses") val statuses: List<OnChatStatusItem>
)

@JsonClass(generateAdapter = true)
data class OnChatStatusUser(
    @Json(name = "id") val id: Long,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "profile_photo") val profilePhoto: String?,
    @Json(name = "phone") val phone: String
)

@JsonClass(generateAdapter = true)
data class StatusViewRequest(
    @Json(name = "statusId") val statusId: Long
)

@JsonClass(generateAdapter = true)
data class StatusViewResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "statusId") val statusId: Long,
    @Json(name = "viewedByMe") val viewedByMe: Boolean,
    @Json(name = "viewsCount") val viewsCount: Int
)

@JsonClass(generateAdapter = true)
data class StatusLikeRequest(
    @Json(name = "statusId") val statusId: Long,
    @Json(name = "liked") val liked: Boolean
)

@JsonClass(generateAdapter = true)
data class StatusLikeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "statusId") val statusId: Long,
    @Json(name = "likedByMe") val likedByMe: Boolean,
    @Json(name = "likesCount") val likesCount: Int
)

@JsonClass(generateAdapter = true)
data class CreateStatusMultipartResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "status") val status: OnChatMultipartStatusItem? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class OnChatMultipartStatusItem(
    @Json(name = "id") val id: Long,
    @Json(name = "userId") val userId: Long,
    @Json(name = "type") val type: String,
    @Json(name = "content") val content: String,
    @Json(name = "backgroundColor") val backgroundColor: String? = null,
    @Json(name = "expiresAt") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoUploadGeneralResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "type") val type: String? = "video",
    @Json(name = "url") val url: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = null,
    @Json(name = "visibility") val visibility: String? = "public"
)

@JsonClass(generateAdapter = true)
data class VideoListGeneralResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "videos") val videos: List<VideoUploadGeneralResponse> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VideoDetailGeneralResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "video") val video: VideoUploadGeneralResponse? = null
)
