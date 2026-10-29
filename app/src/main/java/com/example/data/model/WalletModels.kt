package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@JsonClass(generateAdapter = true)
data class WalletApiResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "wallet") val wallet: WalletObject? = null,
    @Json(name = "user") val user: WalletUserObject? = null,
    @Json(name = "nationalCurrency") val nationalCurrency: NationalCurrencyDto? = null,
    @Json(name = "balances") val balances: BalancesDto? = null,
    @Json(name = "bitcoin") val bitcoin: BitcoinDto? = null,
    @Json(name = "usdt") val usdt: BitcoinDto? = null,
    @Json(name = "crypto") val crypto: List<CryptoBalanceDto>? = null,
    @Json(name = "cards") val cards: List<CardDto>? = null,
    @Json(name = "transactions") val transactions: List<TransactionDto>? = null,
    @Json(name = "sync") val sync: SyncDto? = null
)

fun WalletApiResponse.assetBalance(code: String): Double {
    val currency = code.uppercase()
    if (currency == "BTC") bitcoin?.balance?.let { return it }
    if (currency == "USDT" || currency == "USDT_TRC20") usdt?.balance?.let { return it }
    val aliases = if (currency == "USDT") setOf("USDT", "USDT_TRC20") else setOf(currency)
    return (crypto.orEmpty() + balances?.crypto.orEmpty()).firstOrNull { it.currency?.uppercase() in aliases }?.balance
        ?: balances?.fiat?.get(currency) ?: 0.0
}

@JsonClass(generateAdapter = true)
data class WalletObject(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "isGroupWallet") val isGroupWallet: Boolean? = false,
    @Json(name = "lastUpdated") val lastUpdated: String? = null
)

@JsonClass(generateAdapter = true)
data class WalletUserObject(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "walletId") val walletId: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "countryCode") val countryCode: String? = null,
    @Json(name = "profilePhoto") val profilePhoto: String? = null,
    @Json(name = "profilePhotoUrl") val profilePhotoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class NationalCurrencyDto(
    @Json(name = "countryCode") val countryCode: String? = "CD",
    @Json(name = "code") val code: String? = "CDF",
    @Json(name = "name") val name: String? = "Franc congolais",
    @Json(name = "symbol") val symbol: String? = "FC"
)

@JsonClass(generateAdapter = true)
data class BalancesDto(
    @Json(name = "fiat") val fiat: Map<String, Double>? = null,
    @Json(name = "crypto") val crypto: List<CryptoBalanceDto>? = null
)

@JsonClass(generateAdapter = true)
data class CryptoBalanceDto(
    @Json(name = "currency") val currency: String? = "BTC",
    @Json(name = "balance") val balance: Double? = 0.0,
    @Json(name = "address") val address: String? = null
)

@JsonClass(generateAdapter = true)
data class BitcoinDto(
    @Json(name = "address") val address: String? = null,
    @Json(name = "balance") val balance: Double? = 0.0,
    @Json(name = "available") val available: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class CardDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "brand") val brand: String? = "visa",
    @Json(name = "cardType") val cardType: String? = "physique",
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "last4") val last4: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "isPrimary") val isPrimary: Boolean? = false,
    @Json(name = "cardProviderId") val cardProviderId: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncDto(
    @Json(name = "source") val source: String? = null,
    @Json(name = "serverAuthoritative") val serverAuthoritative: Boolean? = true,
    @Json(name = "realtimeCompatible") val realtimeCompatible: Boolean? = true,
    @Json(name = "fetchedAt") val fetchedAt: String? = null
)

class BalancesDtoAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): BalancesDto {
        var fiat: Map<String, Double>? = null
        var crypto: List<CryptoBalanceDto>? = null

        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "fiat" -> {
                        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                            val map = mutableMapOf<String, Double>()
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val key = reader.nextName()
                                try {
                                    when (reader.peek()) {
                                        JsonReader.Token.NUMBER -> {
                                            map[key] = reader.nextDouble()
                                        }
                                        JsonReader.Token.STRING -> {
                                            val s = reader.nextString()
                                            map[key] = s.toDoubleOrNull() ?: 0.0
                                        }
                                        else -> {
                                            reader.skipValue()
                                            map[key] = 0.0
                                        }
                                    }
                                } catch (_: Exception) {
                                    map[key] = 0.0
                                }
                            }
                            reader.endObject()
                            fiat = map
                        } else {
                            reader.skipValue()
                        }
                    }
                    "crypto" -> {
                        reader.skipValue()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()
        }
        return BalancesDto(fiat = fiat, crypto = crypto)
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: BalancesDto?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("fiat")
        writer.beginObject()
        value.fiat?.forEach { (k, v) ->
            writer.name(k).value(v)
        }
        writer.endObject()
        writer.endObject()
    }
}
