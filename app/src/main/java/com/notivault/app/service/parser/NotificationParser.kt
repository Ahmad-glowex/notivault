package com.notivault.app.service.parser

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.notivault.app.service.engine.DeletedMessageDetector

object NotificationParser {

    /**
     * Parses a StatusBarNotification into a list of one or more ParsedNotification items.
     * Handles MessagingStyle bundle extraction, group chat resolution, and deleted message flags.
     */
    fun parse(sbn: StatusBarNotification): List<ParsedNotification> {
        val notification = sbn.notification ?: return emptyList()
        val packageName = sbn.packageName ?: return emptyList()
        val extras = notification.extras ?: return emptyList()

        val results = mutableListOf<ParsedNotification>()
        val postTime = sbn.postTime
        val key = sbn.key ?: "${packageName}_$postTime"

        // Group conversation metadata
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) || !conversationTitle.isNullOrEmpty()

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val normalText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val fallbackText = (bigText ?: normalText) ?: ""

        // Check MessagingStyle EXTRA_MESSAGES (ParcelableArray of Bundles)
        val messagesArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messagesArray != null && messagesArray.isNotEmpty()) {
            val chatTitle = conversationTitle ?: rawTitle.ifEmpty { "Chat" }

            for (item in messagesArray) {
                if (item is Bundle) {
                    val msgText = item.getCharSequence("text")?.toString()?.trim() ?: ""
                    if (msgText.isBlank()) continue

                    val senderPerson = item.getCharSequence("sender")?.toString()?.trim()
                    val senderName = senderPerson ?: (if (isGroup) "Member" else chatTitle)
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
                            isGroup = isGroup,
                            isDeletedNotice = isDeleted,
                            hasMedia = mediaUri != null,
                            mediaType = mediaType
                        )
                    )
                }
            }
        }

        // If no messaging style bundles were found, parse single notification
        if (results.isEmpty() && fallbackText.isNotBlank()) {
            val (chatTitle, senderName, cleanText) = resolveTitleAndSender(
                packageName = packageName,
                rawTitle = rawTitle,
                text = fallbackText,
                conversationTitle = conversationTitle,
                isGroup = isGroup
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
                    isGroup = isGroup,
                    isDeletedNotice = isDeleted,
                    hasMedia = false,
                    mediaType = null
                )
            )
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
            // Direct message or group without explicit conversation title
            if (isGroup && text.contains(": ")) {
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
