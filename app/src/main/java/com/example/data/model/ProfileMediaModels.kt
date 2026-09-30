package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileMediaResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "media") val media: List<ProfileMediaItem> = emptyList(),
    @Json(name = "count") val count: Int = 0,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileMediaItem(
    @Json(name = "id") val id: Long,
    @Json(name = "type") val type: String = "album_image", // "image", "album_image", "profile", "cover"
    @Json(name = "fileName") val fileName: String? = null,
    @Json(name = "filePath") val filePath: String? = null,
    @Json(name = "url") val url: String,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "fileSize") val fileSize: Long? = null,
    @Json(name = "visibility") val visibility: String = "public",
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "isProfile") val isProfile: Boolean = false,
    @Json(name = "isCover") val isCover: Boolean = false
) {
    val displayUrl: String
        get() = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else if (url.startsWith("/")) {
            "https://app.cashpay-all.com$url"
        } else {
            "https://app.cashpay-all.com/$url"
        }
}

@JsonClass(generateAdapter = true)
data class SetMediaTargetRequest(
    @Json(name = "mediaId") val mediaId: Long
)

@JsonClass(generateAdapter = true)
data class SetProfileMediaResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "profilePhoto") val profilePhoto: String? = null,
    @Json(name = "profilePhotoUrl") val profilePhotoUrl: String? = null,
    @Json(name = "mediaId") val mediaId: Long? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SetCoverMediaResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "coverPhoto") val coverPhoto: String? = null,
    @Json(name = "coverPhotoUrl") val coverPhotoUrl: String? = null,
    @Json(name = "mediaId") val mediaId: Long? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class UploadProfileMediaResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "media") val media: ProfileMediaItem? = null,
    @Json(name = "error") val error: String? = null
)
