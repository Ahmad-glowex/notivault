package com.notivault.app.service.media

import java.io.File
import java.io.FileInputStream

object MediaMagicDetector {

    data class MediaInfo(
        val mimeType: String,
        val extension: String,
        val mediaType: String
    )

    fun detect(file: File): MediaInfo? {
        if (!file.exists() || !file.isFile || file.length() < 8) return null

        try {
            val header = ByteArray(32)
            val bytesRead = FileInputStream(file).use { it.read(header) }
            if (bytesRead >= 8) {
                // 1. JPEG: FF D8 FF
                if ((header[0].toInt() and 0xFF) == 0xFF &&
                    (header[1].toInt() and 0xFF) == 0xD8 &&
                    (header[2].toInt() and 0xFF) == 0xFF
                ) {
                    return MediaInfo("image/jpeg", "jpg", "IMAGE")
                }

                // 2. PNG: 89 50 4E 47
                if ((header[0].toInt() and 0xFF) == 0x89 &&
                    (header[1].toInt() and 0xFF) == 0x50 &&
                    (header[2].toInt() and 0xFF) == 0x4E &&
                    (header[3].toInt() and 0xFF) == 0x47
                ) {
                    return MediaInfo("image/png", "png", "IMAGE")
                }

                // 3. GIF: GIF87a / GIF89a
                if (header[0] == 'G'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte()
                ) {
                    return MediaInfo("image/gif", "gif", "IMAGE")
                }

                // 4. WebP: RIFF .... WEBP
                if (bytesRead >= 12 &&
                    header[0] == 'R'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() &&
                    header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() &&
                    header[9] == 'E'.code.toByte() &&
                    header[10] == 'B'.code.toByte() &&
                    header[11] == 'P'.code.toByte()
                ) {
                    return MediaInfo("image/webp", "webp", "IMAGE")
                }

                // 5. MP4 / MOV: check for 'ftyp' in first 16 bytes
                for (i in 4..minOf(bytesRead - 4, 16)) {
                    if (header[i] == 'f'.code.toByte() &&
                        header[i + 1] == 't'.code.toByte() &&
                        header[i + 2] == 'y'.code.toByte() &&
                        header[i + 3] == 'p'.code.toByte()
                    ) {
                        return MediaInfo("video/mp4", "mp4", "VIDEO")
                    }
                }

                // 6. Ogg Audio: OggS
                if (header[0] == 'O'.code.toByte() &&
                    header[1] == 'g'.code.toByte() &&
                    header[2] == 'g'.code.toByte() &&
                    header[3] == 'S'.code.toByte()
                ) {
                    return MediaInfo("audio/ogg", "opus", "AUDIO")
                }

                // 7. MP3 Audio: ID3
                if (header[0] == 'I'.code.toByte() &&
                    header[1] == 'D'.code.toByte() &&
                    header[2] == '3'.code.toByte()
                ) {
                    return MediaInfo("audio/mpeg", "mp3", "AUDIO")
                }
            }
        } catch (_: Exception) {
            // Ignore stream read errors and fallback to extension
        }

        // Fallback detection by file extension
        val ext = file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg" -> MediaInfo("image/jpeg", "jpg", "IMAGE")
            "png" -> MediaInfo("image/png", "png", "IMAGE")
            "webp" -> MediaInfo("image/webp", "webp", "IMAGE")
            "gif" -> MediaInfo("image/gif", "gif", "IMAGE")
            "mp4", "mkv", "3gp", "webm" -> MediaInfo("video/mp4", if (ext.isNotBlank()) ext else "mp4", "VIDEO")
            "mp3", "ogg", "m4a", "opus", "wav" -> MediaInfo("audio/$ext", ext, "AUDIO")
            else -> null
        }
    }
}
