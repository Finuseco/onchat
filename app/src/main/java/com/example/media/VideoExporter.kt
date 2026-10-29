package com.example.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.*
import androidx.media3.transformer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A single rendering pipeline applies edits before any upload starts. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object VideoExporter {
    data class Label(val text: String, val color: Int, val background: Int, val x: Float, val y: Float)
    data class Edits(
        val start: Float, val end: Float, val speed: Float,
        val volume: Float, val muted: Boolean, val filter: String,
        val labels: List<Label>, val music: Uri? = null, val musicVolume: Float = 0.7f,
        val photos: List<Uri> = emptyList(), val sourceIsPhoto: Boolean = false, val photoDurationMs: Long = 3000,
        val outputHeight: Int = 720, val subtitles: List<com.example.data.model.SubtitleCue> = emptyList()
    )

    suspend fun durationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?.takeIf { it > 0 } ?: error("Durée de vidéo introuvable")
        } finally { retriever.release() }
    }

    suspend fun export(context: Context, uri: Uri, durationMs: Long, edits: Edits): File = withContext(Dispatchers.Main) {
        require(durationMs > 0 && edits.start >= 0 && edits.end <= 1 && edits.start < edits.end)
        require(edits.speed in 0.5f..2f)
        require(edits.photoDurationMs in 1000..10000 && edits.outputHeight in listOf(720, 1080))
        val photos = if (edits.sourceIsPhoto) listOf(uri) + edits.photos else edits.photos
        val expectedDuration = (if (edits.sourceIsPhoto) 0.0 else durationMs * (edits.end - edits.start) / edits.speed.toDouble()) + photos.size * edits.photoDurationMs
        require(expectedDuration in 1.0..600_000.0) { "Le montage doit durer au maximum 10 minutes (photos comprises)." }
        val media = MediaItem.Builder().setUri(uri).setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs((durationMs * edits.start).toLong())
                .setEndPositionMs((durationMs * edits.end).toLong()).build()
        ).build()
        val videoEffects = mutableListOf<Effect>(Presentation.createForWidthAndHeight(edits.outputHeight, (edits.outputHeight * 16f / 9f).toInt(), Presentation.LAYOUT_SCALE_TO_FIT))
        if (edits.speed != 1f) videoEffects.add(SpeedChangeEffect(edits.speed))
        when (edits.filter) {
            "Noir & Blanc" -> videoEffects.add(RgbFilter.createGrayscaleFilter())
            "Sepia" -> videoEffects.add(RgbMatrix { _, _ -> floatArrayOf(
                .393f, .349f, .272f, 0f, .769f, .686f, .534f, 0f,
                .189f, .168f, .131f, 0f, 0f, 0f, 0f, 1f
            ) })
            "Cinema", "Cyber Neon", "Vibrant" -> {
                val saturation = when (edits.filter) { "Cinema" -> 1.3f; "Cyber Neon" -> 1.8f; else -> 1.5f }
                val matrix = android.graphics.ColorMatrix().apply { setSaturation(saturation) }.array
                videoEffects.add(RgbMatrix { _, _ -> floatArrayOf(
                    matrix[0], matrix[5], matrix[10], 0f,
                    matrix[1], matrix[6], matrix[11], 0f,
                    matrix[2], matrix[7], matrix[12], 0f, 0f, 0f, 0f, 1f
                ) })
            }
        }
        if (edits.labels.isNotEmpty()) {
            val overlays = edits.labels.map { label ->
                val text = SpannableString(label.text).apply {
                    setSpan(ForegroundColorSpan(label.color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(BackgroundColorSpan(label.background), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                TextOverlay.createStaticTextOverlay(text, OverlaySettings.Builder()
                    .setBackgroundFrameAnchor(label.x.coerceIn(-1f, 1f), label.y.coerceIn(-1f, 1f))
                    .setScale(0.5f, 0.5f).build())
            }
            videoEffects.add(OverlayEffect(overlays))
        }
        val audioEffects = mutableListOf<AudioProcessor>()
        if (edits.speed != 1f) audioEffects.add(SonicAudioProcessor().apply { setSpeed(edits.speed) })
        audioEffects.add(VolumeProcessor(edits.volume))
        val photoEffects = Effects(emptyList(), videoEffects.filterNot { it is SpeedChangeEffect })
        if (edits.subtitles.isNotEmpty()) {
            videoEffects.add(OverlayEffect(listOf(object : TextOverlay() {
                override fun getText(presentationTimeUs: Long): SpannableString {
                    val sourceSeconds = presentationTimeUs / 1_000_000.0 * edits.speed + durationMs * edits.start / 1000.0
                    val cue = edits.subtitles.firstOrNull { sourceSeconds >= it.start && sourceSeconds < it.end }
                    return SpannableString(cue?.text ?: " ").apply {
                        setSpan(ForegroundColorSpan(if (cue == null) android.graphics.Color.TRANSPARENT else android.graphics.Color.WHITE), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        if (cue != null) setSpan(BackgroundColorSpan(0x99000000.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = OverlaySettings.Builder().setBackgroundFrameAnchor(0f, -0.65f).setScale(0.4f, 0.4f).build()
            })))
        }
        val item = EditedMediaItem.Builder(media).setRemoveAudio(edits.muted)
            .setEffects(Effects(audioEffects, videoEffects)).build()
        val items = mutableListOf<EditedMediaItem>()
        if (!edits.sourceIsPhoto) items.add(item)
        photos.forEach { photo ->
            val image = MediaItem.Builder().setUri(photo).setImageDurationMs(edits.photoDurationMs).build()
            items.add(EditedMediaItem.Builder(image).setFrameRate(30).setRemoveAudio(true).setEffects(photoEffects).build())
        }
        val sequences = mutableListOf(EditedMediaItemSequence.Builder(items).build())
        edits.music?.let { music ->
            val musicItem = EditedMediaItem.Builder(MediaItem.fromUri(music)).setRemoveVideo(true)
                .setEffects(Effects(listOf(VolumeProcessor(edits.musicVolume)), emptyList())).build()
            sequences.add(EditedMediaItemSequence.Builder(musicItem).setIsLooping(true).build())
        }
        val composition = Composition.Builder(sequences).experimentalSetForceAudioTrack(true).build()
        val output = File(context.cacheDir, "edited_${java.util.UUID.randomUUID()}.mp4")
        suspendCancellableCoroutine { continuation ->
            val bitrate = minOf(if (edits.outputHeight == 720) 2_000_000 else 4_000_000, (170.0 * 1024 * 1024 * 8 * 1000 / expectedDuration).toInt()).coerceAtLeast(300_000)
            val transformer = Transformer.Builder(context)
                .setEncoderFactory(DefaultEncoderFactory.Builder(context).setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build()).setEnableFallback(true).build())
                .setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(output)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        output.delete()
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                }).build()
            continuation.invokeOnCancellation {
                android.os.Handler(android.os.Looper.getMainLooper()).post { transformer.cancel(); output.delete() }
            }
            try { transformer.start(composition, output.absolutePath) }
            catch (e: Exception) { output.delete(); if (continuation.isActive) continuation.resumeWithException(e) }
        }
    }

    suspend fun audioForJimmy(context: Context, uri: Uri, startMs: Long, endMs: Long): String = withContext(Dispatchers.IO) {
        val extractor = android.media.MediaExtractor()
        val file = File(context.cacheDir, "jimmy_${java.util.UUID.randomUUID()}.m4a")
        var muxer: android.media.MediaMuxer? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(android.media.MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: error("Cette vidéo ne contient pas de piste audio.")
            extractor.selectTrack(track)
            extractor.seekTo(startMs * 1000, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val writer = android.media.MediaMuxer(file.path, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = writer
            val outputTrack = writer.addTrack(extractor.getTrackFormat(track)); writer.start()
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            while (extractor.sampleTime >= 0 && extractor.sampleTime < endMs * 1000) {
                if (extractor.sampleTime < startMs * 1000) { extractor.advance(); continue }
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.set(0, size, extractor.sampleTime - startMs * 1000, extractor.sampleFlags)
                writer.writeSampleData(outputTrack, buffer, info)
                check(file.length() <= 20L * 1024 * 1024) { "L’audio dépasse 20 Mo. Réduisez la sélection pour Jimmy." }
                extractor.advance()
            }
            writer.stop(); writer.release(); muxer = null
            "data:audio/mp4;base64," + android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
        } finally { extractor.release(); runCatching { muxer?.release() }; file.delete() }
    }

    private class VolumeProcessor(private val volume: Float) : BaseAudioProcessor() {
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            return inputAudioFormat
        }
        override fun queueInput(inputBuffer: ByteBuffer) {
            val output = replaceOutputBuffer(inputBuffer.remaining())
            while (inputBuffer.remaining() >= 2) {
                output.putShort((inputBuffer.short * volume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
            output.flip()
        }
    }
}
