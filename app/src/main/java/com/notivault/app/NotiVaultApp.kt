package com.notivault.app

import android.app.Application
import com.notivault.app.data.local.AppDatabase
import com.notivault.app.data.repository.MediaRepository
import com.notivault.app.data.repository.MediaRepositoryImpl
import com.notivault.app.data.repository.MessageRepository
import com.notivault.app.data.repository.MessageRepositoryImpl
import com.notivault.app.data.repository.SettingsRepository
import com.notivault.app.data.repository.SettingsRepositoryImpl
import com.notivault.app.service.media.MediaObserverService
import com.notivault.app.service.media.MediaStoreObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

        // Deduplicate existing messages and clean up legacy polluted media on startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                messageRepository.deduplicateExistingMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cleanPollutedMediaRecords()

            // Pre-populate standard supported apps so user can immediately toggle in Settings
            try {
                val defaultApps = listOf(
                    com.notivault.app.data.local.entity.AppEntity("com.whatsapp", "WhatsApp", isEnabled = true, colorHex = "#25D366"),
                    com.notivault.app.data.local.entity.AppEntity("com.whatsapp.w4b", "WhatsApp Business", isEnabled = true, colorHex = "#25D366"),
                    com.notivault.app.data.local.entity.AppEntity("com.facebook.orca", "Messenger", isEnabled = true, colorHex = "#0084FF"),
                    com.notivault.app.data.local.entity.AppEntity("org.telegram.messenger", "Telegram", isEnabled = true, colorHex = "#229ED9"),
                    com.notivault.app.data.local.entity.AppEntity("com.instagram.android", "Instagram", isEnabled = true, colorHex = "#E1306C")
                )
                database.appDao().insertApps(defaultApps)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Auto-start MediaObserverService if media backup is enabled
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settingsRepository.isMediaBackupEnabled.first()) {
                    MediaObserverService.start(this@NotiVaultApp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

                if (isBlacklisted || isQrBlob || isAvatarImage) {
                    try {
                        val file = File(m.internalSavedPath)
                        if (file.exists()) file.delete()
                    } catch (_: Exception) {}
                    mediaDao.deleteMedia(m.id)
                }
            }

            // Also clean up any message records where mediaUri pointed to an avatar or deleted file
            val allMessages = messageDao.getAllMessagesSync()
            for (msg in allMessages) {
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
