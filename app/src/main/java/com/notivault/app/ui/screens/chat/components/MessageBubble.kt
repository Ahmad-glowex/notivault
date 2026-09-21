package com.notivault.app.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.notivault.app.data.local.entity.MessageEntity
import com.notivault.app.ui.theme.DarkSurfaceVariant
import com.notivault.app.ui.theme.DeletedBadgeBorder
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.DeletedRedBg
import com.notivault.app.ui.theme.TealDark
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    modifier: Modifier = Modifier,
    onMediaClick: ((String) -> Unit)? = null,
    onMissingMediaClick: ((MessageEntity) -> Unit)? = null
) {
    val isSelf = message.isSelf
    val isDeleted = message.isDeleted
    val bubbleColor = when {
        isDeleted -> Color(0xFF241418)
        isSelf -> Color(0xFF0D5A54)
        else -> Color(0xFF1E293B)
    }
    val borderColor = when {
        isDeleted -> DeletedRed.copy(alpha = 0.6f)
        isSelf -> Color(0xFF14B8A6).copy(alpha = 0.3f)
        else -> Color(0xFF334155).copy(alpha = 0.6f)
    }
    val alignment = if (isSelf) Alignment.End else Alignment.Start

    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    val mediaFile = message.mediaUri?.let { File(it) }?.takeIf { it.exists() }
    val isVideo = message.mediaMimeType?.startsWith("video") == true ||
            message.mediaUri?.endsWith(".mp4", ignoreCase = true) == true ||
            message.mediaUri?.endsWith(".mkv", ignoreCase = true) == true ||
            message.mediaUri?.endsWith(".3gp", ignoreCase = true) == true

    val isViewOnce = message.messageText.contains("view once", ignoreCase = true) ||
            message.messageText.contains("opened", ignoreCase = true) ||
            message.messageText.contains("ভিউ ওয়ান্স", ignoreCase = true) ||
            message.messageText.contains("একবার দেখার", ignoreCase = true) ||
            message.messageText.contains("①") ||
            message.messageText.contains("\u2460")
    val isPhoto = message.messageText.contains("photo", ignoreCase = true) ||
            message.messageText.contains("📷") || message.messageText.contains("ছবি") ||
            message.messageText.contains("📸")
    val isVideoMsg = message.messageText.contains("video", ignoreCase = true) ||
            message.messageText.contains("🎥") || message.messageText.contains("ভিডিও") ||
            message.messageText.contains("🎬")

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isSelf) 16.dp else 4.dp,
        bottomEnd = if (isSelf) 4.dp else 16.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = alignment
    ) {
        val bubbleWidthMin = if (mediaFile != null) 240.dp else 120.dp
        val bubbleWidthMax = if (mediaFile != null) 340.dp else 320.dp

        val boxModifier = if (mediaFile == null && isViewOnce && onMissingMediaClick != null) {
            Modifier
                .widthIn(min = bubbleWidthMin, max = bubbleWidthMax)
                .clip(bubbleShape)
                .border(1.dp, borderColor, bubbleShape)
                .background(bubbleColor)
                .clickable { onMissingMediaClick.invoke(message) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        } else {
            Modifier
                .widthIn(min = bubbleWidthMin, max = bubbleWidthMax)
                .clip(bubbleShape)
                .border(1.dp, borderColor, bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        }

        Box(modifier = boxModifier) {
            Column {
                // In group chats, display sender name
                if (isGroup && !isSelf && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = TealSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // If message was marked as deleted, show prominent preserved banner
                if (isDeleted) {
                    DeletedBadge(deletedTimestamp = message.deletedTimestamp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // View Once indicator
                if (isViewOnce) {
                    val isCached = mediaFile != null
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCached) TealSecondary.copy(alpha = 0.15f)
                                else Color(0xFFF59E0B).copy(alpha = 0.15f)
                            )
                            .then(
                                if (!isCached && onMissingMediaClick != null) {
                                    Modifier.clickable { onMissingMediaClick.invoke(message) }
                                } else Modifier
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isCached) TealSecondary else Color(0xFFF59E0B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCached) "View Once Preserved" else "View Once (ট্যাপ করে রিকভারি দেখুন)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCached) TealSecondary else Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Render cached media image cleanly and prominently
                if (mediaFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { onMediaClick?.invoke(mediaFile.absolutePath) }
                    ) {
                        AsyncImage(
                            model = mediaFile,
                            contentDescription = "Attached Media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 280.dp),
                            contentScale = ContentScale.Crop
                        )

                        if (isVideo) {
                            Box(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Play Video",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text Content
                val cleanMsgText = message.messageText.trim()
                val isPlaceholderText = isMediaPlaceholderText(cleanMsgText)

                // Only render text if: not a media placeholder when media is attached, OR media is absent
                val shouldRenderText = if (mediaFile != null) !isPlaceholderText else cleanMsgText.isNotBlank()

                if (shouldRenderText) {
                    val displayMsgText = when {
                        cleanMsgText.startsWith("📷 ") -> cleanMsgText.removePrefix("📷 ")
                        cleanMsgText.startsWith("📷") -> cleanMsgText.removePrefix("📷").trim()
                        cleanMsgText.startsWith("📸 ") -> cleanMsgText.removePrefix("📸 ")
                        cleanMsgText.startsWith("📸") -> cleanMsgText.removePrefix("📸").trim()
                        else -> cleanMsgText
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mediaFile == null) {
                            if (isVideoMsg) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = TealSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (isPhoto) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = TealSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                        Text(
                            text = displayMsgText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimaryDark,
                            fontWeight = if (isDeleted) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Subtle media indicator only if a media URI was logged but missing on disk
                if (message.hasMedia && !message.mediaUri.isNullOrBlank() && mediaFile == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Attachment",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Media attachment not available locally",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp & Status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    if (isSelf) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = TealSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Checks if a message text is simply a placeholder for an attached photo, video, or View-Once item.
 * When media is physically available, returning true suppresses the duplicate text label in the bubble.
 */
fun isMediaPlaceholderText(text: String): Boolean {
    val clean = text.trim()
    if (clean.isBlank()) return true
    val stripped = clean
        .replace("📷", "")
        .replace("📸", "")
        .replace("🎥", "")
        .replace("🎬", "")
        .replace("①", "")
        .replace("➀", "")
        .replace("\u2460", "")
        .trim()

    if (stripped.isBlank()) return true

    val lower = stripped.lowercase()
    return lower == "photo" ||
            lower == "sent a photo" ||
            lower == "video" ||
            lower == "sent a video" ||
            lower == "view once" ||
            lower == "view once photo" ||
            lower == "view once video" ||
            lower == "opened" ||
            lower == "ছবি" ||
            lower == "ভিডিও" ||
            lower == "একটি ছবি পাঠিয়েছেন" ||
            lower == "একটি ভিডিও পাঠিয়েছেন" ||
            lower == "একবার দেখার ছবি" ||
            lower == "একবার দেখার ভিডিও" ||
            lower == "ভিউ ওয়ান্স" ||
            lower.contains("sent a photo") ||
            lower.contains("sent a video")
}
