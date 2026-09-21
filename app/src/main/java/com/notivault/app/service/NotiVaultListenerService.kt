package com.notivault.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.service.parser.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotiVaultListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                        app.messageRepository.saveIncomingNotification(
                            packageName = item.packageName,
                            chatTitle = item.chatTitle,
                            senderName = item.senderName,
                            messageText = item.messageText,
                            timestamp = item.timestamp,
                            notificationKey = item.notificationKey,
                            isGroup = item.isGroup,
                            hasMedia = item.hasMedia,
                            mediaUri = null
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
