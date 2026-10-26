package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val idLong: Long? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "wallet_id") val wallet_id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "full_name") val full_name: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "profilePhoto") val profilePhoto: String? = null,
    @Json(name = "profile_photo") val profile_photo: String? = null,
    @Json(name = "profilePhotoUrl") val profilePhotoUrl: String? = null,
    @Json(name = "profile_photo_url") val profile_photo_url: String? = null,
    @Json(name = "coverPhoto") val coverPhoto: String? = null,
    @Json(name = "cover_photo") val cover_photo: String? = null,
    @Json(name = "coverPhotoUrl") val coverPhotoUrl: String? = null,
    @Json(name = "cover_photo_url") val cover_photo_url: String? = null,
    @Json(name = "profession") val profession: String? = null,
    @Json(name = "activityDescription") val activityDescription: String? = null,
    @Json(name = "activity_description") val activity_description: String? = null,
    @Json(name = "birthDate") val birthDate: String? = null,
    @Json(name = "birth_date") val birth_date: String? = null,
    @Json(name = "nationality") val nationality: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "province") val province: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "verificationStatus") val verificationStatus: String? = null,
    @Json(name = "verification_status") val verification_status: String? = null,
    @Json(name = "availabilityStatus") val availabilityStatus: String? = null,
    @Json(name = "availability_status") val availability_status: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "astro") val astro: AstroInfo? = null,
    @Json(name = "bitcoin") val bitcoin: ProfileBitcoinDto? = null,
    @Json(name = "bitcoinAddress") val bitcoinAddress: String? = null,
    @Json(name = "bitcoin_address") val bitcoin_address: String? = null,
    @Json(name = "btcAddress") val btcAddress: String? = null,
    @Json(name = "btc_address") val btc_address: String? = null,
    @Json(name = "relationship") val relationship: RelationshipDto? = null,
    @Json(name = "followers") val followers: FollowersDto? = null,
    @Json(name = "following") val following: FollowersDto? = null,
    @Json(name = "media") val media: MediaDto? = null,
    @Json(name = "albums") val albums: List<SocialAlbum> = emptyList()
) {
    val id: Long
        get() = idLong ?: userId ?: 0L

    val resolvedFullName: String
        get() = fullName?.ifBlank { null } ?: full_name?.ifBlank { null } ?: name?.ifBlank { null } ?: "${firstName.orEmpty()} ${lastName.orEmpty()}".trim().ifEmpty { null } ?: "Contact OnChat"

    val resolvedWalletId: String
        get() = walletId?.ifBlank { null } ?: wallet_id?.ifBlank { null } ?: "CD0100000000"

    val resolvedBitcoinAddress: String?
        get() = bitcoinAddress?.ifBlank { null }
            ?: bitcoin_address?.ifBlank { null }
            ?: btcAddress?.ifBlank { null }
            ?: btc_address?.ifBlank { null }
            ?: bitcoin?.address?.ifBlank { null }

    val resolvedAvatar: String?
        get() = profilePhotoUrl?.ifBlank { null } ?: profile_photo_url?.ifBlank { null } ?: profilePhoto?.ifBlank { null } ?: profile_photo?.ifBlank { null }

    val resolvedCover: String?
        get() = coverPhotoUrl?.ifBlank { null } ?: cover_photo_url?.ifBlank { null } ?: coverPhoto?.ifBlank { null } ?: cover_photo?.ifBlank { null }

    val displayAvatar: String? get() = resolvedAvatar
    val displayCover: String? get() = resolvedCover

    val resolvedActivity: String?
        get() = activityDescription?.ifBlank { null } ?: activity_description?.ifBlank { null }

    val resolvedBirthDate: String?
        get() = birthDate?.ifBlank { null } ?: birth_date?.ifBlank { null }

    val resolvedVerificationStatus: String?
        get() = verificationStatus?.ifBlank { null } ?: verification_status?.ifBlank { null }

    val resolvedAvailabilityStatus: String
        get() = availabilityStatus?.ifBlank { null } ?: availability_status?.ifBlank { null } ?: "available"
}

@JsonClass(generateAdapter = true)
data class PhoneProfileResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "found") val found: Boolean = false,
    @Json(name = "profile") val profile: ProfileDto? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "wallet_id") val wallet_id: String? = null,
    @Json(name = "bitcoinAddress") val bitcoinAddress: String? = null,
    @Json(name = "bitcoin_address") val bitcoin_address: String? = null,
    @Json(name = "btcAddress") val btcAddress: String? = null,
    @Json(name = "error") val error: String? = null
) {
    val relation: SocialRelation? get() = profile?.relationship?.let {
        SocialRelation(
            following = it.isFollowing,
            isFriend = it.isFollowedBy,
            friendshipStatus = "none"
        )
    }

    val stats: SocialStats? get() = profile?.let {
        SocialStats(
            followers = it.followers?.count ?: 0,
            following = it.following?.count ?: 0,
            friends = 0
        )
    }

    val media: List<SocialMediaItem> get() = profile?.media?.items?.map { item ->
        SocialMediaItem(
            id = item.id,
            ownerId = item.ownerId,
            mediaType = item.mediaType,
            fileUrl = item.fileUrl,
            mimeType = item.mimeType,
            likeCount = item.likeCount,
            commentCount = item.commentCount,
            likedByMe = item.likedByMe
        )
    } ?: emptyList()

    val albums: List<SocialAlbum> get() = profile?.albums ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class ContactsCheckRequest(
    @Json(name = "phoneNumbers") val phoneNumbers: List<String>
)

@JsonClass(generateAdapter = true)
data class ProfileBitcoinDto(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "address") val address: String? = null,
    @Json(name = "balance") val balance: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class ContactCheckResult(
    @Json(name = "phone") val phone: String,
    @Json(name = "onChat") val onChat: Boolean = false,
    @Json(name = "hasActiveStatus") val hasActiveStatus: Boolean = false,
    @Json(name = "statuses") val statuses: List<OnChatStatusItem> = emptyList(),
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "directBitcoinAddress") val directBitcoinAddress: String? = null,
    @Json(name = "profile") val profile: ProfileDto? = null
) {
    val resolvedWalletId: String get() = walletId?.ifBlank { null } ?: profile?.resolvedWalletId ?: "CD0100000000"
    val bitcoinAddress: String? get() = directBitcoinAddress?.ifBlank { null } ?: profile?.resolvedBitcoinAddress
}

@JsonClass(generateAdapter = true)
data class RelationshipDto(
    @Json(name = "isFollowing") val isFollowing: Boolean = false,
    @Json(name = "isFollowedBy") val isFollowedBy: Boolean = false
)

@JsonClass(generateAdapter = true)
data class FollowersDto(
    @Json(name = "count") val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class MediaDto(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "items") val items: List<MediaItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MediaItemDto(
    @Json(name = "id") val id: Long,
    @Json(name = "ownerId") val ownerId: Long,
    @Json(name = "mediaType") val mediaType: String,
    @Json(name = "fileUrl") val fileUrl: String,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "likeCount") val likeCount: Int = 0,
    @Json(name = "commentCount") val commentCount: Int = 0,
    @Json(name = "likedByMe") val likedByMe: Boolean = false
)
