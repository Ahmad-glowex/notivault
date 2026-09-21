package com.notivault.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.service.parser.NotificationParser
import com.notivault.app.service.media.MediaCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotiVaultListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheManager by lazy { MediaCacheManager(this) }

    companion object {
        private const val TAG = "NotiVaultListener"

        // Packages to ignore from intercepting (own app, system UI, play store, etc.)
        private val IGNORED_PACKAGES = setOf(
            "com.notivault.app",
            "com.notivault.app.debug",
            "android",
            "com.android.systemui",
            "com.android.vending",
            "com.google.android.gms"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName in IGNORED_PACKAGES) return

        // Skip ongoing notifications (e.g., media player playback, call in progress)
        if (sbn.isOngoing) return

        // Unconditionally skip group summary notifications to prevent duplicate messages
        if ((sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0) return

        val app = application as? NotiVaultApp ?: return

        serviceScope.launch {
            try {
                // Ensure the app entity exists in DB
                ensureAppRegistered(app, packageName)

                val parsedItems = NotificationParser.parse(sbn)
                for (item in parsedItems) {
                    if (item.isDeletedNotice) {
                        Log.d(TAG, "Detected deleted message notification for ${item.chatTitle} by ${item.senderName}")
                        val marked = app.messageRepository.markDeletedBySender(
                            packageName = item.packageName,
                            chatTitle = item.chatTitle,
                            senderName = item.senderName,
                            timestamp = item.timestamp
                        )
                        Log.d(TAG, "Marked previous message as deleted: $marked")
                    } else {
                        var savedMediaUri: String? = null
                        var hasMedia = item.hasMedia

                        try {
                            if (item.mediaBitmap != null) {
                                val cachedFile = cacheManager.cacheBitmap(
                                    item.mediaBitmap,
                                    prefix = item.packageName.replace(".", "_")
                                )
                                if (cachedFile != null) {
                                    savedMediaUri = cachedFile.absolutePath
                                    hasMedia = true
                                    app.mediaRepository.saveCachedMedia(
                                        packageName = item.packageName,
                                        originalPath = "notification_${item.notificationKey}_${item.timestamp}",
                                        internalSavedPath = cachedFile.absolutePath,
                                        fileName = cachedFile.name,
                                        mimeType = "image/jpeg",
                                        fileSizeBytes = cachedFile.length(),
                                        mediaType = "IMAGE",
                                        threadId = "${item.packageName}_${item.chatTitle.trim()}"
                                    )
                                }
                            } else if (item.mediaIcon != null) {
                                val cachedFile = cacheManager.cacheIcon(
                                    item.mediaIcon,
                                    prefix = item.packageName.replace(".", "_")
                                )
                                if (cachedFile != null) {
                                    savedMediaUri = cachedFile.absolutePath
                                    hasMedia = true
                                    app.mediaRepository.saveCachedMedia(
                                        packageName = item.packageName,
                                        originalPath = "notification_${item.notificationKey}_${item.timestamp}",
                                        internalSavedPath = cachedFile.absolutePath,
                                        fileName = cachedFile.name,
                                        mimeType = "image/jpeg",
                                        fileSizeBytes = cachedFile.length(),
                                        mediaType = "IMAGE",
                                        threadId = "${item.packageName}_${item.chatTitle.trim()}"
                                    )
                                }
                            } else if (item.mediaDataUri != null) {
                                val cachedFile = cacheManager.cacheContentUri(
                                    item.mediaDataUri,
                                    item.mediaType,
                                    prefix = item.packageName.replace(".", "_")
                                )
                                if (cachedFile != null) {
                                    savedMediaUri = cachedFile.absolutePath
                                    hasMedia = true
                                    val mType = when {
                                        item.mediaType?.startsWith("video") == true -> "VIDEO"
                                        item.mediaType?.startsWith("audio") == true -> "AUDIO"
                                        else -> "IMAGE"
                                    }
                                    app.mediaRepository.saveCachedMedia(
                                        packageName = item.packageName,
                                        originalPath = item.mediaDataUri.toString(),
                                        internalSavedPath = cachedFile.absolutePath,
                                        fileName = cachedFile.name,
                                        mimeType = item.mediaType ?: "image/jpeg",
                                        fileSizeBytes = cachedFile.length(),
                                        mediaType = mType,
                                        threadId = "${item.packageName}_${item.chatTitle.trim()}"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error caching media payload from notification", e)
                        }

                        app.messageRepository.saveIncomingNotification(
                            packageName = item.packageName,
                            chatTitle = item.chatTitle,
                            senderName = item.senderName,
                            messageText = item.messageText,
                            timestamp = item.timestamp,
                            notificationKey = item.notificationKey,
                            isGroup = item.isGroup,
                            hasMedia = hasMedia,
                            mediaUri = savedMediaUri
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    private suspend fun ensureAppRegistered(app: NotiVaultApp, packageName: String) {
        val appDao = app.database.appDao()
        val existing = appDao.getApp(packageName)
        if (existing == null) {
            val pm = packageManager
            val appName = try {
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                packageName
            }
            val colorHex = when (packageName) {
                "com.whatsapp" -> "#25D366"
                "com.facebook.orca" -> "#0084FF"
                "com.instagram.android" -> "#E1306C"
                "org.telegram.messenger" -> "#229ED9"
                else -> "#6366F1"
            }
            appDao.insertApp(
                AppEntity(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = true,
                    colorHex = colorHex
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
