package com.notivault.app.service.media

import android.os.FileObserver
import java.io.File

@Suppress("DEPRECATION")
class MediaFileObserver(
    private val directory: File,
    private val packageName: String,
    private val onNewMediaFile: (File, String) -> Unit
) : FileObserver(
    directory.absolutePath,
    CLOSE_WRITE or MOVED_TO or CREATE
) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        if ((event and (CLOSE_WRITE or MOVED_TO or CREATE)) != 0) {
            val file = File(directory, path)
            // Skip .nomedia file specifically, but allow files in .Shared or hidden cache dirs
            if (file.name.equals(".nomedia", ignoreCase = true)) return
            if (file.exists() && file.isFile && file.length() > 0) {
                onNewMediaFile(file, packageName)
            }
        }
    }
}
