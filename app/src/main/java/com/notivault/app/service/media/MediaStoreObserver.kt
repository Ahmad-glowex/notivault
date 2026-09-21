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
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "unknown" else "unknown"
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "image/jpeg" else "image/jpeg"
                    val path = if (dataIdx >= 0) cursor.getString(dataIdx) ?: "" else ""

                    onMediaDetected(uri, name, mime)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
