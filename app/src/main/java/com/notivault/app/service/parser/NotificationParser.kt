package com.notivault.app.service.parser

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
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
     * individual senders, group conversation titles, deleted message flags, and media attachments.
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

        // Extract any notification-level media payload (e.g. BigPictureStyle picture, LargeIcon)
        val (extraBitmap, extraIcon) = extractNotificationMedia(extras)

        // 1. First-class: AndroidX NotificationCompat.MessagingStyle extraction
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val styleTitle = messagingStyle.conversationTitle?.toString()?.trim()
            val isGroup = messagingStyle.isGroupConversation || !styleTitle.isNullOrEmpty() || isGroupExtra
            val chatTitle = styleTitle ?: rawTitle.ifEmpty { "Chat" }

            val messages = messagingStyle.messages
            for (i in messages.indices) {
                val msg = messages[i]
                val msgText = msg.text?.toString()?.trim() ?: ""
                if (msgText.isBlank() && msg.dataUri == null && extraBitmap == null) continue

                val personName = msg.person?.name?.toString()?.trim()
                @Suppress("DEPRECATION")
                val fallbackSender = msg.sender?.toString()?.trim()
                val baseSender = personName ?: fallbackSender ?: (if (isGroup) "Member" else chatTitle)

                val msgTimestamp = if (msg.timestamp > 0) msg.timestamp else postTime
                val mediaUri = msg.dataUri
                val mediaType = msg.dataMimeType

                val isDeleted = DeletedMessageDetector.isDeletedNotification(msgText)
                val resolvedSender = if (isDeleted) {
                    DeletedMessageDetector.extractUnsentAuthor(msgText, baseSender)
                } else {
                    baseSender
                }

                val isLatest = (i == messages.size - 1)
                val isMediaText = isMediaIndicatingText(msgText)
                val hasAttachedMedia = mediaUri != null || (isLatest && (extraBitmap != null || extraIcon != null || isMediaText))

                val msgBitmap = if (isLatest) extraBitmap else null
                val msgIcon = if (isLatest && msgBitmap == null) extraIcon else null

                results.add(
                    ParsedNotification(
                        packageName = packageName,
                        chatTitle = chatTitle,
                        senderName = resolvedSender,
                        messageText = msgText.ifEmpty { if (hasAttachedMedia) "📷 Sent a photo" else "" },
                        timestamp = msgTimestamp,
                        notificationKey = key,
                        isGroup = isGroup,
                        isDeletedNotice = isDeleted,
                        hasMedia = hasAttachedMedia,
                        mediaType = mediaType ?: if (msgBitmap != null || msgIcon != null) "image/jpeg" else null,
                        mediaBitmap = msgBitmap,
                        mediaIcon = msgIcon,
                        mediaDataUri = mediaUri
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

                for (i in messagesArray.indices) {
                    val item = messagesArray[i]
                    if (item is Bundle) {
                        val msgText = item.getCharSequence("text")?.toString()?.trim() ?: ""
                        val mediaUriStr = item.getString("dataUri")
                        val mediaUri = mediaUriStr?.let { Uri.parse(it) }
                        val mediaType = item.getString("dataMimeType")

                        if (msgText.isBlank() && mediaUri == null && extraBitmap == null) continue

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

                        val isDeleted = DeletedMessageDetector.isDeletedNotification(msgText)
                        val resolvedSender = if (isDeleted) {
                            DeletedMessageDetector.extractUnsentAuthor(msgText, senderName)
                        } else {
                            senderName
                        }

                        val isLatest = (i == messagesArray.size - 1)
                        val isMediaText = isMediaIndicatingText(msgText)
                        val hasAttachedMedia = mediaUri != null || (isLatest && (extraBitmap != null || extraIcon != null || isMediaText))

                        val msgBitmap = if (isLatest) extraBitmap else null
                        val msgIcon = if (isLatest && msgBitmap == null) extraIcon else null

                        results.add(
                            ParsedNotification(
                                packageName = packageName,
                                chatTitle = chatTitle,
                                senderName = resolvedSender,
                                messageText = msgText.ifEmpty { if (hasAttachedMedia) "📷 Sent a photo" else "" },
                                timestamp = msgTimestamp,
                                notificationKey = key,
                                isGroup = isGroupExtra,
                                isDeletedNotice = isDeleted,
                                hasMedia = hasAttachedMedia,
                                mediaType = mediaType ?: if (msgBitmap != null || msgIcon != null) "image/jpeg" else null,
                                mediaBitmap = msgBitmap,
                                mediaIcon = msgIcon,
                                mediaDataUri = mediaUri
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

            if (fallbackText.isNotBlank() || extraBitmap != null || extraIcon != null) {
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

                val isMediaText = isMediaIndicatingText(cleanText)
                val hasAttachedMedia = extraBitmap != null || extraIcon != null || isMediaText

                results.add(
                    ParsedNotification(
                        packageName = packageName,
                        chatTitle = chatTitle,
                        senderName = resolvedSender,
                        messageText = cleanText.ifEmpty { if (hasAttachedMedia) "📷 Sent a photo" else "" },
                        timestamp = postTime,
                        notificationKey = key,
                        isGroup = isGroupExtra,
                        isDeletedNotice = isDeleted,
                        hasMedia = hasAttachedMedia,
                        mediaType = if (hasAttachedMedia) "image/jpeg" else null,
                        mediaBitmap = extraBitmap,
                        mediaIcon = extraIcon,
                        mediaDataUri = null
                    )
                )
            }
        }

        return results
    }

    private fun isMediaIndicatingText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("photo") ||
                lower.contains("video") ||
                lower.contains("audio") ||
                lower.contains("voice message") ||
                lower.contains("view once") ||
                text.contains("📷") ||
                text.contains("🎥") ||
                text.contains("📸") ||
                text.contains("🎬") ||
                text.contains("🎤") ||
                text.contains("ছবি") ||
                text.contains("ভিডিও")
    }

    private fun extractNotificationMedia(extras: Bundle): Pair<Bitmap?, Icon?> {
        var bitmap: Bitmap? = null
        var icon: Icon? = null

        // 1. Direct picture from BigPictureStyle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bitmap = extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            bitmap = extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
        }

        // 2. Icon from BigPictureStyle on API 31+
        if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                icon = extras.getParcelable("android.pictureIcon", Icon::class.java)
            } else {
                @Suppress("DEPRECATION")
                icon = extras.getParcelable("android.pictureIcon") as? Icon
            }
        }

        // 3. Large Icon Big
        if (bitmap == null && icon == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bitmap = extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                bitmap = extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap
            }
        }

        // 4. Fallback: inspect standard LARGE_ICON
        if (bitmap == null && icon == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bitmap = extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
                if (bitmap == null) {
                    icon = extras.getParcelable(Notification.EXTRA_LARGE_ICON, Icon::class.java)
                }
            } else {
                @Suppress("DEPRECATION")
                val largeObj = extras.get(Notification.EXTRA_LARGE_ICON)
                if (largeObj is Bitmap) {
                    bitmap = largeObj
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && largeObj is Icon) {
                    icon = largeObj
                }
            }
        }

        return Pair(bitmap, icon)
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
