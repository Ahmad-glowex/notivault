package com.notivault.app.service.media

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import com.notivault.app.NotiVaultApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

object RootViewOnceManager {

    private const val TAG = "RootViewOnceManager"
    private var activeJob: Job? = null

    @Volatile
    private var cachedRootAvailable: Boolean? = null

    /**
     * Checks if root access (su binary) is available on this device without blocking indefinitely.
     */
    fun isRootAvailable(forceCheck: Boolean = false): Boolean {
        if (!forceCheck && cachedRootAvailable != null) {
            return cachedRootAvailable == true
        }

        // On non-Android host environments (e.g. desktop JVM unit tests), return false safely
        val isAndroid = System.getProperty("java.vm.name")?.contains("Dalvik", ignoreCase = true) == true ||
                File("/system/bin").exists() || File("/system/build.prop").exists()
        if (!isAndroid) {
            cachedRootAvailable = false
            return false
        }

        val standardPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        val hasPath = standardPaths.any { path ->
            try { File(path).exists() } catch (_: Exception) { false }
        }

        if (!hasPath) {
            val whichSu = try {
                val p = Runtime.getRuntime().exec(arrayOf("which", "su"))
                val finished = try {
                    p.waitFor(1, TimeUnit.SECONDS)
                } catch (_: NoSuchMethodError) {
                    p.waitFor() == 0
                }
                finished && p.exitValue() == 0
            } catch (_: Exception) { false }
            if (!whichSu) {
                cachedRootAvailable = false
                return false
            }
        }

        val isRoot = try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val finished = try {
                p.waitFor(2000, TimeUnit.MILLISECONDS)
            } catch (_: NoSuchMethodError) {
                var done = false
                val t = Thread {
                    try { p.waitFor(); done = true } catch (_: Exception) {}
                }
                t.start()
                t.join(2000)
                if (!done) p.destroy()
                done
            }
            if (finished && p.exitValue() == 0) {
                val output = p.inputStream.bufferedReader().use { it.readText() }
                output.contains("uid=0")
            } else {
                p.destroyForcibly()
                false
            }
        } catch (_: Exception) {
            false
        }

