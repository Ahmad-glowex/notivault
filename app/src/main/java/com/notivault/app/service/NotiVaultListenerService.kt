package com.notivault.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notivault.app.NotiVaultApp
import com.notivault.app.R
import com.notivault.app.data.local.CoreApps
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

        /**
         * Ensures that NotiVaultListenerService is bound and connected by Android's NotificationManagerService.
         * Toggles the component setting if needed to trigger system re-registration on devices that kill services.
         */
        fun ensureServiceConnected(context: Context) {
            val componentName = ComponentName(context, NotiVaultListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(componentName)
                    Log.d(TAG, "requestRebind initiated for NotiVaultListenerService")
                } catch (e: Exception) {
                    Log.w(TAG, "requestRebind failed: ${e.message}")
                }
            }
            try {
                val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
                if (enabledListeners.contains(context.packageName)) {
                    val pm = context.packageManager
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Component re-enabled to refresh NotificationListener binding")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed component toggle rebind: ${e.message}")
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotiVaultListenerService connected successfully to Android Notification System")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "NotiVaultListenerService disconnected! Attempting immediate rebind...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotiVaultListenerService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rebind in onListenerDisconnected", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName in IGNORED_PACKAGES) return

        // Skip ongoing notifications (e.g., media playback, live call, download progress)
        if (sbn.isOngoing) return

        val app = application as? NotiVaultApp ?: return

        serviceScope.launch {
            try {
                // Synchronously check if notification interception is globally enabled
                val isInterceptionEnabled = app.settingsRepository.isInterceptionEnabledDirect()
                if (!isInterceptionEnabled) {
                    Log.d(TAG, "Notification interception paused, skipping notification from $packageName")
                    return@launch
                }

                // Check if app is monitored:
                // 1. Explicitly enabled in database
                // 2. Or a core messaging app (WhatsApp, Messenger, Telegram, Instagram, IMO, Signal, etc.)
                // 3. Or flagged as a messaging notification (CATEGORY_MESSAGE)
                val appEntity = app.database.appDao().getApp(packageName)
                val isMonitored = when {
                    appEntity != null -> appEntity.isEnabled
                    CoreApps.isCoreApp(packageName) || sbn.notification.category == Notification.CATEGORY_MESSAGE -> {
                        ensureAppRegistered(app, packageName)
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

                val isMediaBackupEnabled = app.settingsRepository.isMediaBackupEnabledDirect()
                val isDeletedAlertEnabled = app.settingsRepository.isDeletedAlertEnabledDirect()

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
                        Log.d(TAG, "Marked message as deleted: $marked")

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

    private suspend fun ensureAppRegistered(app: NotiVaultApp, packageName: String) {
        val appDao = app.database.appDao()
        val existing = appDao.getApp(packageName)
        if (existing == null) {
            val pm = packageManager
            val appName = try {
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                when {
                    CoreApps.isWhatsApp(packageName) -> "WhatsApp"
                    CoreApps.isMessenger(packageName) -> "Messenger"
                    CoreApps.isTelegram(packageName) -> "Telegram"
                    CoreApps.isInstagram(packageName) -> "Instagram"
                    else -> packageName
                }
            }
            val colorHex = CoreApps.getDefaultColor(packageName)
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
