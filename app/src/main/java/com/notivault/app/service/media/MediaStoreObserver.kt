package com.notivault.app.service.media

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class MediaStoreObserver(
    private val context: Context,
    private val onMediaDetected: (Uri, String, String) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val contentResolver: ContentResolver = context.contentResolver

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (uri == null) return

        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE
            )

            val isItemUri = uri.lastPathSegment?.toLongOrNull() != null
            val sortOrder = if (isItemUri) null else "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT 1"

            contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)

                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "unknown" else "unknown"
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "image/jpeg" else "image/jpeg"
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else null

                    val itemUri = if (isItemUri) uri else {
                        id?.let { Uri.withAppendedPath(uri, it.toString()) } ?: uri
                    }

                    onMediaDetected(itemUri, name, mime)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
