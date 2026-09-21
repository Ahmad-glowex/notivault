package com.notivault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an individual message captured from a notification event.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["packageName"]),
        Index(value = ["timestamp"]),
        Index(value = ["isDeleted"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: String,
    val packageName: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val originalNotificationKey: String? = null,
    val hasMedia: Boolean = false,
    val mediaUri: String? = null,
    val mediaMimeType: String? = null,
    val isSelf: Boolean = false
)
