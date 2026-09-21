package com.notivault.app.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivault.app.data.local.entity.MessageEntity
import com.notivault.app.ui.theme.DarkSurfaceVariant
import com.notivault.app.ui.theme.TealDark
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: MessageEntity,
    isGroup: Boolean,
    modifier: Modifier = Modifier
) {
    val isSelf = message.isSelf
    val bubbleColor = if (isSelf) TealDark else DarkSurfaceVariant
    val alignment = if (isSelf) Alignment.End else Alignment.Start

    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSelf) 16.dp else 4.dp,
                        bottomEnd = if (isSelf) 4.dp else 16.dp
                    )
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

                // If message was marked as deleted, show deleted badge
                if (message.isDeleted) {
                    DeletedBadge(deletedTimestamp = message.deletedTimestamp)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text Content
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimaryDark
                )

                // Media indicator if applicable
                if (message.hasMedia) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Attachment",
                            tint = TealSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Media Attachment",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
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
