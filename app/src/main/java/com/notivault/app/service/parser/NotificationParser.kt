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

        val fallbackText = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim() ?: ""

        // Skip pure group summary count notifications (e.g. "3 new messages" container)
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val cleanFallbackForSummary = DeletedMessageDetector.sanitize(fallbackText)
        val isSummaryCountText = (rawTitle.equals("WhatsApp", ignoreCase = true) || rawTitle.isEmpty()) &&
                (cleanFallbackForSummary.matches(Regex("""^\d+\s+(?:new\s+)?messages?.*$""", RegexOption.IGNORE_CASE)) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+টি\s+নতুন\s+মেসেজ.*$""")) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+رسائل\s+جديدة.*$""")) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+نئے\s+پیغامات.*$""")) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+नए\s+संदेश.*$""")) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+mensajes?\s+nuevos?.*$""", RegexOption.IGNORE_CASE)) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+novas?\s+mensagens?.*$""", RegexOption.IGNORE_CASE)) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+новых?\s+сообщен.*$""", RegexOption.IGNORE_CASE)) ||
                 cleanFallbackForSummary.matches(Regex("""^\d+\s+条新消息.*$""")))
        if (isGroupSummary && isSummaryCountText) {
            return emptyList()
        }

        // 1. First-class: AndroidX NotificationCompat.MessagingStyle extraction
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val styleTitle = messagingStyle.conversationTitle?.toString()?.trim()
            val isGroup = messagingStyle.isGroupConversation || !styleTitle.isNullOrEmpty() || isGroupExtra
            val chatTitle = styleTitle ?: rawTitle.ifEmpty { "Chat" }

            val messages = messagingStyle.messages
            for (i in messages.indices) {
                val msg = messages[i]
                var msgText = msg.text?.toString()?.trim() ?: ""
                val mediaUri = msg.dataUri
                val mediaType = msg.dataMimeType

                val isLatest = (i == messages.size - 1)
                val hasAttachedMedia = mediaUri != null || (isLatest && (extraBitmap != null || extraIcon != null))

                if (msgText.isBlank() && hasAttachedMedia) {
                    msgText = if (fallbackText.isNotBlank() && isMediaIndicatingText(fallbackText)) {
                        fallbackText
                    } else {
                        "📷 Sent a photo"
                    }
                }

                if (msgText.isBlank() && !hasAttachedMedia) continue

                val personName = msg.person?.name?.toString()?.trim()
                @Suppress("DEPRECATION")
                val fallbackSender = msg.sender?.toString()?.trim()
                val baseSender = personName ?: fallbackSender ?: (if (isGroup) "Member" else chatTitle)

                val msgTimestamp = if (msg.timestamp > 0) msg.timestamp else postTime

                val isDeleted = DeletedMessageDetector.isDeletedNotification(msgText)
                val resolvedSender = if (isDeleted) {
                    DeletedMessageDetector.extractUnsentAuthor(msgText, baseSender)
                } else {
                    baseSender
                }

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
                        var msgText = item.getCharSequence("text")?.toString()?.trim() ?: ""
                        val mediaUriStr = item.getString("dataUri")
                        val mediaUri = mediaUriStr?.let { Uri.parse(it) }
                        val mediaType = item.getString("dataMimeType")

                        val isLatest = (i == messagesArray.size - 1)
                        val hasAttachedMedia = mediaUri != null || (isLatest && (extraBitmap != null || extraIcon != null))

                        if (msgText.isBlank() && hasAttachedMedia) {
                            msgText = if (fallbackText.isNotBlank() && isMediaIndicatingText(fallbackText)) {
                                fallbackText
                            } else {
                                "📷 Sent a photo"
                            }
                        }

                        if (msgText.isBlank() && !hasAttachedMedia) continue

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

        // 2.5 Fallback: Parse EXTRA_TEXT_LINES (InboxStyle multi-message bundle)
        if (results.isEmpty()) {
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!textLines.isNullOrEmpty()) {
                val chatTitle = conversationTitle ?: rawTitle.ifEmpty { "Chat" }
                val lineCount = textLines.size
                for (i in textLines.indices) {
                    val rawLine = textLines[i]?.toString()?.trim() ?: continue
                    if (rawLine.isBlank()) continue

                    val isLatest = (i == lineCount - 1)
                    val (lineChatTitle, lineSender, lineText) = resolveTitleAndSender(
                        packageName = packageName,
                        rawTitle = rawTitle,
                        text = rawLine,
                        conversationTitle = conversationTitle,
                        isGroup = isGroupExtra
                    )

                    val isDeleted = DeletedMessageDetector.isDeletedNotification(lineText)
                    val resolvedSender = if (isDeleted) {
                        DeletedMessageDetector.extractUnsentAuthor(lineText, lineSender)
                    } else {
                        lineSender
                    }

                    val hasAttachedMedia = isLatest && (extraBitmap != null || extraIcon != null)
                    val msgBitmap = if (isLatest) extraBitmap else null
                    val msgIcon = if (isLatest && msgBitmap == null) extraIcon else null

                    results.add(
                        ParsedNotification(
                            packageName = packageName,
                            chatTitle = if (lineChatTitle.isNotBlank() && lineChatTitle != "Direct Message") lineChatTitle else chatTitle,
                            senderName = resolvedSender,
                            messageText = lineText.ifEmpty { if (hasAttachedMedia) "📷 Sent a photo" else "" },
                            timestamp = postTime - (lineCount - 1 - i) * 1000L,
                            notificationKey = key,
                            isGroup = isGroupExtra,
                            isDeletedNotice = isDeleted,
                            hasMedia = hasAttachedMedia,
                            mediaType = if (hasAttachedMedia) "image/jpeg" else null,
                            mediaBitmap = msgBitmap,
                            mediaIcon = msgIcon,
                            mediaDataUri = null
                        )
                    )
                }
            }
        }

        // 3. Fallback: Standard single-notification text (BigTextStyle / Normal)
        if (results.isEmpty()) {
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

                val hasAttachedMedia = extraBitmap != null || extraIcon != null

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

    internal fun isMediaIndicatingText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("photo") ||
                lower.contains("video") ||
                lower.contains("audio") ||
                lower.contains("voice message") ||
                lower.contains("view once") ||
                lower.contains("opened") ||
                // Bengali
                lower.contains("ছবি") ||
                lower.contains("ভিডিও") ||
                lower.contains("একবার দেখার") ||
                lower.contains("ভিউ ওয়ান্স") ||
                lower.contains("খোলা হয়েছে") ||
                // Hindi
                lower.contains("खोला गया") ||
                lower.contains("फ़ोटो") ||
                lower.contains("वीडियो") ||
                // Arabic
                lower.contains("صورة") ||
                lower.contains("فيديو") ||
                lower.contains("مقطع صوتي") ||
                lower.contains("رسالة صوتية") ||
                lower.contains("عرض لمرة واحدة") ||
                // Urdu
                lower.contains("تصویر") ||
                lower.contains("صوتی پیغام") ||
                // Russian
                lower.contains("фото") ||
                lower.contains("видео") ||
                lower.contains("голосовое сообщение") ||
                // Spanish
                lower.contains("foto") ||
                // French
                lower.contains("vidéo") ||
                // Chinese
                lower.contains("照片") ||
                lower.contains("视频") ||
                lower.contains("语音") ||
                // Japanese
                lower.contains("写真") ||
                lower.contains("動画") ||
                // Emojis
                text.contains("📷") ||
                text.contains("🎥") ||
                text.contains("📸") ||
                text.contains("🎬") ||
                text.contains("🎤") ||
                text.contains("👁️")
    }

    internal fun extractNotificationMedia(extras: Bundle): Pair<Bitmap?, Icon?> {
        var bitmap: Bitmap? = null
        var icon: Icon? = null

        fun inspect(key: String) {
            if (bitmap != null || icon != null) return
            try {
                val obj = extras.get(key) ?: return
                if (obj is Bitmap) {
                    bitmap = obj
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && obj is Icon) {
                    icon = obj
                }
            } catch (_: Exception) {}
        }

        // 1. EXTRA_PICTURE ("android.picture") from BigPictureStyle
        inspect(Notification.EXTRA_PICTURE)
        inspect("android.picture")

        // 2. EXTRA_PICTURE_ICON ("android.pictureIcon") from BigPictureStyle
        inspect("android.pictureIcon")

        // Note: NEVER inspect EXTRA_LARGE_ICON or EXTRA_LARGE_ICON_BIG as they contain
        // the sender's circular profile avatar, not an attached media file.
        return Pair(bitmap, icon)
    }

    /**
     * Resolves group chat vs direct chat titles and splits "Sender: Text" patterns.
     * Sanitizes RTL Unicode isolates and zero-width characters for consistent multilingual thread grouping.
     */
    fun resolveTitleAndSender(
        packageName: String,
        rawTitle: String,
        text: String,
        conversationTitle: String?,
        isGroup: Boolean
    ): Triple<String, String, String> {
        val cleanRawTitle = DeletedMessageDetector.sanitize(rawTitle)
        val cleanText = DeletedMessageDetector.sanitize(text)
        val cleanConvTitle = conversationTitle?.let { DeletedMessageDetector.sanitize(it) }?.takeIf { it.isNotBlank() }

        val chatTitle: String
        val senderName: String
        var messageText = cleanText

        if (!cleanConvTitle.isNullOrBlank()) {
            chatTitle = cleanConvTitle
            // In group notifications, text or title often has "Alice: Hello"
            if (cleanRawTitle.isNotBlank() && cleanRawTitle != cleanConvTitle) {
                senderName = cleanRawTitle
            } else if (cleanText.contains(": ")) {
                val split = cleanText.split(": ", limit = 2)
                senderName = split[0].trim()
                messageText = split[1].trim()
            } else if (cleanText.contains("：")) {
                val split = cleanText.split("：", limit = 2)
                senderName = split[0].trim()
                messageText = split[1].trim()
            } else {
                senderName = cleanRawTitle.ifBlank { chatTitle }
            }
        } else {
            // Check for title formatted like "GroupName (Sender)"
            val groupMatch = GROUP_TITLE_SENDER_PATTERN.matchEntire(cleanRawTitle)
            if (groupMatch != null) {
                chatTitle = groupMatch.groupValues[1].trim()
                senderName = groupMatch.groupValues[2].trim()
            } else if (isGroup && cleanText.contains(": ")) {
                val split = cleanText.split(": ", limit = 2)
                chatTitle = cleanRawTitle.ifBlank { "Group Chat" }
                senderName = split[0].trim()
                messageText = split[1].trim()
            } else if (isGroup && cleanText.contains("：")) {
                val split = cleanText.split("：", limit = 2)
                chatTitle = cleanRawTitle.ifBlank { "Group Chat" }
                senderName = split[0].trim()
                messageText = split[1].trim()
            } else {
                chatTitle = cleanRawTitle.ifBlank { "Direct Message" }
                senderName = chatTitle
            }
        }

        return Triple(chatTitle, senderName, messageText)
    }
}
