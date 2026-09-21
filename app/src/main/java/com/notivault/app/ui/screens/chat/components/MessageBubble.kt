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
    onMediaClick: ((String) -> Unit)? = null
) {
    val isSelf = message.isSelf
    val isDeleted = message.isDeleted
    val bubbleColor = when {
        isDeleted -> DarkSurfaceVariant
        isSelf -> TealDark
        else -> DarkSurfaceVariant
    }
    val alignment = if (isSelf) Alignment.End else Alignment.Start

    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    val mediaFile = message.mediaUri?.let { File(it) }?.takeIf { it.exists() }

    val isViewOnce = message.messageText.contains("view once", ignoreCase = true) ||
            message.messageText.contains("opened", ignoreCase = true)
    val isPhoto = message.messageText.contains("photo", ignoreCase = true) ||
            message.messageText.contains("📷") || message.messageText.contains("ছবি")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSelf) 16.dp else 4.dp,
                        bottomEnd = if (isSelf) 4.dp else 16.dp
                    )
                )
                .then(
                    if (isDeleted) {
                        Modifier.border(
                            1.dp,
                            DeletedRed.copy(alpha = 0.6f),
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isSelf) 16.dp else 4.dp,
                                bottomEnd = if (isSelf) 4.dp else 16.dp
                            )
                        )
                    } else Modifier
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
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

                // View Once preserved badge
                if (isViewOnce) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "View Once Preserved",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Render cached media image if present
                if (mediaFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { onMediaClick?.invoke(mediaFile.absolutePath) }
                    ) {
                        AsyncImage(
                            model = mediaFile,
                            contentDescription = "Cached Media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text Content
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPhoto && mediaFile == null) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = message.messageText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDeleted) TextPrimaryDark else TextPrimaryDark,
                        fontWeight = if (isDeleted) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Fallback media indicator if file was not available locally
                if (message.hasMedia && mediaFile == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Attachment",
                            tint = TealSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Attachment logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealSecondary
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
                }
            }
        }
    }
}
