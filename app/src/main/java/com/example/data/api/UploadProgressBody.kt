package com.example.data.api

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File

/** Progress reflects bytes written to the HTTP request, never a timer. */
class UploadProgressBody(private val file: File, private val mime: MediaType?, private val progress: (Long, Long) -> Unit) : RequestBody() {
    override fun contentType() = mime
    override fun contentLength() = file.length()
    override fun isOneShot() = true
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var sent = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                sink.write(buffer, 0, count)
                sink.emitCompleteSegments()
                sent += count
                progress(sent, total)
            }
        }
    }
}
