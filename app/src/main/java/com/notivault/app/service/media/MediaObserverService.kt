package com.notivault.app.service.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.notivault.app.ui.MainActivity
import com.notivault.app.NotiVaultApp
import com.notivault.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MediaObserverService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var cacheManager: MediaCacheManager
    private val fileObservers = mutableListOf<MediaFileObserver>()
    private var mediaStoreObserver: MediaStoreObserver? = null
    private val pathsToWatch = mutableListOf<Pair<String, File>>()

    companion object {
        const val CHANNEL_ID = "notivault_media_service_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            try {
                val intent = Intent(context, MediaObserverService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MediaObserverService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        cacheManager = MediaCacheManager(this)
        createNotificationChannel()
        startForegroundSafely()
        setupObservers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fileObservers.forEach { it.stopWatching() }
        fileObservers.clear()

        mediaStoreObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }

        serviceScope.cancel()
    }

    private fun startForegroundSafely() {
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupObservers() {
        // 1. Setup MediaStore ContentObserver
        if (mediaStoreObserver == null) {
            val observer = MediaStoreObserver(this) { uri, name, mime, packageName, data, relPath ->
                handleMediaStoreChange(uri, name, mime, packageName, data, relPath)
            }
            this.mediaStoreObserver = observer
            try {
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Setup FileObservers for WhatsApp and standard messaging folders
        fileObservers.forEach { it.stopWatching() }
        fileObservers.clear()

        val baseExternal = Environment.getExternalStorageDirectory()
        pathsToWatch.clear()
        pathsToWatch.addAll(
            listOf(
                // WhatsApp Root & Staging Media Folders
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/.Shared")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/.trash")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Private")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes")),
                // WhatsApp Business
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Shared")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Video")),
                // Telegram
                Pair("org.telegram.messenger", File(baseExternal, "Android/media/org.telegram.messenger/Telegram/Telegram Images")),
                Pair("org.telegram.messenger", File(baseExternal, "Android/media/org.telegram.messenger/Telegram/Telegram Video")),
                // Messenger
                Pair("com.facebook.orca", File(baseExternal, "Android/media/com.facebook.orca")),
                // Instagram
                Pair("com.instagram.android", File(baseExternal, "Android/media/com.instagram.android"))
            )
        )

        for ((pkg, dir) in pathsToWatch) {
            watchDirectory(dir, pkg)
        }
    }

    private fun watchDirectory(dir: File, pkg: String) {
        try {
            if (dir.exists() && dir.isDirectory) {
                val observer = MediaFileObserver(
                    directory = dir,
                    packageName = pkg,
                    onNewMediaFile = { file, packageName ->
                        handleNewMediaFile(file, packageName)
                    },
                    onNewDirectory = { newDir, packageName ->
                        watchDirectory(newDir, packageName)
                    }
                )
                observer.startWatching()
                fileObservers.add(observer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNewMediaFile(file: File, packageName: String) {
        serviceScope.launch {
            val app = application as? NotiVaultApp ?: return@launch
            if (MediaStoreObserver.isBlacklisted(file.absolutePath, "", file.name)) return@launch

            // Check if app is enabled by user in settings
            val appEntity = app.database.appDao().getApp(packageName)
            if (appEntity != null && !appEntity.isEnabled) return@launch

            // CRITICAL SAFEGUARD: Never copy old existing files from phone storage!
            // Only process files modified within the last 15 minutes (or 60s future clock skew)
            val fileAge = System.currentTimeMillis() - file.lastModified()
            if (fileAge > 900_000L || fileAge < -60_000L) {
                return@launch
            }

            // Check if already backed up before writing a new copy to disk
            val existing = app.mediaRepository.getMediaByOriginalPath(file.absolutePath)
            if (existing != null) return@launch

            // Detect binary magic bytes (JPEG, PNG, MP4, WebP, Audio)
            val detected = MediaMagicDetector.detect(file) ?: return@launch

            val now = System.currentTimeMillis()
            val pendingWindow = 4 * 3600 * 1000L // 4 hours window for pending media
            val msgDao = app.database.messageDao()
            val targetMsg = if (detected.mediaType == "VIDEO") {
                msgDao.getLatestPendingVideoMessage(packageName, sinceTimestamp = now - pendingWindow)
                    ?: msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
            } else {
                msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
            }

            // CRITICAL PROTECTION: Only copy media if there is an active incoming message waiting for it!
            if (targetMsg == null) {
                return@launch
            }

            // Wait if file is currently being written/flushed to disk (e.g. video chunks)
            var attempts = 0
            var lastLen = -1L
            while (attempts < 10 && file.exists()) {
                val len = file.length()
                if (len > 0 && len == lastLen) break
                lastLen = len
                delay(300)
                attempts++
            }

            val cachedFile = cacheManager.cacheLocalFile(file, prefix = packageName.replace(".", "_"))
            if (cachedFile != null) {
                msgDao.updateMessageMedia(targetMsg.id, cachedFile.absolutePath, detected.mimeType)

                app.mediaRepository.saveCachedMedia(
                    packageName = packageName,
                    originalPath = file.absolutePath,
                    internalSavedPath = cachedFile.absolutePath,
                    fileName = cachedFile.name,
                    mimeType = detected.mimeType,
                    fileSizeBytes = cachedFile.length(),
                    mediaType = detected.mediaType,
                    threadId = targetMsg.threadId,
                    messageId = targetMsg.id
                )
            }
        }
    }

    private fun handleMediaStoreChange(
        uri: Uri,
        name: String,
        mime: String,
        packageName: String,
        data: String,
        relPath: String
    ) {
        serviceScope.launch {
            val app = application as? NotiVaultApp ?: return@launch
            if (packageName.isBlank() || packageName == "unknown.mediastore") return@launch
            if (MediaStoreObserver.isBlacklisted(data, relPath, name)) return@launch

            // Check if app is enabled by user in settings
            val appEntity = app.database.appDao().getApp(packageName)
            if (appEntity != null && !appEntity.isEnabled) return@launch

            val originalPath = data.ifBlank { uri.toString() }
            val existing = app.mediaRepository.getMediaByOriginalPath(originalPath)
            if (existing != null) return@launch

            val now = System.currentTimeMillis()
            val pendingWindow = 4 * 3600 * 1000L // 4 hours window for pending media
            val msgDao = app.database.messageDao()
            val mediaType = if (mime.startsWith("video")) "VIDEO" else "IMAGE"

            val targetMsg = if (mediaType == "VIDEO") {
                msgDao.getLatestPendingVideoMessage(packageName, sinceTimestamp = now - pendingWindow)
                    ?: msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
            } else {
                msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
            }

            // CRITICAL PROTECTION: Never copy media unless there is an active incoming message waiting for it!
            if (targetMsg == null) {
                return@launch
            }

            val cachedFile = cacheManager.cacheContentUri(uri, mime, prefix = packageName.replace(".", "_"))
            if (cachedFile != null) {
                val resolvedMime = if (mediaType == "VIDEO" && !mime.startsWith("video")) "video/mp4" else mime

                msgDao.updateMessageMedia(targetMsg.id, cachedFile.absolutePath, resolvedMime)

                app.mediaRepository.saveCachedMedia(
                    packageName = packageName,
                    originalPath = originalPath,
                    internalSavedPath = cachedFile.absolutePath,
                    fileName = cachedFile.name,
                    mimeType = resolvedMime,
                    fileSizeBytes = cachedFile.length(),
                    mediaType = mediaType,
                    threadId = targetMsg.threadId,
                    messageId = targetMsg.id
                )
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.media_service_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
