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
        val cleanTitle = com.notivault.app.service.engine.DeletedMessageDetector.sanitize(chatTitle).ifEmpty {
            com.notivault.app.service.engine.DeletedMessageDetector.sanitize(senderName).ifEmpty { "Unknown" }
        }
        val resolvedSender = com.notivault.app.service.engine.DeletedMessageDetector.sanitize(senderName).ifEmpty { cleanTitle }
        val threadId = "${packageName}_$cleanTitle"

        // Safeguard: If message text indicates deletion, route to markDeletedBySender
        if (com.notivault.app.service.engine.DeletedMessageDetector.isDeletedNotification(messageText)) {
            markDeletedBySender(packageName, cleanTitle, resolvedSender, timestamp)
            return@withContext 0L
        }

        // Deduplication: Check if this exact message or duplicate within 30s tolerance exists
        val existingMsg = messageDao.findExistingMessageWithTolerance(
            threadId = threadId,
            senderName = resolvedSender,
            messageText = messageText,
            timestamp = timestamp,
            toleranceMs = 30000L
        ) ?: messageDao.findExistingMessageInThread(
            threadId = threadId,
            messageText = messageText,
            timestamp = timestamp,
            toleranceMs = 30000L
        )
        if (existingMsg != null) {
            // If new notification has media but stored one did not, update media
            if (!mediaUri.isNullOrEmpty() && existingMsg.mediaUri.isNullOrEmpty()) {
                messageDao.updateMessageMedia(existingMsg.id, mediaUri, "image/jpeg")
            }
            return@withContext existingMsg.id
        }

        // Attempt linking with recently cached unlinked media for this package (e.g. MediaStoreObserver captured just before notification)
        var resolvedMediaUri = mediaUri
        var resolvedHasMedia = hasMedia
        if (resolvedMediaUri.isNullOrEmpty() && (hasMedia || com.notivault.app.service.parser.NotificationParser.run { messageText.contains("photo", ignoreCase = true) || messageText.contains("📷") })) {
            val unlinkedMedia = db.mediaDao().getRecentUnlinkedMediaForPackage(packageName, sinceTimestamp = timestamp - 30000L)
            if (unlinkedMedia != null) {
                resolvedMediaUri = unlinkedMedia.internalSavedPath
                resolvedHasMedia = true
                db.mediaDao().updateMediaLinkage(unlinkedMedia.id, threadId, null)
            }
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
            hasMedia = resolvedHasMedia,
            mediaUri = resolvedMediaUri,
            mediaMimeType = if (resolvedHasMedia) "image/jpeg" else null,
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
        var cleanTitle = com.notivault.app.service.engine.DeletedMessageDetector.sanitize(chatTitle).ifEmpty {
            com.notivault.app.service.engine.DeletedMessageDetector.sanitize(senderName).ifEmpty { "Unknown" }
        }
        var resolvedSender = com.notivault.app.service.engine.DeletedMessageDetector.sanitize(senderName).ifEmpty { cleanTitle }

        // If title is just the generic app name ("WhatsApp", "Telegram"), ignore it for threadId
        val isGenericAppTitle = cleanTitle.equals("WhatsApp", ignoreCase = true) ||
                cleanTitle.equals("Telegram", ignoreCase = true) ||
                cleanTitle.equals("Messenger", ignoreCase = true) ||
                cleanTitle.equals(packageName, ignoreCase = true)

        var threadId = "${packageName}_$cleanTitle"

        // 1. Try matching active message in thread near the deletion timestamp (within 30s)
        var targetMessage: MessageEntity? = null
        if (!isGenericAppTitle) {
            if (timestamp > 0) {
                targetMessage = messageDao.getActiveMessageNearTimestamp(threadId, timestamp, toleranceMs = 30000L)
            }
            // 2. Fallback to latest active message by this sender in thread
            if (targetMessage == null) {
                targetMessage = messageDao.getLatestActiveMessageBySender(threadId, resolvedSender)
            }
            // 3. Fallback to latest active message in thread
            if (targetMessage == null) {
                targetMessage = messageDao.getLatestActiveMessageInThread(threadId)
            }
        }

        // 4. Fallback across entire package if generic app title or thread had no matching messages
        if (targetMessage == null) {
            if (resolvedSender.isNotBlank() && !isGenericAppTitle) {
                targetMessage = messageDao.getLatestActiveMessageBySenderForPackage(packageName, resolvedSender)
            }
            if (targetMessage == null) {
                targetMessage = messageDao.getLatestActiveMessageForPackage(packageName)
            }
            if (targetMessage != null) {
                threadId = targetMessage.threadId
                cleanTitle = targetMessage.threadId.removePrefix("${packageName}_")
                resolvedSender = targetMessage.senderName
            }
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
            // Preserve a tombstone record only if we have a real contact name (not generic app title)
            if (isGenericAppTitle) {
                return@withContext false
            }

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

    override suspend fun deduplicateExistingMessages(threadId: String?) = withContext(Dispatchers.IO) {
        val messages = if (threadId != null) {
            messageDao.getMessagesForThreadSync(threadId)
        } else {
            messageDao.getAllMessagesSync()
        }

        val grouped = messages.groupBy { "${it.threadId}___${it.messageText.trim()}" }
        for ((_, list) in grouped) {
            if (list.size <= 1) continue
            val sorted = list.sortedBy { it.timestamp }
            var baseMsg = sorted[0]
            for (i in 1 until sorted.size) {
                val candidate = sorted[i]
                if (Math.abs(candidate.timestamp - baseMsg.timestamp) <= 30000L) {
                    // Duplicate within 15 seconds tolerance!
                    if (!candidate.mediaUri.isNullOrEmpty() && baseMsg.mediaUri.isNullOrEmpty()) {
                        messageDao.updateMessageMedia(baseMsg.id, candidate.mediaUri!!, candidate.mediaMimeType)
                        baseMsg = baseMsg.copy(mediaUri = candidate.mediaUri, hasMedia = true, mediaMimeType = candidate.mediaMimeType)
                    }
                    messageDao.deleteMessage(candidate.id)
                } else {
                    baseMsg = candidate
                }
            }
        }
    }
}
