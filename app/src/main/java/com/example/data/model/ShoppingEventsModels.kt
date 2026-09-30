package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShoppingFeedResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "products") val products: List<ProductItem>? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "limit") val limit: Int? = null,
    @Json(name = "authenticated") val authenticated: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ProductItem(
    @Json(name = "productId") val productId: String,
    @Json(name = "productName") val productName: String,
    @Json(name = "sellerId") val sellerId: String,
    @Json(name = "boutiqueId") val boutiqueId: String? = null,
    @Json(name = "shopName") val shopName: String? = null,
    @Json(name = "followerCount") val followerCount: Int? = null,
    @Json(name = "isFollowing") val isFollowing: Boolean? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "productUrl") val productUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class FollowShopRequest(
    @Json(name = "sellerId") val sellerId: Int
)

@JsonClass(generateAdapter = true)
data class FollowShopResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "sellerId") val sellerId: Int,
    @Json(name = "isFollowing") val isFollowing: Boolean,
    @Json(name = "followerCount") val followerCount: Int
)

@JsonClass(generateAdapter = true)
data class EventsFeedResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "events") val events: List<EventItem>? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "limit") val limit: Int? = null
)

@JsonClass(generateAdapter = true)
data class EventItem(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "date") val date: String,
    @Json(name = "time") val time: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "organizerName") val organizerName: String? = null,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "minimumTicketPrice") val minimumTicketPrice: Double? = null,
    @Json(name = "ticketTypes") val ticketTypes: List<TicketType>? = null,
    @Json(name = "eventUrl") val eventUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class TicketType(
    @Json(name = "id") val id: Int,
    @Json(name = "type") val type: String,
    @Json(name = "price") val price: Double,
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "quantity") val quantity: Int? = null,
    @Json(name = "sold") val sold: Int? = null
)
