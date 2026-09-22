package com.notivault.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.service.media.MediaObserverService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context
import android.content.Intent
import com.notivault.app.export.DataExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotiVaultApp
    private val settingsRepo = app.settingsRepository
    private val messageRepo = app.messageRepository

    private val _exportIntent = MutableStateFlow<Intent?>(null)
    val exportIntent: StateFlow<Intent?> = _exportIntent.asStateFlow()

    val isInterceptionEnabled: StateFlow<Boolean> = settingsRepo.isInterceptionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val isDeletedAlertEnabled: StateFlow<Boolean> = settingsRepo.isDeletedAlertEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoCleanupDays: StateFlow<Int> = settingsRepo.autoCleanupDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMediaBytes: StateFlow<Long> = app.mediaRepository.getTotalMediaBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalMessageCount: StateFlow<Int> = messageRepo.getMessageCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMediaCount: StateFlow<Int> = app.mediaRepository.getMediaCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isBiometricLockEnabled: StateFlow<Boolean> = settingsRepo.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isMediaBackupEnabled: StateFlow<Boolean> = settingsRepo.isMediaBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSecureWindowEnabled: StateFlow<Boolean> = settingsRepo.isSecureWindowEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val monitoredApps: StateFlow<List<AppEntity>> = messageRepo.getMonitoredApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInterception(enabled: Boolean) {
        settingsRepo.setInterceptionEnabled(enabled)
    }

    fun setThemeMode(mode: String) {
        settingsRepo.setThemeMode(mode)
    }

    fun setDeletedAlert(enabled: Boolean) {
        settingsRepo.setDeletedAlertEnabled(enabled)
    }

    fun setAutoCleanup(days: Int) {
        settingsRepo.setAutoCleanupDays(days)
        if (days > 0) {
            viewModelScope.launch {
                messageRepo.deleteMessagesOlderThan(days)
                app.mediaRepository.deleteMediaOlderThan(days)
            }
        }
    }

    fun purgeCache(context: Context) {
        viewModelScope.launch {
            app.mediaRepository.purgeOrphanMediaFiles(context)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        settingsRepo.setBiometricLockEnabled(enabled)
    }

    fun setMediaBackup(enabled: Boolean) {
        settingsRepo.setMediaBackupEnabled(enabled)
        if (enabled) {
            MediaObserverService.start(getApplication())
        } else {
            MediaObserverService.stop(getApplication())
        }
    }

    fun setSecureWindow(enabled: Boolean) {
        settingsRepo.setSecureWindowEnabled(enabled)
    }

    fun toggleAppMonitoring(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            messageRepo.setAppMonitoring(packageName, enabled)
        }
    }

    fun addMonitoredApp(packageName: String, appName: String, colorHex: String? = null) {
        viewModelScope.launch {
            val resolvedColor = colorHex ?: com.notivault.app.data.local.CoreApps.getDefaultColor(packageName)
            val appEntity = AppEntity(
                packageName = packageName,
                appName = appName,
                isEnabled = true,
                colorHex = resolvedColor
            )
            messageRepo.addMonitoredApp(appEntity)
        }
    }

    fun removeMonitoredApp(packageName: String) {
        viewModelScope.launch {
            messageRepo.deleteMonitoredApp(packageName)
        }
    }

    fun resetToDefaultApps() {
        viewModelScope.launch {
            val allApps = app.database.appDao().getAllAppsSync()
            for (item in allApps) {
                if (!com.notivault.app.data.local.CoreApps.isCoreApp(item.packageName)) {
                    messageRepo.deleteMonitoredApp(item.packageName)
                }
            }
            app.database.appDao().insertApps(com.notivault.app.data.local.CoreApps.DEFAULT_APPS)
        }
    }

    fun exportAllToJson(context: Context) {
        viewModelScope.launch {
            val allMessages = messageRepo.getAllMessages()
            val file = DataExporter.exportAllToJson(context, allMessages)
            _exportIntent.value = DataExporter.getShareIntent(context, file, "application/json")
        }
    }

    fun exportAllToCsv(context: Context) {
        viewModelScope.launch {
            val allMessages = messageRepo.getAllMessages()
            val file = DataExporter.exportAllToCsv(context, allMessages)
            _exportIntent.value = DataExporter.getShareIntent(context, file, "text/csv")
        }
    }

    fun clearExportIntent() {
        _exportIntent.value = null
    }

    fun clearDeletedMessages() {
        viewModelScope.launch {
            messageRepo.clearAllDeletedMessages()
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            messageRepo.clearAllData()
            app.mediaRepository.clearAllMedia()
        }
    }
}
