package com.example.data.api

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

object MeetingLinks {
    fun canonical(input: String): String? {
        val value = input.trim()
        if (value.matches(Regex("[A-Za-z0-9_-]{4,64}"))) return "${ApiClient.BASE_URL}feedback/meet/$value"
        return try {
            val uri = URI(value)
            val base = URI(ApiClient.BASE_URL)
            if (uri.scheme != "https" || uri.host != base.host || uri.userInfo != null || uri.port !in listOf(-1, 443) || uri.fragment != null) return null
            val code = Regex("/feedback/meet/([A-Za-z0-9_-]{4,64})/?").matchEntire(uri.rawPath)?.groupValues?.get(1) ?: return null
            val ticket = uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("ticket=") }?.substringAfter('=')?.let { URLDecoder.decode(it, "UTF-8") }
            if (ticket != null && !ticket.matches(Regex("[A-Za-z0-9_-]{1,128}"))) return null
            "${ApiClient.BASE_URL}feedback/meet/$code" + if (ticket != null) "?ticket=${URLEncoder.encode(ticket, "UTF-8")}" else ""
        } catch (_: Exception) { null }
    }
}
