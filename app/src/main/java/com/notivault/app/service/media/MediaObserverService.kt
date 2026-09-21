package com.notivault.app.service.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
    private var activeSnifferJob: Job? = null

    companion object {
        const val CHANNEL_ID = "notivault_media_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SCAN_NOW = "com.notivault.app.ACTION_SCAN_NOW"
        const val ACTION_SNIFF_VIEW_ONCE = "com.notivault.app.ACTION_SNIFF_VIEW_ONCE"
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"

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

        fun scanNow(context: Context, targetPackage: String? = null) {
            try {
                val intent = Intent(context, MediaObserverService::class.java).apply {
                    action = ACTION_SCAN_NOW
                    putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun triggerViewOnceSniff(context: Context, targetPackage: String = "com.whatsapp") {
            try {
                val intent = Intent(context, MediaObserverService::class.java).apply {
                    action = ACTION_SNIFF_VIEW_ONCE
                    putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                }
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
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        setupObservers()
        scanRecentMediaStore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN_NOW -> {
                val targetPkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
                scanTargetDirectories(targetPkg)
            }
            ACTION_SNIFF_VIEW_ONCE -> {
                val targetPkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: "com.whatsapp"
                startHighFrequencySniffer(targetPkg)
            }
            else -> {
                setupObservers()
                scanRecentMediaStore()
            }
        }
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
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/.trash")),
                // WhatsApp Images
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/.trash")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Images")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Images/Private")),
                Pair("com.whatsapp", File(baseExternal, "Pictures/WhatsApp")),
                // WhatsApp Video
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Private")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/.trash")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Video")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Video/Private")),
                Pair("com.whatsapp", File(baseExternal, "Movies/WhatsApp")),
                // WhatsApp Audio & Voice Notes
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/Private")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Audio")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Audio/Private")),
                Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes")),
                Pair("com.whatsapp", File(baseExternal, "WhatsApp/Media/WhatsApp Voice Notes")),
                // WhatsApp Business
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Shared")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Video")),
                Pair("com.whatsapp.w4b", File(baseExternal, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Audio")),
                // Telegram
                Pair("org.telegram.messenger", File(baseExternal, "Telegram/Telegram Images")),
                Pair("org.telegram.messenger", File(baseExternal, "Telegram/Telegram Video")),
                Pair("org.telegram.messenger", File(baseExternal, "Telegram/Telegram Audio")),
                Pair("org.telegram.messenger", File(baseExternal, "Telegram/Telegram Documents")),
                Pair("org.telegram.messenger", File(baseExternal, "Pictures/Telegram")),
                Pair("org.telegram.messenger", File(baseExternal, "Movies/Telegram")),
                Pair("org.telegram.messenger", File(baseExternal, "Android/media/org.telegram.messenger/Telegram/Telegram Images")),
                Pair("org.telegram.messenger", File(baseExternal, "Android/media/org.telegram.messenger/Telegram/Telegram Video")),
                // Messenger
                Pair("com.facebook.orca", File(baseExternal, "Pictures/Messenger")),
                Pair("com.facebook.orca", File(baseExternal, "Movies/Messenger")),
                Pair("com.facebook.orca", File(baseExternal, "Android/media/com.facebook.orca")),
                // Instagram
                Pair("com.instagram.android", File(baseExternal, "Pictures/Instagram")),
                Pair("com.instagram.android", File(baseExternal, "Movies/Instagram")),
                Pair("com.instagram.android", File(baseExternal, "Android/media/com.instagram.android"))
            )
        )

        for ((pkg, dir) in pathsToWatch) {
            watchDirectory(dir, pkg)
        }

        scanExistingMediaDirectories(pathsToWatch)
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

    private fun startHighFrequencySniffer(targetPackage: String) {
        activeSnifferJob?.cancel()
        activeSnifferJob = serviceScope.launch {
            val baseExternal = Environment.getExternalStorageDirectory()
            val stagingDirs = listOf(
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media/.Shared"),
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media/.trash"),
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media/WhatsApp Images"),
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media/WhatsApp Images/Private"),
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media/WhatsApp Video"),
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media/WhatsApp Video/Private"),
                File(baseExternal, "Android/media/$targetPackage/WhatsApp/Media")
            )

            // High-frequency polling loop: runs for 60 seconds (200 iterations @ 300ms)
            var count = 0
            while (count < 200) {
                for (dir in stagingDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles() ?: continue
                        for (file in files) {
                            if (file.isFile && file.length() > 0 && !file.name.equals(".nomedia", ignoreCase = true)) {
                                handleNewMediaFile(file, targetPackage)
                            }
                        }
                    }
                }
                delay(300)
                count++
            }
        }
    }

    private fun scanTargetDirectories(targetPackage: String? = null) {
        serviceScope.launch {
            val listToScan = if (!targetPackage.isNullOrBlank()) {
                pathsToWatch.filter { it.first == targetPackage }
            } else {
                pathsToWatch
            }
            scanExistingMediaDirectories(listToScan)
            scanRecentMediaStore()
        }
    }

    private fun scanExistingMediaDirectories(paths: List<Pair<String, File>>) {
        serviceScope.launch {
            for ((pkg, dir) in paths) {
                try {
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles { f ->
                            f.isFile && !f.name.equals(".nomedia", ignoreCase = true) && f.length() > 0 &&
                                    !MediaStoreObserver.isBlacklisted(f.absolutePath, "", f.name)
                        } ?: continue

                        files.sortedByDescending { it.lastModified() }
                            .take(25)
                            .forEach { file ->
                                handleNewMediaFile(file, pkg)
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun scanRecentMediaStore() {
        serviceScope.launch {
            try {
                val projection = mutableListOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATA
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.MediaColumns.RELATIVE_PATH)
                    }
                }.toTypedArray()

                val sinceSecs = (System.currentTimeMillis() - 30 * 60 * 1000L) / 1000L
                val selection = "${MediaStore.MediaColumns.DATE_ADDED} >= ?"
                val selectionArgs = arrayOf(sinceSecs.toString())

                val uris = listOf(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                for (baseUri in uris) {
                    contentResolver.query(
                        baseUri,
                        projection,
                        selection,
                        selectionArgs,
                        "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                        val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                        val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        val relPathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        } else -1

                        var count = 0
                        while (cursor.moveToNext() && count < 10) {
                            count++
                            val id = cursor.getLong(idIdx)
                            val name = cursor.getString(nameIdx) ?: if (baseUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) "recent_video.mp4" else "recent_photo.jpg"
                            val mime = cursor.getString(mimeIdx) ?: if (baseUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) "video/mp4" else "image/jpeg"
                            val data = if (dataIdx >= 0) cursor.getString(dataIdx) ?: "" else ""
                            val relPath = if (relPathIdx >= 0) cursor.getString(relPathIdx) ?: "" else ""

                            val itemUri = Uri.withAppendedPath(baseUri, id.toString())
                            val detectedPackage = MediaStoreObserver.resolveMessagingPackage(data, relPath, name) ?: continue
                            handleMediaStoreChange(itemUri, name, mime, detectedPackage, data, relPath)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleNewMediaFile(file: File, packageName: String) {
        serviceScope.launch {
            val app = application as? NotiVaultApp ?: return@launch
            if (MediaStoreObserver.isBlacklisted(file.absolutePath, "", file.name)) return@launch

            // Check if app is enabled by user in settings
            val appEntity = app.database.appDao().getApp(packageName)
            if (appEntity != null && !appEntity.isEnabled) return@launch

            // Check if already backed up before writing a new copy to disk
            val existing = app.mediaRepository.getMediaByOriginalPath(file.absolutePath)
            if (existing != null) return@launch

            // Wait if file is currently being written/flushed to disk (e.g. video chunks)
            var attempts = 0
            var lastLen = -1L
            while (attempts < 15 && file.exists()) {
                val len = file.length()
                if (len > 0 && len == lastLen) break
                lastLen = len
                delay(300)
                attempts++
            }

            // Detect binary magic bytes (JPEG, PNG, MP4, WebP, Audio)
            val detected = MediaMagicDetector.detect(file) ?: return@launch

            val cachedFile = cacheManager.cacheLocalFile(file, prefix = packageName.replace(".", "_"))
            if (cachedFile != null) {
                val now = System.currentTimeMillis()
                val pendingWindow = 4 * 3600 * 1000L // 4 hours window for pending media
                val msgDao = app.database.messageDao()
                val targetMsg = if (detected.mediaType == "VIDEO") {
                    msgDao.getLatestPendingVideoMessage(packageName, sinceTimestamp = now - pendingWindow)
                        ?: msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
                        ?: msgDao.getLatestMessageForPackage(packageName, sinceTimestamp = now - 1800000L)
                } else {
                    msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
                        ?: msgDao.getLatestMessageForPackage(packageName, sinceTimestamp = now - 1800000L)
                }

                val isViewOnce = (targetMsg?.messageText?.let {
                    it.contains("①") || it.contains("\u2460") || it.contains("view once", ignoreCase = true) || it.contains("একবার দেখার")
                } == true) || file.absolutePath.contains(".Shared") || file.absolutePath.contains(".trash")

                val finalMediaType = if (isViewOnce) {
                    if (detected.mediaType == "VIDEO") "VIEW_ONCE_VIDEO" else "VIEW_ONCE_IMAGE"
                } else {
                    detected.mediaType
                }

                val threadId = targetMsg?.threadId
                val messageId = targetMsg?.id

                if (targetMsg != null) {
                    msgDao.updateMessageMedia(targetMsg.id, cachedFile.absolutePath, detected.mimeType)
                }

                app.mediaRepository.saveCachedMedia(
                    packageName = packageName,
                    originalPath = file.absolutePath,
                    internalSavedPath = cachedFile.absolutePath,
                    fileName = cachedFile.name,
                    mimeType = detected.mimeType,
                    fileSizeBytes = cachedFile.length(),
                    mediaType = finalMediaType,
                    threadId = threadId,
                    messageId = messageId
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
            // Check if already backed up before writing a new copy to disk
            val existing = app.mediaRepository.getMediaByOriginalPath(originalPath)
            if (existing != null) return@launch

            val cachedFile = cacheManager.cacheContentUri(uri, mime, prefix = packageName.replace(".", "_"))
            if (cachedFile != null) {
                val mediaType = if (mime.startsWith("video") || cachedFile.extension in listOf("mp4", "mkv", "3gp", "webm")) "VIDEO" else "IMAGE"
                val resolvedMime = if (mediaType == "VIDEO" && !mime.startsWith("video")) "video/mp4" else mime
                val now = System.currentTimeMillis()
                val pendingWindow = 4 * 3600 * 1000L // 4 hours window for pending media

                val msgDao = app.database.messageDao()
                val targetMsg = if (mediaType == "VIDEO") {
                    msgDao.getLatestPendingVideoMessage(packageName, sinceTimestamp = now - pendingWindow)
                        ?: msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
                        ?: msgDao.getLatestMessageForPackage(packageName, sinceTimestamp = now - 1800000L)
                } else {
                    msgDao.getLatestPendingMediaMessage(packageName, sinceTimestamp = now - pendingWindow)
                        ?: msgDao.getLatestMessageForPackage(packageName, sinceTimestamp = now - 1800000L)
                }

                val isViewOnce = (targetMsg?.messageText?.let {
                    it.contains("①") || it.contains("\u2460") || it.contains("view once", ignoreCase = true) || it.contains("একবার দেখার")
                } == true) || originalPath.contains(".Shared") || originalPath.contains(".trash")

                val finalMediaType = if (isViewOnce) {
                    if (mediaType == "VIDEO") "VIEW_ONCE_VIDEO" else "VIEW_ONCE_IMAGE"
                } else {
                    mediaType
                }

                val threadId = targetMsg?.threadId
                val messageId = targetMsg?.id

                if (targetMsg != null) {
                    msgDao.updateMessageMedia(targetMsg.id, cachedFile.absolutePath, resolvedMime)
                }

                app.mediaRepository.saveCachedMedia(
                    packageName = packageName,
                    originalPath = originalPath,
                    internalSavedPath = cachedFile.absolutePath,
                    fileName = cachedFile.name,
                    mimeType = resolvedMime,
                    fileSizeBytes = cachedFile.length(),
                    mediaType = finalMediaType,
                    threadId = threadId,
                    messageId = messageId
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
