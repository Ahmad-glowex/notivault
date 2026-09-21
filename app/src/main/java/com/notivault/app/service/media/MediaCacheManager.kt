package com.notivault.app.service.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaCacheManager(private val context: Context) {

    private val mediaStorageDir: File by lazy {
        File(context.filesDir, "saved_media").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Copies a file from sourcePath to app-internal protected storage.
     * Returns the destination File or null on failure.
     */
    suspend fun cacheLocalFile(sourceFile: File, prefix: String = "media"): File? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || !sourceFile.canRead()) return@withContext null

        try {
            val extension = sourceFile.extension.ifEmpty { "bin" }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val targetName = "${prefix}_${timeStamp}.${extension}"
            val targetFile = File(mediaStorageDir, targetName)

            sourceFile.inputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies content from a content URI to app-internal protected storage.
     */
    suspend fun cacheContentUri(uri: Uri, mimeType: String?, prefix: String = "media"): File? = withContext(Dispatchers.IO) {
        try {
            val ext = when {
                mimeType?.contains("image/jpeg") == true -> "jpg"
                mimeType?.contains("image/png") == true -> "png"
                mimeType?.contains("image/webp") == true -> "webp"
                mimeType?.contains("video/mp4") == true -> "mp4"
                mimeType?.contains("audio/ogg") == true -> "ogg"
                mimeType?.contains("audio/mp4") == true -> "m4a"
                else -> "bin"
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val targetName = "${prefix}_${timeStamp}.${ext}"
            val targetFile = File(mediaStorageDir, targetName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
