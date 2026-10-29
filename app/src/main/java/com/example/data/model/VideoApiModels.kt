package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoUploadResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "video") val video: VideoItem? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoListResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "videos") val videos: List<VideoItem> = emptyList(),
    @Json(name = "total") val total: Int? = 0
)

@JsonClass(generateAdapter = true)
data class VideoDetailResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "video") val video: VideoItem? = null
)

@JsonClass(generateAdapter = true)
data class VideoLikeResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "liked") val liked: Boolean? = false,
    @Json(name = "likes") val likes: Int? = 0,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoCommentsResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "comments") val comments: List<VideoCommentItem> = emptyList(),
    @Json(name = "total") val total: Int? = 0
)

@JsonClass(generateAdapter = true)
data class PostVideoCommentRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class PostVideoCommentResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "comment") val comment: VideoCommentItem? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoPublisher(
    @Json(name = "type") val type: String? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "ownerId") val ownerId: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "avatar") val avatar: String? = null
) {
    val resolvedName: String
        get() = name?.ifBlank { null }
            ?: fullName?.ifBlank { null }
            ?: username?.ifBlank { null }
            ?: "Créateur OnChat"

    val resolvedAvatar: String?
        get() = avatarUrl?.ifBlank { null } ?: avatar?.ifBlank { null }
}

@JsonClass(generateAdapter = true)
data class VideoInteractions(
    @Json(name = "likes") val likes: Int? = 0,
    @Json(name = "likesCount") val likesCount: Int? = 0,
    @Json(name = "comments") val comments: Int? = 0,
    @Json(name = "shares") val shares: Int? = 0,
    @Json(name = "views") val views: Int? = 0,
    @Json(name = "liked") val liked: Boolean? = false,
    @Json(name = "isLiked") val isLiked: Boolean? = false,
    @Json(name = "likedByMe") val likedByMe: Boolean? = false
) {
    val realLikesCount: Int
        get() {
            val l = likes ?: 0
            val lc = likesCount ?: 0
            return if (l > 0) l else lc
        }

    val realIsLiked: Boolean
        get() = (liked == true) || (isLiked == true) || (likedByMe == true)
}

@JsonClass(generateAdapter = true)
data class VideoFollowInfo(
    @Json(name = "isFollowing") val isFollowing: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class VideoItem(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "url") val url: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "caption") val caption: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = 0,
    @Json(name = "publisher") val publisher: VideoPublisher? = null,
    @Json(name = "interactions") val interactions: VideoInteractions? = null,
    @Json(name = "follow") val follow: VideoFollowInfo? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "soundTitle") val soundTitle: String? = null,
    @Json(name = "soundAuthor") val soundAuthor: String? = null
) {
    val resolvedUrl: String
        get() = url?.ifBlank { null } ?: videoUrl?.ifBlank { null } ?: ""

    val resolvedThumbnailUrl: String?
        get() = thumbnailUrl?.ifBlank { null } ?: resolvedUrl.ifBlank { null }

    val resolvedContent: String
        get() = content?.ifBlank { null }
            ?: description?.ifBlank { null }
            ?: caption?.ifBlank { null }
            ?: ""
}

@JsonClass(generateAdapter = true)
data class VideoCommentUser(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "avatar") val avatar: String? = null
) {
    val resolvedName: String
        get() = name?.ifBlank { null } ?: "Utilisateur OnChat"

    val resolvedAvatar: String?
        get() = avatarUrl?.ifBlank { null } ?: avatar?.ifBlank { null }
}

@JsonClass(generateAdapter = true)
data class VideoCommentItem(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "content") val content: String = "",
    @Json(name = "user") val user: VideoCommentUser? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)
