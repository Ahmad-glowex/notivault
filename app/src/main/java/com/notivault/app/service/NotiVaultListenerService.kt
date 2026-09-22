package com.notivault.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.notivault.app.NotiVaultApp
import com.notivault.app.R
import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.service.parser.NotificationParser
import com.notivault.app.service.media.MediaCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
                // Check if notification interception is globally enabled
                val isInterceptionEnabled = app.settingsRepository.isInterceptionEnabled.first()
                if (!isInterceptionEnabled) {
                    Log.d(TAG, "Notification interception is paused in settings, skipping notification")
                    return@launch
                }

                // Check if app is monitored. By default, ONLY core messaging apps are allowed.
                // Any other app (e.g. ChatGPT, Grove, Muse, Gmail) is NOT captured unless explicitly added by user.
                val appEntity = app.database.appDao().getApp(packageName)
                val isMonitored = when {
                    appEntity != null -> appEntity.isEnabled
                    com.notivault.app.data.local.CoreApps.isCoreApp(packageName) -> {
                        ensureCoreAppRegistered(app, packageName)
                        true
                    }
                    else -> {
                        Log.d(TAG, "Skipping notification from non-monitored app: $packageName")
                        false
                    }
                }

                if (!isMonitored) {
                    return@launch
                }

                val isMediaBackupEnabled = app.settingsRepository.isMediaBackupEnabled.first()
                val isDeletedAlertEnabled = app.settingsRepository.isDeletedAlertEnabled.first()

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

                        if (marked && isDeletedAlertEnabled) {
                            postDeletedAlertNotification(item.chatTitle, item.senderName, item.packageName)
                        }
                    } else {
                        var savedMediaUri: String? = null
                        var hasMedia = item.hasMedia

                        if (isMediaBackupEnabled) {
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
                            mediaUri = savedMediaUri,
                            mediaMimeType = item.mediaType
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    private suspend fun ensureCoreAppRegistered(app: NotiVaultApp, packageName: String) {
        if (!com.notivault.app.data.local.CoreApps.isCoreApp(packageName)) return
        val appDao = app.database.appDao()
        val existing = appDao.getApp(packageName)
        if (existing == null) {
            val pm = packageManager
            val appName = try {
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                when (packageName) {
                    com.notivault.app.data.local.CoreApps.PACKAGE_WHATSAPP -> "WhatsApp"
                    com.notivault.app.data.local.CoreApps.PACKAGE_WHATSAPP_W4B -> "WhatsApp Business"
                    com.notivault.app.data.local.CoreApps.PACKAGE_MESSENGER -> "Messenger"
                    com.notivault.app.data.local.CoreApps.PACKAGE_TELEGRAM -> "Telegram"
                    com.notivault.app.data.local.CoreApps.PACKAGE_INSTAGRAM -> "Instagram"
                    else -> packageName
                }
            }
            val colorHex = com.notivault.app.data.local.CoreApps.getDefaultColor(packageName)
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

    private fun postDeletedAlertNotification(chatTitle: String, senderName: String, packageName: String) {
        try {
            val channelId = "notivault_deleted_alerts"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Deleted Message Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts you when a message is deleted or recalled by sender"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(this, com.notivault.app.ui.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            val pendingIntent = PendingIntent.getActivity(
                this,
                (System.currentTimeMillis() % 100000).toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val appName = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }

            val alertNotification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Deleted Message Preserved ($appName)")
                .setContentText("$senderName deleted a message in $chatTitle")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$senderName deleted a message in $chatTitle.\nNotiVault has safely preserved the original content in your vault.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt() + 2000, alertNotification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post deleted message alert notification", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
