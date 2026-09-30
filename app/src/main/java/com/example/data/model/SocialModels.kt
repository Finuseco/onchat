package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SocialMediaItem(
    @Json(name = "id") val id: Long,
    @Json(name = "ownerType") val ownerType: String? = null,
    @Json(name = "ownerId") val ownerId: Long? = null,
    @Json(name = "mediaType") val mediaType: String? = null,
    @Json(name = "fileName") val fileName: String? = null,
    @Json(name = "filePath") val filePath: String? = null,
    @Json(name = "fileUrl") val fileUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "fileSize") val fileSize: Long? = null,
    @Json(name = "visibility") val visibility: String? = "public",
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "likeCount") val likeCount: Int = 0,
    @Json(name = "commentCount") val commentCount: Int = 0,
    @Json(name = "likedByMe") val likedByMe: Boolean = false
) {
    val resolvedUrl: String
        get() = fileUrl?.ifBlank { null } ?: url?.ifBlank { null } ?: ""

    val resolvedDisplayUrl: String
        get() {
            val base = resolvedUrl
            return if (base.startsWith("http")) {
                base
            } else if (base.startsWith("/")) {
                "https://app.cashpay-all.com$base"
            } else if (base.isNotEmpty()) {
                "https://app.cashpay-all.com/$base"
            } else {
                ""
            }
        }

    val isVideo: Boolean
        get() = mediaType?.contains("video", ignoreCase = true) == true || mimeType?.startsWith("video/") == true
}

@JsonClass(generateAdapter = true)
data class SocialRelation(
    @Json(name = "following") val following: Boolean = false,
    @Json(name = "isFriend") val isFriend: Boolean = false,
    @Json(name = "friendshipStatus") val friendshipStatus: String? = "none", // none, sent, received, accepted
    @Json(name = "relationStatus") val relationStatus: String? = "none" // none, active, pending
)

@JsonClass(generateAdapter = true)
data class SocialStats(
    @Json(name = "followers") val followers: Int = 0,
    @Json(name = "following") val following: Int = 0,
    @Json(name = "friends") val friends: Int = 0
)

@JsonClass(generateAdapter = true)
data class SocialAlbum(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "mediaCount") val mediaCount: Int = 0,
    @Json(name = "coverUrl") val coverUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SocialActionRequest(
    @Json(name = "userId") val userId: Long,
    @Json(name = "action") val action: String, // follow, unfollow, friend-request, friend-accept, friend-cancel, relation-request
    @Json(name = "relationType") val relationType: String? = null // professional, sentimental, etc.
)

@JsonClass(generateAdapter = true)
data class SocialActionResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "relation") val relation: SocialRelation? = null,
    @Json(name = "stats") val stats: SocialStats? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)
