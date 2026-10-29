package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubtitleCue(val start: Double, val end: Double, val text: String)
@JsonClass(generateAdapter = true)
data class EditorPlan(val filter: String, val speed: Float, val voiceVolume: Float, val caption: String, val subtitles: List<SubtitleCue> = emptyList())
@JsonClass(generateAdapter = true)
data class EditorRequest(val prompt: String, val durationSeconds: Double, val audioDataUri: String? = null)
@JsonClass(generateAdapter = true)
data class EditorResponse(val success: Boolean = false, val plan: EditorPlan? = null, val error: String? = null)
@JsonClass(generateAdapter = true)
data class ReusableSound(val sourceId: Long, val title: String, val allowReuse: Boolean, val authorId: Long, val authorName: String, val authorAvatar: String? = null, val url: String)
@JsonClass(generateAdapter = true)
data class SoundResponse(val success: Boolean = false, val sound: ReusableSound? = null, val error: String? = null)
