package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MonPaysCompany(
    @Json(name = "id") val id: Long,
    @Json(name = "company_name") val companyName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "cover_photo_url") val coverPhotoUrl: String? = null,
    @Json(name = "status") val status: String? = "verified",
    @Json(name = "followerCount") val followerCount: Int? = 0,
    @Json(name = "isFollowing") val isFollowing: Boolean? = false
) {
    val displayName: String
        get() = companyName ?: name ?: "Page MonPays"
}

@JsonClass(generateAdapter = true)
data class MonPaysActivity(
    @Json(name = "id") val id: Long,
    @Json(name = "companyId") val companyId: Long? = null,
    @Json(name = "companyName") val companyName: String? = null,
    @Json(name = "companyLogo") val companyLogo: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "mediaUrl") val mediaUrl: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "likesCount") val likesCount: Int? = 0,
    @Json(name = "commentsCount") val commentsCount: Int? = 0,
    @Json(name = "isLiked") val isLiked: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class MonPaysListResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "companies") val companies: List<MonPaysCompany> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MonPaysDetailResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "company") val company: MonPaysCompany? = null,
    @Json(name = "followerCount") val followerCount: Int? = 0,
    @Json(name = "isFollowing") val isFollowing: Boolean? = false,
    @Json(name = "activities") val activities: List<MonPaysActivity> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MonPaysFollowRequest(
    @Json(name = "dummy") val dummy: String? = null // Dummy field to keep JSON object structure
)

@JsonClass(generateAdapter = true)
data class MonPaysFollowResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "following") val following: Boolean = false,
    @Json(name = "followerCount") val followerCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class MonPaysFeedResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "activities") val activities: List<MonPaysActivity> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MonPaysComment(
    @Json(name = "id") val id: Long = System.currentTimeMillis(),
    @Json(name = "postId") val postId: Long,
    @Json(name = "userId") val userId: Long,
    @Json(name = "userName") val userName: String? = "Utilisateur",
    @Json(name = "userAvatar") val userAvatar: String? = null,
    @Json(name = "content") val content: String,
    @Json(name = "createdAt") val createdAt: String? = "À l'instant"
)

@JsonClass(generateAdapter = true)
data class MonPaysCommentRequest(
    @Json(name = "postId") val postId: Long,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class MonPaysCommentsResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "comments") val comments: List<MonPaysComment> = emptyList()
)
