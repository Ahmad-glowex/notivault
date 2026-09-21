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
import com.notivault.app.NotiVaultApp
import com.notivault.app.R
import com.notivault.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class MediaObserverService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var cacheManager: MediaCacheManager
    private val fileObservers = mutableListOf<MediaFileObserver>()
    private var mediaStoreObserver: MediaStoreObserver? = null

    companion object {
        const val CHANNEL_ID = "notivault_media_service_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, MediaObserverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaObserverService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        cacheManager = MediaCacheManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        setupObservers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        val mediaStoreObserver = MediaStoreObserver(this) { uri, name, mime ->
            handleMediaStoreChange(uri, name, mime)
        }
        this.mediaStoreObserver = mediaStoreObserver
        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver
            )
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Setup FileObservers for WhatsApp and standard messaging folders
        val baseExternal = Environment.getExternalStorageDirectory()
        val pathsToWatch = listOf(
            Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")),
            Pair("com.whatsapp", File(baseExternal, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video")),
            Pair("org.telegram.messenger", File(baseExternal, "Telegram/Telegram Images")),
            Pair("org.telegram.messenger", File(baseExternal, "Pictures/Telegram"))
        )

        for ((pkg, dir) in pathsToWatch) {
            if (dir.exists() && dir.isDirectory) {
                val observer = MediaFileObserver(dir, pkg) { file, packageName ->
                    handleNewMediaFile(file, packageName)
                }
                observer.startWatching()
                fileObservers.add(observer)
            }
        }
    }

    private fun handleNewMediaFile(file: File, packageName: String) {
        serviceScope.launch {
            val app = application as? NotiVaultApp ?: return@launch

            // Check if already backed up before writing a new copy to disk
            val existing = app.mediaRepository.getMediaByOriginalPath(file.absolutePath)
            if (existing != null) return@launch

            val cachedFile = cacheManager.cacheLocalFile(file, prefix = packageName.replace(".", "_"))
            if (cachedFile != null) {
                val mediaType = when {
                    cachedFile.extension in listOf("jpg", "jpeg", "png", "webp", "gif") -> "IMAGE"
                    cachedFile.extension in listOf("mp4", "mkv", "3gp", "webm") -> "VIDEO"
                    cachedFile.extension in listOf("mp3", "ogg", "m4a", "wav") -> "AUDIO"
                    else -> "DOCUMENT"
                }

                app.mediaRepository.saveCachedMedia(
                    packageName = packageName,
                    originalPath = file.absolutePath,
                    internalSavedPath = cachedFile.absolutePath,
                    fileName = cachedFile.name,
                    mimeType = "image/${cachedFile.extension}",
                    fileSizeBytes = cachedFile.length(),
                    mediaType = mediaType
                )
            }
        }
    }

    private fun handleMediaStoreChange(uri: Uri, name: String, mime: String) {
        serviceScope.launch {
            val app = application as? NotiVaultApp ?: return@launch

            // Check if already backed up before writing a new copy to disk
            val existing = app.mediaRepository.getMediaByOriginalPath(uri.toString())
            if (existing != null) return@launch

            val cachedFile = cacheManager.cacheContentUri(uri, mime, prefix = "mediastore")
            if (cachedFile != null) {
                val mediaType = if (mime.startsWith("video")) "VIDEO" else "IMAGE"
                app.mediaRepository.saveCachedMedia(
                    packageName = "unknown.mediastore",
                    originalPath = uri.toString(),
                    internalSavedPath = cachedFile.absolutePath,
                    fileName = cachedFile.name,
                    mimeType = mime,
                    fileSizeBytes = cachedFile.length(),
                    mediaType = mediaType
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
