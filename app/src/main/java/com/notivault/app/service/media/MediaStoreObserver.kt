package com.notivault.app.service.media

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class MediaStoreObserver(
    private val context: Context,
    private val onMediaDetected: (Uri, String, String, String, String, String) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val contentResolver: ContentResolver = context.contentResolver

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (uri == null) return

        try {
            val projection = mutableListOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.MediaColumns.RELATIVE_PATH)
                    add(MediaStore.MediaColumns.IS_PENDING)
                }
            }.toTypedArray()

            val isItemUri = uri.lastPathSegment?.toLongOrNull() != null
            val sortOrder = if (isItemUri) null else "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                var processed = 0
                val isPendingIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
                } else -1

                while (cursor.moveToNext() && processed < 5) {
                    if (isPendingIdx >= 0 && cursor.getInt(isPendingIdx) == 1) {
                        // File is currently being streamed to disk, wait for IS_PENDING = 0
                        continue
                    }
                    processed++
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val relPathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    } else -1

                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "unknown" else "unknown"
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "image/jpeg" else "image/jpeg"
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else null
                    val data = if (dataIdx >= 0) cursor.getString(dataIdx) ?: "" else ""
                    val relPath = if (relPathIdx >= 0) cursor.getString(relPathIdx) ?: "" else ""

                    val itemUri = if (isItemUri) uri else {
                        id?.let { Uri.withAppendedPath(uri, it.toString()) } ?: uri
                    }

                    val detectedPackage = resolveMessagingPackage(data, relPath, name) ?: continue
                    onMediaDetected(itemUri, name, mime, detectedPackage, data, relPath)

                    if (isItemUri) break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val BLACKLIST_PATTERNS = listOf(
            "screenshots",
            "screenshot",
            "dcim",
            "camera",
            ".thumbnails",
            "download",
            "downloads",
            "/sent/",
            "/sent"
        )

        fun isBlacklisted(data: String, relPath: String, name: String): Boolean {
            val combined = "$data/$relPath/$name".lowercase()
            return BLACKLIST_PATTERNS.any { combined.contains(it) }
        }

        fun resolveMessagingPackage(data: String, relPath: String, name: String): String? {
            val combined = "$data/$relPath/$name".lowercase()

            // 1. Explicitly ignore and skip screenshots, camera, dcim, thumbnails, downloads, sent media
            if (isBlacklisted(data, relPath, name)) {
                return null
            }

            // Standard WhatsApp filename pattern (e.g. VID-2023...-WA...mp4, IMG-...-WA...jpg)
            val lowerName = name.lowercase()
            val isWaFileName = (lowerName.startsWith("vid-") && lowerName.contains("-wa")) ||
                    (lowerName.startsWith("img-") && lowerName.contains("-wa")) ||
                    (lowerName.startsWith("aud-") && lowerName.contains("-wa")) ||
                    (lowerName.startsWith("ptt-") && lowerName.contains("-wa")) ||
                    lowerName.startsWith("whatsapp video") ||
                    lowerName.startsWith("whatsapp image")

            // 2. Strict directory & filename whitelist:
            return when {
                combined.contains("android/media/com.whatsapp.w4b") ||
                combined.contains("whatsapp business") -> "com.whatsapp.w4b"

                combined.contains("android/media/com.whatsapp") ||
                combined.contains("whatsapp/media") ||
                combined.contains("pictures/whatsapp") ||
                combined.contains("movies/whatsapp") ||
                combined.contains("whatsapp video") ||
                combined.contains("whatsapp images") ||
                combined.contains("whatsapp animated gifs") ||
                isWaFileName -> "com.whatsapp"

                combined.contains("android/media/org.telegram.messenger") ||
                combined.contains("telegram/") ||
                combined.contains("telegram images") ||
                combined.contains("telegram video") ||
                combined.contains("movies/telegram") ||
                combined.contains("pictures/telegram") -> "org.telegram.messenger"

                combined.contains("android/media/com.facebook.orca") ||
                combined.contains("pictures/messenger") ||
                combined.contains("movies/messenger") ||
                combined.contains("messenger/") -> "com.facebook.orca"

                combined.contains("android/media/com.instagram.android") ||
                combined.contains("pictures/instagram") ||
                combined.contains("movies/instagram") ||
                combined.contains("instagram/") -> "com.instagram.android"

                else -> null
            }
        }
    }
}
