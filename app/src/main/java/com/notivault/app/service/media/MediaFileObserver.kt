package com.notivault.app.service.media

import android.os.Build
import android.os.FileObserver
import java.io.File

class MediaFileObserver(
    private val directory: File,
    private val packageName: String,
    private val onNewMediaFile: (File, String) -> Unit
) : FileObserver(
    directory.absolutePath,
    CREATE or CLOSE_WRITE or MOVED_TO
) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        val mask = event and ALL_EVENTS
        if (mask == CREATE || mask == CLOSE_WRITE || mask == MOVED_TO) {
            val file = File(directory, path)
            // Filter out temporary / hidden files (.nomedia, .tmp)
            if (!file.name.startsWith(".") && file.exists() && file.isFile && file.length() > 0) {
                onNewMediaFile(file, packageName)
            }
        }
    }
}
