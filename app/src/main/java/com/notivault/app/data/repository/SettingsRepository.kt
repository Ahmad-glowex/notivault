package com.notivault.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface SettingsRepository {
    val isBiometricLockEnabled: Flow<Boolean>
    val isMediaBackupEnabled: Flow<Boolean>
    val isSecureWindowEnabled: Flow<Boolean>
    val isInterceptionEnabled: Flow<Boolean>

    fun setBiometricLockEnabled(enabled: Boolean)
    fun setMediaBackupEnabled(enabled: Boolean)
    fun setSecureWindowEnabled(enabled: Boolean)
    fun setInterceptionEnabled(enabled: Boolean)
    fun isAppLocked(): Boolean
}

class SettingsRepositoryImpl(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notivault_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_LOCK = "key_biometric_lock"
        private const val KEY_MEDIA_BACKUP = "key_media_backup"
        private const val KEY_SECURE_WINDOW = "key_secure_window"
        private const val KEY_INTERCEPTION = "key_interception"
    }

    override val isBiometricLockEnabled: Flow<Boolean> = preferenceFlow(KEY_BIOMETRIC_LOCK, false)
    override val isMediaBackupEnabled: Flow<Boolean> = preferenceFlow(KEY_MEDIA_BACKUP, true)
    override val isSecureWindowEnabled: Flow<Boolean> = preferenceFlow(KEY_SECURE_WINDOW, false)
    override val isInterceptionEnabled: Flow<Boolean> = preferenceFlow(KEY_INTERCEPTION, true)

    override fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    override fun setMediaBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEDIA_BACKUP, enabled).apply()
    }

    override fun setSecureWindowEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_WINDOW, enabled).apply()
    }

    override fun setInterceptionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTERCEPTION, enabled).apply()
    }

    override fun isAppLocked(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    }

    private fun preferenceFlow(key: String, defaultValue: Boolean): Flow<Boolean> = callbackFlow {
        trySend(prefs.getBoolean(key, defaultValue))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getBoolean(key, defaultValue))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
