package com.notivault.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notivault.app.data.local.entity.ChatThreadEntity
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.InstagramPink
import com.notivault.app.ui.theme.MessengerBlue
import com.notivault.app.ui.theme.TelegramBlue
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import com.notivault.app.ui.theme.WhatsAppGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatThreadItem(
    thread: ChatThreadEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColor = when (thread.packageName) {
        "com.whatsapp" -> WhatsAppGreen
        "com.facebook.orca" -> MessengerBlue
        "com.instagram.android" -> InstagramPink
        "org.telegram.messenger" -> TelegramBlue
        else -> Color(0xFF6366F1)
    }

    val initial = thread.chatTitle.firstOrNull()?.uppercase() ?: "?"
    val formattedTime = formatTimestamp(thread.lastMessageTimestamp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with app color accent
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(appColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = appColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and last message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.chatTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (thread.isPinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = appColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            val isDeletedPreview = thread.lastMessageText.contains("(Deleted)", ignoreCase = true) ||
                    thread.lastMessageText.contains("deleted", ignoreCase = true) ||
                    thread.lastMessageText.contains("ডিলিট", ignoreCase = true)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDeletedPreview) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = DeletedRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = thread.lastMessageText.ifBlank { "No message preview" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDeletedPreview) Color(0xFFFCA5A5) else TextSecondaryDark,
                    fontWeight = if (isDeletedPreview) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Timestamp & Badge
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
            )

            if (thread.unreadCount > 0) {
                Badge(
                    containerColor = appColor,
                    contentColor = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(text = thread.unreadCount.toString())
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 86_400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        diff < 172_800_000 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
