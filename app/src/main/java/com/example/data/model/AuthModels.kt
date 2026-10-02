package com.example.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@JsonClass(generateAdapter = true)
data class Country(
    @Json(name = "code") val code: String,
    @Json(name = "name") val name: String,
    @Json(name = "dialCode") val dialCode: String
) {
    val cleanDialCode: String
        get() = dialCode.replace("+", "").trim()

    val flagEmoji: String
        get() = countryCodeToEmoji(code)
}

fun countryCodeToEmoji(code: String): String {
    val clean = code.trim().uppercase()
    if (clean.length != 2) return "🌐"
    return try {
        val firstChar = Character.codePointAt(clean, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(clean, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (e: Exception) {
        "🌐"
    }
}

@JsonClass(generateAdapter = true)
data class CountriesResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "countries") val countries: List<Country> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "phoneOrWalletId") val phoneOrWalletId: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "phone_number") val phone_number: String? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "wallet_id") val wallet_id: String? = null,
    @Json(name = "wallet") val wallet: String? = null
)

@JsonClass(generateAdapter = true)
data class AvailableChannel(
    @Json(name = "type") val type: String, // "sms", "whatsapp", "email", "in-app"
    @Json(name = "available") val available: Boolean = true,
    @Json(name = "maskedDestination") val maskedDestination: String? = null
)

class AvailableChannelsAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): List<AvailableChannel> {
        val list = mutableListOf<AvailableChannel>()
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Unit>()
            return list
        }
        if (reader.peek() != JsonReader.Token.BEGIN_ARRAY) {
            reader.skipValue()
            return list
        }
        reader.beginArray()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.STRING -> {
                    val str = reader.nextString()
                    list.add(AvailableChannel(type = str, available = true))
                }
                JsonReader.Token.BEGIN_OBJECT -> {
                    var type = ""
                    var available = true
                    var maskedDestination: String? = null
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "type" -> type = reader.nextString()
                            "available" -> available = reader.nextBoolean()
                            "maskedDestination" -> {
                                if (reader.peek() == JsonReader.Token.NULL) {
                                    reader.nextNull<Unit>()
                                } else {
                                    maskedDestination = reader.nextString()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (type.isNotEmpty()) {
                        list.add(AvailableChannel(type, available, maskedDestination))
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endArray()
        return list
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: List<AvailableChannel>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        for (item in value) {
            writer.beginObject()
            writer.name("type").value(item.type)
            writer.name("available").value(item.available)
            writer.name("maskedDestination").value(item.maskedDestination)
            writer.endObject()
        }
        writer.endArray()
    }
}

@JsonClass(generateAdapter = true)
data class OtpSendRequest(
    @Json(name = "userId") val userId: Long,
    @Json(name = "channel") val channel: String
)

@JsonClass(generateAdapter = true)
data class OtpSendResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "maskedDestination") val maskedDestination: String? = null
)

@JsonClass(generateAdapter = true)
data class OtpVerifyRequest(
    @Json(name = "userId") val userId: Long,
    @Json(name = "code") val code: String,
    @Json(name = "otp") val otp: String = code
)

@JsonClass(generateAdapter = true)
data class PinVerifyRequest(
    @Json(name = "userId") val userId: Long,
    @Json(name = "pin") val pin: String
)

@JsonClass(generateAdapter = true)
data class ZodiacData(
    @Json(name = "key") val key: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "nameFr") val nameFr: String? = null,
    @Json(name = "nameEn") val nameEn: String? = null
)

