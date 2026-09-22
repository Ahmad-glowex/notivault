package com.notivault.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Collections

interface SettingsRepository {
    val isBiometricLockEnabled: Flow<Boolean>
    val isMediaBackupEnabled: Flow<Boolean>
    val isSecureWindowEnabled: Flow<Boolean>
    val isInterceptionEnabled: Flow<Boolean>
    val themeMode: Flow<String>
    val isDeletedAlertEnabled: Flow<Boolean>
    val autoCleanupDays: Flow<Int>

    fun setBiometricLockEnabled(enabled: Boolean)
    fun setMediaBackupEnabled(enabled: Boolean)
    fun setSecureWindowEnabled(enabled: Boolean)
    fun setInterceptionEnabled(enabled: Boolean)
    fun setThemeMode(mode: String)
    fun setDeletedAlertEnabled(enabled: Boolean)
    fun setAutoCleanupDays(days: Int)
    fun isAppLocked(): Boolean

    // Direct synchronous access methods for services & background workers
    fun isInterceptionEnabledDirect(): Boolean
    fun isMediaBackupEnabledDirect(): Boolean
    fun isDeletedAlertEnabledDirect(): Boolean
}

class SettingsRepositoryImpl(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notivault_settings", Context.MODE_PRIVATE)

    // Maintain strong references to listeners to prevent Android SharedPreferences weak reference GC bug
    private val activeListeners = Collections.synchronizedSet(
        mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    )

    companion object {
        private const val KEY_BIOMETRIC_LOCK = "key_biometric_lock"
        private const val KEY_MEDIA_BACKUP = "key_media_backup"
        private const val KEY_SECURE_WINDOW = "key_secure_window"
        private const val KEY_INTERCEPTION = "key_interception"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DELETED_ALERT = "key_deleted_alert"
        private const val KEY_AUTO_CLEANUP = "key_auto_cleanup"
    }

    override val isBiometricLockEnabled: Flow<Boolean> = preferenceFlow(KEY_BIOMETRIC_LOCK, false)
    override val isMediaBackupEnabled: Flow<Boolean> = preferenceFlow(KEY_MEDIA_BACKUP, true)
    override val isSecureWindowEnabled: Flow<Boolean> = preferenceFlow(KEY_SECURE_WINDOW, false)
    override val isInterceptionEnabled: Flow<Boolean> = preferenceFlow(KEY_INTERCEPTION, true)
    override val themeMode: Flow<String> = stringPreferenceFlow(KEY_THEME_MODE, "SYSTEM")
    override val isDeletedAlertEnabled: Flow<Boolean> = preferenceFlow(KEY_DELETED_ALERT, true)
    override val autoCleanupDays: Flow<Int> = intPreferenceFlow(KEY_AUTO_CLEANUP, 0)

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

    override fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    override fun setDeletedAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DELETED_ALERT, enabled).apply()
    }

    override fun setAutoCleanupDays(days: Int) {
        prefs.edit().putInt(KEY_AUTO_CLEANUP, days).apply()
    }

    override fun isAppLocked(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    }

    override fun isInterceptionEnabledDirect(): Boolean {
        return prefs.getBoolean(KEY_INTERCEPTION, true)
    }

    override fun isMediaBackupEnabledDirect(): Boolean {
        return prefs.getBoolean(KEY_MEDIA_BACKUP, true)
    }

    override fun isDeletedAlertEnabledDirect(): Boolean {
        return prefs.getBoolean(KEY_DELETED_ALERT, true)
    }

    private fun stringPreferenceFlow(key: String, defaultValue: String): Flow<String> = callbackFlow {
        trySend(prefs.getString(key, defaultValue) ?: defaultValue)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getString(key, defaultValue) ?: defaultValue)
            }
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    private fun intPreferenceFlow(key: String, defaultValue: Int): Flow<Int> = callbackFlow {
        trySend(prefs.getInt(key, defaultValue))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getInt(key, defaultValue))
            }
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }

    private fun preferenceFlow(key: String, defaultValue: Boolean): Flow<Boolean> = callbackFlow {
        trySend(prefs.getBoolean(key, defaultValue))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getBoolean(key, defaultValue))
            }
        }
        activeListeners.add(listener)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            activeListeners.remove(listener)
        }
    }
}
