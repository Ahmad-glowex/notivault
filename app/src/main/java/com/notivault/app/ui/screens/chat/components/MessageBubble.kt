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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
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
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.TealPrimary
import com.notivault.app.ui.theme.TealSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    modifier: Modifier = Modifier,
    onMediaClick: ((String) -> Unit)? = null
) {
    val isSelf = message.isSelf
    val isDeleted = message.isDeleted
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val bubbleColor = when {
        isDeleted -> if (isDark) Color(0xFF2B1418) else Color(0xFFFEF2F2)
        isSelf -> if (isDark) Color(0xFF0F5147) else Color(0xFFDCFCE7)
        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    }
    val borderColor = when {
        isDeleted -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)
        isSelf -> if (isDark) Color(0xFF14B8A6).copy(alpha = 0.4f) else Color(0xFFBBF7D0)
        else -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    }
    val messageTextColor = when {
        isDeleted -> if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
        isSelf -> if (isDark) Color(0xFFF0FDFA) else Color(0xFF064E3B)
        else -> if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }
    val timestampColor = when {
        isDeleted -> if (isDark) Color(0xFFEF4444) else Color(0xFFB91C1C)
        isSelf -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
        else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }
    val senderColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
    val checkColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF059669)
    val mediaIconColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
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
        val bubbleWidthMin = if (mediaFile != null) 220.dp else 72.dp
        val bubbleWidthMax = if (mediaFile != null) 340.dp else 320.dp

        val boxModifier = Modifier
            .widthIn(min = bubbleWidthMin, max = bubbleWidthMax)
            .clip(bubbleShape)
            .border(1.dp, borderColor, bubbleShape)
            .background(bubbleColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)

        Box(modifier = boxModifier) {
            Column {
                // In group chats, display sender name
                if (isGroup && !isSelf && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = senderColor,
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
                    val voBg = if (isCached) (if (isDark) TealSecondary.copy(alpha = 0.2f) else Color(0xFFCCFBF1)) else (if (isDark) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFFFEF3C7))
                    val voTextCol = if (isCached) (if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)) else (if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(voBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = voTextCol,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCached) "View Once Preserved" else "View Once",
                            style = MaterialTheme.typography.labelSmall,
                            color = voTextCol,
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

                    val hasMediaIcon = mediaFile == null && (isVideoMsg || isPhoto)
                    Row(
                        verticalAlignment = if (hasMediaIcon) Alignment.Top else Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        if (mediaFile == null) {
                            if (isVideoMsg) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = mediaIconColor,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (isPhoto) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = mediaIconColor,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                        Text(
                            text = displayMsgText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.5.sp,
                                lineHeight = 23.sp
                            ),
                            color = messageTextColor,
                            fontWeight = if (isDeleted) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Subtle media indicator only if a media URI was logged but missing on disk
                if (message.hasMedia && !message.mediaUri.isNullOrBlank() && mediaFile == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val missingMediaBg = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                    val missingMediaText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(missingMediaBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Attachment",
                            tint = missingMediaText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Media attachment not available locally",
                            style = MaterialTheme.typography.labelSmall,
                            color = missingMediaText
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
                        color = timestampColor
                    )
                    if (isSelf) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = checkColor,
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
