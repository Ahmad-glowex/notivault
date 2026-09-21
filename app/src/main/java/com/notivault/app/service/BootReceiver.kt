package com.notivault.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notivault.app.NotiVaultApp
import com.notivault.app.service.media.MediaObserverService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? NotiVaultApp ?: return
            CoroutineScope(Dispatchers.IO).launch {
                val isMediaBackupEnabled = app.settingsRepository.isMediaBackupEnabled.first()
                if (isMediaBackupEnabled) {
                    MediaObserverService.start(context)
                }
            }
        }
    }
}
