package com.notivault.app.service.parser

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.notivault.app.service.engine.DeletedMessageDetector

object NotificationParser {

    private val GROUP_TITLE_SENDER_PATTERN = Regex("""^(.+?)\s*\((.+?)\)$""")

    /**
     * Parses a StatusBarNotification into a list of one or more ParsedNotification items.
     * Extracts multi-message bundles via NotificationCompat.MessagingStyle, resolving
     * individual senders, group conversation titles, and deleted message flags.
     */
    fun parse(sbn: StatusBarNotification): List<ParsedNotification> {
        val notification = sbn.notification ?: return emptyList()
        val packageName = sbn.packageName ?: return emptyList()
        val extras = notification.extras ?: return emptyList()

        val results = mutableListOf<ParsedNotification>()
        val postTime = sbn.postTime
        val key = sbn.key ?: "${packageName}_$postTime"

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val isGroupExtra = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) || !conversationTitle.isNullOrEmpty()

        // 1. First-class: AndroidX NotificationCompat.MessagingStyle extraction
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val styleTitle = messagingStyle.conversationTitle?.toString()?.trim()
            val isGroup = messagingStyle.isGroupConversation || !styleTitle.isNullOrEmpty() || isGroupExtra
            val chatTitle = styleTitle ?: rawTitle.ifEmpty { "Chat" }

            for (msg in messagingStyle.messages) {
                val msgText = msg.text?.toString()?.trim() ?: ""
                if (msgText.isBlank()) continue

                val personName = msg.person?.name?.toString()?.trim()
                @Suppress("DEPRECATION")
                val fallbackSender = msg.sender?.toString()?.trim()
                val baseSender = personName ?: fallbackSender ?: (if (isGroup) "Member" else chatTitle)

                val msgTimestamp = if (msg.timestamp > 0) msg.timestamp else postTime
                val mediaUri = msg.dataUri?.toString()
                val mediaType = msg.dataMimeType

                val isDeleted = DeletedMessageDetector.isDeletedNotification(msgText)
                val resolvedSender = if (isDeleted) {
                    DeletedMessageDetector.extractUnsentAuthor(msgText, baseSender)
                } else {
                    baseSender
                }

                results.add(
                    ParsedNotification(
                        packageName = packageName,
                        chatTitle = chatTitle,
                        senderName = resolvedSender,
                        messageText = msgText,
                        timestamp = msgTimestamp,
                        notificationKey = key,
                        isGroup = isGroup,
                        isDeletedNotice = isDeleted,
                        hasMedia = mediaUri != null,
                        mediaType = mediaType
                    )
                )
            }
        }

        // 2. Fallback: Parse raw EXTRA_MESSAGES bundle array if messagingStyle extraction was empty
        if (results.isEmpty()) {
            @Suppress("DEPRECATION")
            val messagesArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messagesArray != null && messagesArray.isNotEmpty()) {
                val chatTitle = conversationTitle ?: rawTitle.ifEmpty { "Chat" }

                for (item in messagesArray) {
                    if (item is Bundle) {
                        val msgText = item.getCharSequence("text")?.toString()?.trim() ?: ""
                        if (msgText.isBlank()) continue

                        // Android 9+ (API 28+) stores Person in KEY_SENDER_PERSON ("sender_person")
                        val senderName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            @Suppress("DEPRECATION")
                            val person = item.getParcelable<android.app.Person>("sender_person")
                            person?.name?.toString()?.trim()
                                ?: item.getCharSequence("sender")?.toString()?.trim()
                                ?: (if (isGroupExtra) "Member" else chatTitle)
                        } else {
                            item.getCharSequence("sender")?.toString()?.trim()
                                ?: (if (isGroupExtra) "Member" else chatTitle)
                        }

                        val msgTimestamp = item.getLong("time").takeIf { it > 0 } ?: postTime
                        val mediaUri = item.getString("dataUri")
                        val mediaType = item.getString("dataMimeType")

                        val isDeleted = DeletedMessageDetector.isDeletedNotification(msgText)
                        val resolvedSender = if (isDeleted) {
                            DeletedMessageDetector.extractUnsentAuthor(msgText, senderName)
                        } else {
                            senderName
                        }

                        results.add(
                            ParsedNotification(
                                packageName = packageName,
                                chatTitle = chatTitle,
                                senderName = resolvedSender,
                                messageText = msgText,
                                timestamp = msgTimestamp,
                                notificationKey = key,
                                isGroup = isGroupExtra,
                                isDeletedNotice = isDeleted,
                                hasMedia = mediaUri != null,
                                mediaType = mediaType
                            )
                        )
                    }
                }
            }
        }

        // 3. Fallback: Standard single-notification text (BigTextStyle / Normal)
        if (results.isEmpty()) {
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            val normalText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            val fallbackText = (bigText ?: normalText) ?: ""

            if (fallbackText.isNotBlank()) {
                val (chatTitle, senderName, cleanText) = resolveTitleAndSender(
                    packageName = packageName,
                    rawTitle = rawTitle,
                    text = fallbackText,
                    conversationTitle = conversationTitle,
                    isGroup = isGroupExtra
                )

                val isDeleted = DeletedMessageDetector.isDeletedNotification(cleanText)
                val resolvedSender = if (isDeleted) {
                    DeletedMessageDetector.extractUnsentAuthor(cleanText, senderName)
                } else {
                    senderName
                }

                results.add(
                    ParsedNotification(
                        packageName = packageName,
                        chatTitle = chatTitle,
                        senderName = resolvedSender,
                        messageText = cleanText,
                        timestamp = postTime,
                        notificationKey = key,
                        isGroup = isGroupExtra,
                        isDeletedNotice = isDeleted,
                        hasMedia = false,
                        mediaType = null
                    )
                )
            }
        }

        return results
    }

    /**
     * Resolves group chat vs direct chat titles and splits "Sender: Text" patterns.
     */
    fun resolveTitleAndSender(
        packageName: String,
        rawTitle: String,
        text: String,
        conversationTitle: String?,
        isGroup: Boolean
    ): Triple<String, String, String> {
        val chatTitle: String
        val senderName: String
        var messageText = text

        if (!conversationTitle.isNullOrBlank()) {
            chatTitle = conversationTitle
            // In group notifications, text or title often has "Alice: Hello"
            if (rawTitle.isNotBlank() && rawTitle != conversationTitle) {
                senderName = rawTitle
            } else if (text.contains(": ")) {
                val split = text.split(": ", limit = 2)
                senderName = split[0].trim()
                messageText = split[1].trim()
            } else {
                senderName = rawTitle.ifBlank { chatTitle }
            }
        } else {
            // Check for title formatted like "GroupName (Sender)"
            val groupMatch = GROUP_TITLE_SENDER_PATTERN.matchEntire(rawTitle)
            if (groupMatch != null) {
                chatTitle = groupMatch.groupValues[1].trim()
                senderName = groupMatch.groupValues[2].trim()
            } else if (isGroup && text.contains(": ")) {
                val split = text.split(": ", limit = 2)
                chatTitle = rawTitle.ifBlank { "Group Chat" }
                senderName = split[0].trim()
                messageText = split[1].trim()
            } else {
                chatTitle = rawTitle.ifBlank { "Direct Message" }
                senderName = chatTitle
            }
        }

        return Triple(chatTitle, senderName, messageText)
    }
}
