package com.notivault.app.data.repository

import com.notivault.app.data.local.AppDatabase
import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.data.local.entity.ChatThreadEntity
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MessageRepositoryImpl(
    private val db: AppDatabase
) : MessageRepository {

    private val appDao = db.appDao()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()

    override fun getAllThreads(): Flow<List<ChatThreadEntity>> = chatDao.getAllThreads()

    override fun getThreadsByPackage(packageName: String): Flow<List<ChatThreadEntity>> =
        chatDao.getThreadsByPackage(packageName)

    override fun getThread(threadId: String): Flow<ChatThreadEntity?> = chatDao.getThread(threadId)

    override fun getMessagesForThread(threadId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForThread(threadId)

    override fun searchThreads(query: String): Flow<List<ChatThreadEntity>> =
        chatDao.searchThreads(query)

    override fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query)

    override fun getMonitoredApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    override fun getDeletedMessagesCount(): Flow<Int> = messageDao.getDeletedMessagesCount()

    override fun getAllDeletedMessages(): Flow<List<MessageEntity>> =
        messageDao.getAllDeletedMessages()

    override suspend fun saveIncomingNotification(
        packageName: String,
        chatTitle: String,
        senderName: String,
        messageText: String,
        timestamp: Long,
        notificationKey: String?,
        isGroup: Boolean,
        hasMedia: Boolean,
        mediaUri: String?
    ): Long = withContext(Dispatchers.IO) {
        val cleanTitle = chatTitle.trim().ifEmpty { senderName.trim().ifEmpty { "Unknown" } }
        val threadId = "${packageName}_$cleanTitle"

        // Update or insert thread
        val existingThread = chatDao.getThreadSync(threadId)
        val updatedThread = ChatThreadEntity(
            threadId = threadId,
            packageName = packageName,
            chatTitle = cleanTitle,
            isGroup = isGroup,
            lastMessageText = messageText,
            lastMessageTimestamp = timestamp,
            unreadCount = (existingThread?.unreadCount ?: 0) + 1,
            isPinned = existingThread?.isPinned ?: false,
            isMuted = existingThread?.isMuted ?: false
        )
        chatDao.insertOrUpdateThread(updatedThread)

        // Insert message
        val messageEntity = MessageEntity(
            threadId = threadId,
            packageName = packageName,
            senderName = senderName.trim().ifEmpty { cleanTitle },
            messageText = messageText,
            timestamp = timestamp,
            isDeleted = false,
            deletedTimestamp = null,
            originalNotificationKey = notificationKey,
            hasMedia = hasMedia,
            mediaUri = mediaUri,
            mediaMimeType = null,
            isSelf = false
        )
        val msgId = messageDao.insertMessage(messageEntity)

        // Update app message count
        appDao.incrementMessageCount(packageName, timestamp)

        msgId
    }

    override suspend fun markDeletedBySender(
        packageName: String,
        chatTitle: String,
        senderName: String,
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanTitle = chatTitle.trim().ifEmpty { senderName.trim() }
        val threadId = "${packageName}_$cleanTitle"

        // Try exact sender match first
        var targetMessage = messageDao.getLatestActiveMessageBySender(threadId, senderName)
        if (targetMessage == null) {
            // Fallback to latest active in thread
            targetMessage = messageDao.getLatestActiveMessageInThread(threadId)
        }

        if (targetMessage != null) {
            messageDao.markMessageAsDeleted(targetMessage.id, timestamp)
            return@withContext true
        }

        false
    }

    override suspend fun setThreadPinned(threadId: String, isPinned: Boolean) =
        withContext(Dispatchers.IO) {
            chatDao.setPinned(threadId, isPinned)
        }

    override suspend fun deleteThread(threadId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteThread(threadId)
    }

    override suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(messageId)
    }

    override suspend fun clearAllDeletedMessages() = withContext(Dispatchers.IO) {
        messageDao.clearAllDeletedMessages()
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        messageDao.deleteAllMessages()
        chatDao.deleteAllThreads()
    }

    override suspend fun setAppMonitoring(packageName: String, isEnabled: Boolean) =
        withContext(Dispatchers.IO) {
            appDao.setAppEnabled(packageName, isEnabled)
        }
}
