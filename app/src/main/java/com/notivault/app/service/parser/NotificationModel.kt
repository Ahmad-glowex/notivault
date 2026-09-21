package com.notivault.app.service.parser

data class ParsedNotification(
    val packageName: String,
    val chatTitle: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long,
    val notificationKey: String,
    val isGroup: Boolean = false,
    val isDeletedNotice: Boolean = false,
    val hasMedia: Boolean = false,
    val mediaType: String? = null
)
