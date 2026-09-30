package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Group(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "description") val description: String? = null,
    @Json(name = "photoUrl") val photoUrl: String? = null,
    @Json(name = "membersCount") val membersCount: Int = 1,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    @Json(name = "name") val name: String,
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "description") val description: String? = null,
    @Json(name = "photoUrl") val photoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class AddMemberRequest(
    @Json(name = "userId") val userId: Long
)

@JsonClass(generateAdapter = true)
data class GroupMember(
    @Json(name = "id") val id: Long,
    @Json(name = "userId") val userId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String = "member",
    @Json(name = "avatarUrl") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupsResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "groups") val groups: List<Group> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupDetailsResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "group") val group: Group? = null,
    @Json(name = "members") val members: List<GroupMember> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SimpleResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)
