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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
}
