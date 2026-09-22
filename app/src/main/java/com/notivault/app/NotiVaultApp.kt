package com.notivault.app

import android.app.Application
import com.notivault.app.data.local.AppDatabase
import com.notivault.app.data.repository.MediaRepository
import com.notivault.app.data.repository.MediaRepositoryImpl
import com.notivault.app.data.repository.MessageRepository
import com.notivault.app.data.repository.MessageRepositoryImpl
import com.notivault.app.data.repository.SettingsRepository
import com.notivault.app.data.repository.SettingsRepositoryImpl
import com.notivault.app.service.media.MediaStoreObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NotiVaultApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepositoryImpl(database)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Asynchronously purge any bulk-scanned orphan media, deduplicate messages, and register default apps
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Immediately reclaim phone storage by deleting any bulk-cloned gallery media
                purgeBulkImportedMedia()

                // 2. Clean polluted media records
                cleanPollutedMediaRecords()

                // 3. Deduplicate messages
                messageRepository.deduplicateExistingMessages()

                // 4. Seed default core messaging apps (WhatsApp, Messenger, Telegram, Instagram, imo, Signal)
                database.appDao().insertApps(com.notivault.app.data.local.CoreApps.DEFAULT_APPS)

                // 5. Ensure NotificationListenerService is bound and active
                com.notivault.app.service.NotiVaultListenerService.ensureServiceConnected(this@NotiVaultApp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun purgeBulkImportedMedia() = withContext(Dispatchers.IO) {
        try {
            val mediaDao = database.mediaDao()
            // Delete physical files of orphan bulk-imported gallery media
            val orphanPaths = mediaDao.getOrphanMediaPaths()
            for (path in orphanPaths) {
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
            }
            mediaDao.deleteOrphanMedia()

            // Delete any loose files in saved_media not linked to an actual message or view-once
            val savedDir = File(filesDir, "saved_media")
            if (savedDir.exists() && savedDir.isDirectory) {
                val validPaths = mediaDao.getValidMediaPaths().toSet()
                savedDir.listFiles()?.forEach { file ->
                    if (!validPaths.contains(file.absolutePath)) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun cleanPollutedMediaRecords() = withContext(Dispatchers.IO) {
        try {
            val mediaDao = database.mediaDao()
            val messageDao = database.messageDao()
            val allMedia = mediaDao.getAllMediaList()

            for (m in allMedia) {
                val isBlacklisted = MediaStoreObserver.isBlacklisted(m.originalPath, "", m.fileName) ||
                        MediaStoreObserver.isBlacklisted(m.internalSavedPath, "", m.fileName)
                val isQrBlob = m.fileName.contains("qr", ignoreCase = true) || m.originalPath.contains("qr", ignoreCase = true)
                val isAvatarImage = m.originalPath.startsWith("notification_") && m.fileSizeBytes < 2500
                val isSyntheticMock = m.originalPath.contains("test_view_once") ||
                        m.internalSavedPath.contains("test_view_once") ||
                        m.fileName.contains("test_view_once")

                if (isBlacklisted || isQrBlob || isAvatarImage || isSyntheticMock) {
                    try {
                        val file = File(m.internalSavedPath)
                        if (file.exists()) file.delete()
                    } catch (_: Exception) {}
                    mediaDao.deleteMedia(m.id)
                }
            }

            // Also clean up any message records where mediaUri pointed to an avatar, deleted file, or synthetic mock
            val allMessages = messageDao.getAllMessagesSync()
            for (msg in allMessages) {
                if (msg.originalNotificationKey?.startsWith("demo_vo_") == true) {
                    messageDao.deleteMessage(msg.id)
                    continue
                }
                if (msg.mediaUri?.contains("test_view_once") == true) {
                    messageDao.clearMessageMedia(msg.id)
                    continue
                }
                if (msg.hasMedia && !msg.mediaUri.isNullOrEmpty()) {
                    val file = File(msg.mediaUri)
                    val isLikelyAvatar = file.exists() && file.length() < 2500 &&
                            !msg.messageText.contains("photo", ignoreCase = true) &&
                            !msg.messageText.contains("📷") &&
                            !msg.messageText.contains("ছবি")
                    if (!file.exists() || isLikelyAvatar) {
                        messageDao.clearMessageMedia(msg.id)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
