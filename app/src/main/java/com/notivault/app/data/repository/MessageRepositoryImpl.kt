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

    override fun searchThreads(query: String, packageName: String?): Flow<List<ChatThreadEntity>> =
        if (packageName != null) {
            chatDao.searchThreadsByPackage(packageName, query)
        } else {
            chatDao.searchThreads(query)
        }

    override fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query)

    override fun getMonitoredApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    override fun getDeletedMessagesCount(): Flow<Int> = messageDao.getDeletedMessagesCount()

    override fun getAllDeletedMessages(): Flow<List<MessageEntity>> =
        messageDao.getAllDeletedMessages()

    override suspend fun getAllMessages(): List<MessageEntity> = withContext(Dispatchers.IO) {
        messageDao.getAllMessagesSync()
    }

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
        val resolvedSender = senderName.trim().ifEmpty { cleanTitle }
        val threadId = "${packageName}_$cleanTitle"

        // Safeguard: If message text indicates deletion, route to markDeletedBySender
        if (com.notivault.app.service.engine.DeletedMessageDetector.isDeletedNotification(messageText)) {
            markDeletedBySender(packageName, cleanTitle, resolvedSender, timestamp)
            return@withContext 0L
        }

        // Deduplication: Check if this exact message has already been captured
        val existingMsg = messageDao.findExistingMessage(
            threadId = threadId,
            senderName = resolvedSender,
            messageText = messageText,
            timestamp = timestamp
        )
        if (existingMsg != null) {
            return@withContext existingMsg.id
        }

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
            senderName = resolvedSender,
            messageText = messageText,
            timestamp = timestamp,
            isDeleted = false,
            deletedTimestamp = null,
            originalNotificationKey = notificationKey,
            hasMedia = hasMedia,
            mediaUri = mediaUri,
            mediaMimeType = if (hasMedia) "image/jpeg" else null,
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
        val cleanTitle = chatTitle.trim().ifEmpty { senderName.trim().ifEmpty { "Unknown" } }
        val resolvedSender = senderName.trim().ifEmpty { cleanTitle }
        val threadId = "${packageName}_$cleanTitle"

        // 1. Try matching active message near the deletion timestamp (within 10s)
        var targetMessage = if (timestamp > 0) {
            messageDao.getActiveMessageNearTimestamp(threadId, timestamp, toleranceMs = 10000L)
        } else null

        // 2. Fallback to latest active message by this sender
        if (targetMessage == null) {
            targetMessage = messageDao.getLatestActiveMessageBySender(threadId, resolvedSender)
        }

        // 3. Fallback to latest active message in thread
        if (targetMessage == null) {
            targetMessage = messageDao.getLatestActiveMessageInThread(threadId)
        }

        if (targetMessage != null) {
            messageDao.markMessageAsDeleted(targetMessage.id, timestamp)

            // Update thread last message preview if this was the latest message
            val currentThread = chatDao.getThreadSync(threadId)
            if (currentThread != null && currentThread.lastMessageTimestamp <= targetMessage.timestamp) {
                chatDao.insertOrUpdateThread(
                    currentThread.copy(
                        lastMessageText = "${targetMessage.messageText} (Deleted)",
                        lastMessageTimestamp = timestamp
                    )
                )
            }
            return@withContext true
        } else {
            // Original message was not cached beforehand (e.g. sender unsent immediately).
            // Preserve a tombstone record so user sees deletion occurred.
            val existingThread = chatDao.getThreadSync(threadId)
            val updatedThread = ChatThreadEntity(
                threadId = threadId,
                packageName = packageName,
                chatTitle = cleanTitle,
                isGroup = false,
                lastMessageText = "Deleted message preserved",
                lastMessageTimestamp = timestamp,
                unreadCount = (existingThread?.unreadCount ?: 0) + 1,
                isPinned = existingThread?.isPinned ?: false,
                isMuted = existingThread?.isMuted ?: false
            )
            chatDao.insertOrUpdateThread(updatedThread)

            messageDao.insertMessage(
                MessageEntity(
                    threadId = threadId,
                    packageName = packageName,
                    senderName = resolvedSender,
                    messageText = "This message was deleted before capture",
                    timestamp = timestamp,
                    isDeleted = true,
                    deletedTimestamp = timestamp,
                    isSelf = false
                )
            )
            return@withContext true
        }
    }

    override suspend fun setThreadPinned(threadId: String, isPinned: Boolean) =
        withContext(Dispatchers.IO) {
            chatDao.setPinned(threadId, isPinned)
        }

    override suspend fun deleteThread(threadId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForThread(threadId)
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
