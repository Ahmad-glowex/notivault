package com.notivault.app.data.repository

import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.data.local.entity.ChatThreadEntity
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getAllThreads(): Flow<List<ChatThreadEntity>>
    fun getThreadsByPackage(packageName: String): Flow<List<ChatThreadEntity>>
    fun getThread(threadId: String): Flow<ChatThreadEntity?>
    fun getMessagesForThread(threadId: String): Flow<List<MessageEntity>>
    fun searchThreads(query: String, packageName: String? = null): Flow<List<ChatThreadEntity>>
    fun searchMessages(query: String): Flow<List<MessageEntity>>
    fun getMonitoredApps(): Flow<List<AppEntity>>
    fun getDeletedMessagesCount(): Flow<Int>
    fun getAllDeletedMessages(): Flow<List<MessageEntity>>
    suspend fun getAllMessages(): List<MessageEntity>

    suspend fun saveIncomingNotification(
        packageName: String,
        chatTitle: String,
        senderName: String,
        messageText: String,
        timestamp: Long,
        notificationKey: String?,
        isGroup: Boolean = false,
        hasMedia: Boolean = false,
        mediaUri: String? = null,
        mediaMimeType: String? = null
    ): Long

    suspend fun markDeletedBySender(
        packageName: String,
        chatTitle: String,
        senderName: String,
        timestamp: Long
    ): Boolean

    suspend fun setThreadPinned(threadId: String, isPinned: Boolean)
    suspend fun deleteThread(threadId: String)
    suspend fun deleteMessage(messageId: Long)
    suspend fun clearAllDeletedMessages()
    suspend fun clearAllData()
    suspend fun setAppMonitoring(packageName: String, isEnabled: Boolean)
    suspend fun deduplicateExistingMessages(threadId: String? = null)
}
