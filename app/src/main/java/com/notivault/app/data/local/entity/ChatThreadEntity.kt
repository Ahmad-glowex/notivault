package com.notivault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a conversation/chat thread (direct message or group chat).
 */
@Entity(
    tableName = "chat_threads",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["lastMessageTimestamp"])
    ]
)
data class ChatThreadEntity(
    @PrimaryKey
    val threadId: String, // e.g. "com.whatsapp_JohnDoe"
    val packageName: String,
    val chatTitle: String,
    val isGroup: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)
