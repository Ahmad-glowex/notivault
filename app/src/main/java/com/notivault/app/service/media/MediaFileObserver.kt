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
    CLOSE_WRITE or MOVED_TO
) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        if ((event and (CLOSE_WRITE or MOVED_TO)) != 0) {
            val file = File(directory, path)
            // Filter out temporary / hidden files (.nomedia, .tmp)
            if (!file.name.startsWith(".") && file.exists() && file.isFile && file.length() > 0) {
                onNewMediaFile(file, packageName)
            }
        }
    }
}
