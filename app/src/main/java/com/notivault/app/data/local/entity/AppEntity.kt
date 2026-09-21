package com.notivault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a monitored application (e.g. WhatsApp, Messenger, Instagram, Telegram).
 */
@Entity(tableName = "monitored_apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val colorHex: String = "#0D9488",
    val iconResName: String? = null,
    val totalMessages: Int = 0,
    val lastActivityTimestamp: Long = System.currentTimeMillis()
)
