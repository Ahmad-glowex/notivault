package com.notivault.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notivault.app.NotiVaultApp
import com.notivault.app.service.media.MediaObserverService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-bind / ensure NotificationListenerService connection on reboot or app update
            NotiVaultListenerService.ensureServiceConnected(context)

            val app = context.applicationContext as? NotiVaultApp ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isMediaBackupEnabled = app.settingsRepository.isMediaBackupEnabledDirect()
                    if (isMediaBackupEnabled) {
                        MediaObserverService.start(context)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