@JsonClass(generateAdapter = true)
data class AstroInfo(
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "birthDate") val birthDate: String? = null,
    @Json(name = "zodiac") val zodiac: ZodiacData? = null,
    @Json(name = "sign") val sign: String? = null,
    @Json(name = "element") val element: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "luckyNumber") val luckyNumber: String? = null,
    @Json(name = "ascendant") val ascendant: String? = null
) {
    val resolvedSign: String?
        get() = zodiac?.nameFr ?: zodiac?.nameEn ?: sign
    val resolvedSymbol: String?
        get() = zodiac?.symbol ?: ""
}

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "wallet_id") val wallet_id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "full_name") val full_name: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "middleName") val middleName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "profession") val profession: String? = null,
    @Json(name = "activityDescription") val activityDescription: String? = null,
    @Json(name = "activity_description") val activity_description: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "province") val province: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "birthDate") val birthDate: String? = null,
    @Json(name = "birth_date") val birth_date: String? = null,
    @Json(name = "birthPlace") val birthPlace: String? = null,
    @Json(name = "nationality") val nationality: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "profilePhoto") val profilePhoto: String? = null,
    @Json(name = "profile_photo") val profile_photo: String? = null,
    @Json(name = "profilePhotoUrl") val profilePhotoUrl: String? = null,
    @Json(name = "profile_photo_url") val profile_photo_url: String? = null,
    @Json(name = "coverPhoto") val coverPhoto: String? = null,
    @Json(name = "cover_photo") val cover_photo: String? = null,
    @Json(name = "coverPhotoUrl") val coverPhotoUrl: String? = null,
    @Json(name = "cover_photo_url") val cover_photo_url: String? = null,
    @Json(name = "googleId") val googleId: String? = null,
    @Json(name = "zoomId") val zoomId: String? = null,
    @Json(name = "astro") val astro: AstroInfo? = null,
    @Json(name = "verificationStatus") val verificationStatus: String? = null,
    @Json(name = "verification_status") val verification_status: String? = null,
    @Json(name = "availabilityStatus") val availabilityStatus: String? = null,
    @Json(name = "availability_status") val availability_status: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "bitcoinAddress") val bitcoinAddress: String? = null,
    @Json(name = "bitcoin_address") val bitcoin_address: String? = null,
    @Json(name = "btcAddress") val btcAddress: String? = null,
    @Json(name = "btc_address") val btc_address: String? = null
) {
    val resolvedFullName: String
        get() = fullName?.ifBlank { null } ?: full_name?.ifBlank { null } ?: name?.ifBlank { null } ?: "Utilisateur CashPay"

    val resolvedWalletId: String
        get() = walletId?.ifBlank { null } ?: wallet_id?.ifBlank { null } ?: ""

    val resolvedAvatar: String?
        get() = profilePhotoUrl?.ifBlank { null } ?: profile_photo_url?.ifBlank { null } ?: profilePhoto?.ifBlank { null } ?: profile_photo?.ifBlank { null } ?: avatarUrl?.ifBlank { null }

    val resolvedCover: String?
        get() = coverPhotoUrl?.ifBlank { null } ?: cover_photo_url?.ifBlank { null } ?: coverPhoto?.ifBlank { null } ?: cover_photo?.ifBlank { null }

    val displayAvatar: String? get() = resolvedAvatar
    val displayCover: String? get() = resolvedCover

    val resolvedBitcoinAddress: String?
        get() = bitcoinAddress?.ifBlank { null } ?: bitcoin_address?.ifBlank { null } ?: btcAddress?.ifBlank { null } ?: btc_address?.ifBlank { null }

    val resolvedActivity: String?
        get() = activityDescription?.ifBlank { null } ?: activity_description?.ifBlank { null } ?: bio?.ifBlank { null }

    val resolvedBirthDate: String?
        get() = birthDate?.ifBlank { null } ?: birth_date?.ifBlank { null }

    val resolvedVerificationStatus: String?
        get() = verificationStatus?.ifBlank { null } ?: verification_status?.ifBlank { null }

    val resolvedAvailabilityStatus: String
        get() = availabilityStatus?.ifBlank { null } ?: availability_status?.ifBlank { null } ?: "available"
}

@JsonClass(generateAdapter = true)
data class ProfileUpdates(
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "profession") val profession: String? = null,
    @Json(name = "activity_description") val activityDescription: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "province") val province: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "birth_date") val birthDate: String? = null,
    @Json(name = "nationality") val nationality: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "profile_photo") val profilePhoto: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
    @Json(name = "cover_photo") val coverPhoto: String? = null,
    @Json(name = "cover_photo_url") val coverPhotoUrl: String? = null,
    @Json(name = "availability_status") val availabilityStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileWithPinRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "updates") val updates: ProfileUpdates
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: User? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "maskedPhone") val maskedPhone: String? = null,
    @Json(name = "maskedEmail") val maskedEmail: String? = null,
    @Json(name = "availableChannels") val availableChannels: List<AvailableChannel>? = null,
    @Json(name = "googleLinked") val googleLinked: Boolean? = null,
    @Json(name = "isGoogleLinked") val isGoogleLinked: Boolean? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileInstallationAvatar(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "source") val source: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "default") val default: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ProfileInstallationCover(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "source") val source: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileInstallationMediaEndpoints(
    @Json(name = "endpoint") val endpoint: String? = null,
    @Json(name = "upload") val upload: String? = null,
    @Json(name = "list") val list: String? = null,
    @Json(name = "delete") val delete: String? = null,
    @Json(name = "setProfile") val setProfile: String? = null,
    @Json(name = "setCover") val setCover: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileInstallationData(
    @Json(name = "ready") val ready: Boolean = false,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "avatar") val avatar: ProfileInstallationAvatar? = null,
    @Json(name = "cover") val cover: ProfileInstallationCover? = null,
    @Json(name = "media") val media: ProfileInstallationMediaEndpoints? = null
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "found") val found: Boolean = false,
    @Json(name = "user") val user: User? = null,
    @Json(name = "profile") val profile: User? = null,
    @Json(name = "profileInstallation") val profileInstallation: ProfileInstallationData? = null,
    @Json(name = "astro") val astro: AstroInfo? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
) {
    val resolvedUser: User?
        get() = profile ?: user
}