        cachedRootAvailable = isRoot
        return isRoot
    }

    /**
     * Triggers a continuous root-based sandbox extraction loop when a View-Once notification arrives.
     * Polls the WhatsApp ViewOnce directory for 30 seconds to capture media as soon as it is downloaded.
     */
    fun triggerCapture(context: Context, targetPackage: String = "com.whatsapp", scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        activeJob?.cancel()
        activeJob = scope.launch {
            if (!isRootAvailable()) {
                Log.d(TAG, "Root access is not available on this device. Skipping direct sandbox extraction.")
                return@launch
            }

            Log.d(TAG, "Root access detected. Starting sandbox sniffer for $targetPackage")
            var attempts = 0
            while (attempts < 30) { // 30 iterations @ 1s = 30 seconds window
                val capturedCount = extractViewOnceFiles(context, targetPackage)
                if (capturedCount > 0) {
                    Log.d(TAG, "Successfully extracted $capturedCount View-Once file(s) via Root sandbox.")
                    break
                }
                delay(1000)
                attempts++
            }
        }
    }

    /**
     * Performs a one-shot root extraction scan of the WhatsApp ViewOnce sandbox and databases.
     * Returns the count of newly extracted and attached media files.
     */
    suspend fun extractViewOnceFiles(context: Context, targetPackage: String = "com.whatsapp"): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? NotiVaultApp ?: return@withContext 0
        if (!isRootAvailable()) return@withContext 0

        val stagingDir = File(context.cacheDir, "root_vo_staging").apply { mkdirs() }
        val stagingPath = stagingDir.absolutePath
        val uid = context.applicationInfo.uid

        val candidateDirs = listOf(
            "/data/data/$targetPackage/files/ViewOnce",
            "/data/user/0/$targetPackage/files/ViewOnce",
            "/data/data/com.whatsapp/files/ViewOnce",
            "/data/user/0/com.whatsapp/files/ViewOnce",
            "/data/data/com.whatsapp.w4b/files/ViewOnce",
            "/data/user/0/com.whatsapp.w4b/files/ViewOnce",
            "/data/data/$targetPackage/cache",
            "/data/user/0/$targetPackage/cache",
            "/data/data/com.whatsapp/cache",
            "/data/user/0/com.whatsapp/cache"
        )

        val candidateDbDirs = listOf(
            "/data/data/$targetPackage/databases",
            "/data/user/0/$targetPackage/databases",
            "/data/data/com.whatsapp/databases",
            "/data/user/0/com.whatsapp/databases",
            "/data/data/com.whatsapp.w4b/databases",
            "/data/user/0/com.whatsapp.w4b/databases"
        )

        var newlyExtracted = 0

        try {
            // Build root script to copy all files from ViewOnce directories into staging with app UID ownership
            val sb = StringBuilder()
            sb.append("mkdir -p '").append(stagingPath).append("' && ")

            for (dir in candidateDirs) {
                sb.append("if [ -d '").append(dir).append("' ]; then ")
                sb.append("for f in '").append(dir).append("'/*; do ")
                sb.append("if [ -f \"\$f\" ]; then ")
                sb.append("cp -f \"\$f\" '").append(stagingPath).append("/' 2>/dev/null; ")
                sb.append("fi; done; ")
                sb.append("fi; ")
            }

            // Copy WhatsApp's msgstore.db as fallback for inline thumbnails
            for (dbDir in candidateDbDirs) {
                sb.append("if [ -f '").append(dbDir).append("/msgstore.db' ]; then ")
                sb.append("cp -f '").append(dbDir).append("/msgstore.db' '").append(stagingPath).append("/msgstore_dump.db' 2>/dev/null; ")
                sb.append("cp -f '").append(dbDir).append("/msgstore.db-wal' '").append(stagingPath).append("/msgstore_dump.db-wal' 2>/dev/null; ")
                sb.append("break; ")
                sb.append("fi; ")
            }

            sb.append("chown -R ").append(uid).append(":").append(uid).append(" '").append(stagingPath).append("' && ")
            sb.append("chmod -R 777 '").append(stagingPath).append("' && ")
            sb.append("chcon -R u:object_r:app_data_file:s0 '").append(stagingPath).append("' 2>/dev/null || true")

            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", sb.toString()))
            val finished = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(8, TimeUnit.SECONDS)
            } else {
                process.waitFor()
                true
            }
            if (!finished) {
                process.destroyForcibly()
            }

            // Now inspect staged files using normal process permissions
            val stagedFiles = stagingDir.listFiles() ?: emptyArray()
            val cacheManager = MediaCacheManager(context)
            val msgDao = app.database.messageDao()
            val now = System.currentTimeMillis()
            val pendingWindow = 4 * 3600 * 1000L

            for (file in stagedFiles) {
                if (!file.isFile || file.length() == 0L || file.name.startsWith("msgstore_dump")) {
                    continue
                }

                // Identify binary format (JPEG, PNG, MP4, etc.)
                val detected = MediaMagicDetector.detect(file)
                if (detected != null) {
                    val cachedFile = cacheManager.cacheLocalFile(file, prefix = "root_vo")
                    if (cachedFile != null) {
                        val targetMsg = msgDao.getLatestPendingMediaMessage(targetPackage, sinceTimestamp = now - pendingWindow)
                            ?: msgDao.getLatestPendingMediaMessage("com.whatsapp", sinceTimestamp = now - pendingWindow)

                        val threadId = targetMsg?.threadId ?: "${targetPackage}_view_once"
                        val messageId = targetMsg?.id
                        val finalMediaType = if (detected.mediaType == "VIDEO") "VIEW_ONCE_VIDEO" else "VIEW_ONCE_IMAGE"

                        if (targetMsg != null) {
                            msgDao.updateMessageMedia(targetMsg.id, cachedFile.absolutePath, detected.mimeType)
                            Log.d(TAG, "Linked extracted View-Once media ${cachedFile.name} to message ${targetMsg.id}")
                        }

                        app.mediaRepository.saveCachedMedia(
                            packageName = targetPackage,
                            originalPath = "root_view_once_${file.name}",
                            internalSavedPath = cachedFile.absolutePath,
                            fileName = cachedFile.name,
                            mimeType = detected.mimeType,
                            fileSizeBytes = cachedFile.length(),
                            mediaType = finalMediaType,
                            threadId = threadId,
                            messageId = messageId
                        )

                        newlyExtracted++
                    }
                }
                file.delete()
            }

            // Fallback: If no files were found directly in ViewOnce/, check msgstore_dump.db for inline JPEG thumbnails
            val dbFile = File(stagingPath, "msgstore_dump.db")
            if (newlyExtracted == 0 && dbFile.exists() && dbFile.length() > 0) {
                try {
                    val db = SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    )
                    val cursor = db.rawQuery(
                        "SELECT thumbnail, mime_type, file_path FROM message_media WHERE thumbnail IS NOT NULL ORDER BY message_row_id DESC LIMIT 5",
                        null
                    )
                    while (cursor.moveToNext()) {
                        val thumbBytes = cursor.getBlob(0)
                        val mime = cursor.getString(1) ?: "image/jpeg"
                        if (thumbBytes != null && thumbBytes.size > 200) {
                            // Verify standard JPEG magic bytes: FF D8 FF
                            if (thumbBytes[0] == 0xFF.toByte() && thumbBytes[1] == 0xD8.toByte() && thumbBytes[2] == 0xFF.toByte()) {
                                val tempThumb = File(stagingDir, "extracted_thumb_${System.currentTimeMillis()}.jpg")
                                tempThumb.writeBytes(thumbBytes)
                                val cached = cacheManager.cacheLocalFile(tempThumb, prefix = "root_vo_thumb")
                                tempThumb.delete()
                                if (cached != null) {
                                    val targetMsg = msgDao.getLatestPendingMediaMessage(targetPackage, sinceTimestamp = now - pendingWindow)
                                        ?: msgDao.getLatestPendingMediaMessage("com.whatsapp", sinceTimestamp = now - pendingWindow)
                                    val threadId = targetMsg?.threadId ?: "${targetPackage}_view_once"
                                    val messageId = targetMsg?.id

                                    if (targetMsg != null) {
                                        msgDao.updateMessageMedia(targetMsg.id, cached.absolutePath, mime)
                                        Log.d(TAG, "Linked extracted View-Once thumbnail ${cached.name} to message ${targetMsg.id}")
                                    }

                                    app.mediaRepository.saveCachedMedia(
                                        packageName = targetPackage,
                                        originalPath = "root_view_once_db_${cached.name}",
                                        internalSavedPath = cached.absolutePath,
                                        fileName = cached.name,
                                        mimeType = mime,
                                        fileSizeBytes = cached.length(),
                                        mediaType = "VIEW_ONCE_IMAGE",
                                        threadId = threadId,
                                        messageId = messageId
                                    )
                                    newlyExtracted++
                                    break
                                }
                            }
                        }
                    }
                    cursor.close()
                    db.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error inspecting msgstore dump for thumbnails", e)
                }
            }

            // Cleanup staging folder
            stagingDir.listFiles()?.forEach { try { it.delete() } catch (_: Exception) {} }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root ViewOnce extraction", e)
        }

        newlyExtracted
    }
}
