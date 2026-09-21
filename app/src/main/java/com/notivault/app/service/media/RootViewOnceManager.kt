package com.notivault.app.service.media

import android.content.Context
import android.util.Log
import com.notivault.app.NotiVaultApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object RootViewOnceManager {

    private const val TAG = "RootViewOnceManager"
    private var activeJob: Job? = null

    /**
     * Checks if root access (su binary) is available on this device.
     */
    fun isRootAvailable(): Boolean {
        val standardPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        for (path in standardPaths) {
            if (File(path).exists()) return true
        }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            exitCode == 0 && output.contains("uid=0")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Triggers a continuous root-based sandbox extraction loop when a View-Once notification arrives.
     * Polls the WhatsApp ViewOnce directory for 30 seconds to capture media as soon as it is decrypted.
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
     * Performs a one-shot root extraction scan of the WhatsApp ViewOnce sandbox.
     * Returns the count of newly extracted and attached media files.
     */
    suspend fun extractViewOnceFiles(context: Context, targetPackage: String = "com.whatsapp"): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? NotiVaultApp ?: return@withContext 0
        val stagingDir = File(context.cacheDir, "root_view_once_staging").apply { mkdirs() }
        val stagingPath = stagingDir.absolutePath

        val candidateDirs = listOf(
            "/data/data/$targetPackage/files/ViewOnce",
            "/data/data/com.whatsapp/files/ViewOnce",
            "/data/data/com.whatsapp.w4b/files/ViewOnce"
        )

        var newlyExtracted = 0

        try {
            // Build root command to copy all files from ViewOnce to stagingDir with read permissions
            val sb = StringBuilder()
            sb.append("mkdir -p '").append(stagingPath).append("' && ")
            sb.append("chmod 777 '").append(stagingPath).append("' && ")

            for (dir in candidateDirs) {
                sb.append("if [ -d '").append(dir).append("' ]; then ")
                sb.append("for f in '").append(dir).append("'/*; do ")
                sb.append("if [ -f \"\$f\" ]; then ")
                sb.append("cp -f \"\$f\" '").append(stagingPath).append("/' && ")
                sb.append("chmod 666 '").append(stagingPath).append("/'\$(basename \"\$f\"); ")
                sb.append("fi; done; ")
                sb.append("fi; ")
            }

            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", sb.toString()))
            process.waitFor()

            // Now inspect staged files using normal process permissions
            val stagedFiles = stagingDir.listFiles() ?: emptyArray()
            val cacheManager = MediaCacheManager(context)
            val msgDao = app.database.messageDao()
            val now = System.currentTimeMillis()
            val pendingWindow = 4 * 3600 * 1000L

            for (file in stagedFiles) {
                if (!file.isFile || file.length() == 0L) {
                    file.delete()
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
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root ViewOnce extraction", e)
        }

        newlyExtracted
    }
}
