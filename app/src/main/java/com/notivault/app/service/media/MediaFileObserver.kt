package com.notivault.app.service.media

import android.os.FileObserver
import java.io.File

@Suppress("DEPRECATION")
class MediaFileObserver(
    private val directory: File,
    private val packageName: String,
    private val onNewMediaFile: (File, String) -> Unit,
    private val onNewDirectory: ((File, String) -> Unit)? = null
) : FileObserver(
    directory.absolutePath,
    CLOSE_WRITE or MOVED_TO or CREATE
) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        val file = File(directory, path)
        if (file.isDirectory) {
            onNewDirectory?.invoke(file, packageName)
            return
        }
        if ((event and (CLOSE_WRITE or MOVED_TO or CREATE)) != 0) {
            if (file.exists() && file.isFile && !file.name.equals(".nomedia", ignoreCase = true)) {
                onNewMediaFile(file, packageName)
            }
        }
    }
}
