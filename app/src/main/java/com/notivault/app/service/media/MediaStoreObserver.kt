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
    private val onMediaDetected: (Uri, String, String, String) -> Unit
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
                }
            }.toTypedArray()

            val isItemUri = uri.lastPathSegment?.toLongOrNull() != null
            val sortOrder = if (isItemUri) null else "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                var processed = 0
                while (cursor.moveToNext() && processed < 5) {
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

                    val detectedPackage = resolvePackage(data, relPath, name)
                    onMediaDetected(itemUri, name, mime, detectedPackage)

                    if (isItemUri) break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resolvePackage(data: String, relPath: String, name: String): String {
        val combined = "$data/$relPath/$name".lowercase()
        return when {
            combined.contains("com.whatsapp.w4b") -> "com.whatsapp.w4b"
            combined.contains("whatsapp") || combined.contains("img-") || combined.contains("vid-") && combined.contains("wa") -> "com.whatsapp"
            combined.contains("telegram") -> "org.telegram.messenger"
            combined.contains("messenger") || combined.contains("facebook") -> "com.facebook.orca"
            combined.contains("instagram") -> "com.instagram.android"
            else -> "unknown.mediastore"
        }
    }
}
