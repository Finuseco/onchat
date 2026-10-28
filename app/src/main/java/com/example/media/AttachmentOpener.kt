package com.example.media

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.api.AuthenticatedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.UUID

object AttachmentOpener {
    suspend fun open(context: Context, url: String) {
        val local = withContext(Dispatchers.IO) {
            val response = AuthenticatedMedia.client.newCall(Request.Builder().url(url).build()).execute()
            response.use {
                check(it.isSuccessful) { "Téléchargement impossible (HTTP ${it.code})" }
                val body = it.body ?: error("Fichier vide")
                val extension = android.net.Uri.parse(url).lastPathSegment?.substringAfterLast('.', "")
                    ?.takeIf { value -> value.matches(Regex("[A-Za-z0-9]{1,8}")) } ?: "bin"
                val directory = File(context.cacheDir, "attachments").apply { mkdirs() }
                val file = File(directory, "${UUID.randomUUID()}.$extension")
                try {
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var total = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                check(total <= 200L * 1024 * 1024) { "Fichier trop volumineux" }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    file
                } catch (e: Exception) { file.delete(); throw e }
            }
        }
        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", local)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(local.extension) ?: "application/octet-stream"
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Ouvrir le fichier"))
        }
    }
}
