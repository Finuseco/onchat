package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class ForgotPinFlowStep {
    ID_INPUT,
    CHANNEL_SELECTION,
    OTP_VERIFY,
    NEW_PIN_INPUT,
    SUCCESS
}

@JsonClass(generateAdapter = true)
data class ForgotPinRequest(
    @Json(name = "phoneOrWalletId") val phoneOrWalletId: String
)

@JsonClass(generateAdapter = true)
data class ForgotPinRequestResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "resetToken") val resetToken: String? = null,
    @Json(name = "availableChannels") val availableChannels: List<AvailableChannel>? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPinVerifyRequest(
    @Json(name = "resetToken") val resetToken: String,
    @Json(name = "otp") val otp: String
)

@JsonClass(generateAdapter = true)
data class ForgotPinVerifyResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "verifiedToken") val verifiedToken: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPinResetRequest(
    @Json(name = "verifiedToken") val verifiedToken: String,
    @Json(name = "newPin") val newPin: String
)

@JsonClass(generateAdapter = true)
data class ForgotPinResetResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null
)
