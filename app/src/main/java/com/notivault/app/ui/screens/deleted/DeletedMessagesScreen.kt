package com.notivault.app.ui.screens.deleted

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.notivault.app.data.local.entity.MediaEntity
import com.notivault.app.data.local.entity.MessageEntity
import com.notivault.app.ui.screens.chat.components.DeletedBadge
import com.notivault.app.ui.screens.media.components.MediaViewerModal
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.TealSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedMessagesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: DeletedMessagesViewModel = viewModel()
) {
    val deletedMessages by viewModel.deletedMessages.collectAsState()
    var viewingMedia by remember { mutableStateOf<MediaEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Preserved Deleted Messages",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${deletedMessages.size} unsent messages recovered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (deletedMessages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllDeleted() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (deletedMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = DeletedRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No deleted messages captured yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When a sender sends and then deletes or unsends a message, NotiVault will preserve the original text and media here!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = deletedMessages,
                    key = { it.id }
                ) { message ->
                    DeletedMessageCard(
                        message = message,
                        onClick = { onNavigateToChat(message.threadId) },
                        onMediaClick = { path ->
                            viewingMedia = MediaEntity(
                                id = message.id,
                                threadId = message.threadId,
                                packageName = message.packageName,
                                originalPath = path,
                                internalSavedPath = path,
                                fileName = File(path).name,
                                mimeType = message.mediaMimeType ?: "image/jpeg",
                                fileSizeBytes = File(path).length(),
                                timestamp = message.timestamp,
                                mediaType = "IMAGE"
                            )
                        }
                    )
                }
            }
        }

        viewingMedia?.let { mediaItem ->
            MediaViewerModal(
                media = mediaItem,
                onDismiss = { viewingMedia = null },
                onDelete = { viewingMedia = null }
            )
        }
    }
}

@Composable
private fun DeletedMessageCard(
    message: MessageEntity,
    onClick: () -> Unit,
    onMediaClick: (String) -> Unit
) {
    val cleanChatName = message.threadId.substringAfter("${message.packageName}_", message.senderName)
    val appColor = when (message.packageName) {
        "com.whatsapp" -> Color(0xFF25D366)
        "com.facebook.orca" -> Color(0xFF0084FF)
        "com.instagram.android" -> Color(0xFFE1306C)
        "org.telegram.messenger" -> Color(0xFF229ED9)
        else -> TealSecondary
    }

    val appLabel = when (message.packageName) {
        "com.whatsapp" -> "WhatsApp"
        "com.facebook.orca" -> "Messenger"
        "com.instagram.android" -> "Instagram"
        "org.telegram.messenger" -> "Telegram"
        else -> message.packageName
    }

    val origTimeFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    val mediaFile = message.mediaUri?.let { File(it) }?.takeIf { it.exists() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DeletedRed.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: App badge, Contact Name, and Navigation Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(appColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = appColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cleanChatName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Go to chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Deleted Badge
            DeletedBadge(deletedTimestamp = message.deletedTimestamp)

            Spacer(modifier = Modifier.height(8.dp))

            // Cached media thumbnail if present
            if (mediaFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onMediaClick(mediaFile.absolutePath) }
                ) {
                    AsyncImage(
                        model = mediaFile,
                        contentDescription = "Deleted Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Preserved Original Message Content
            Text(
                text = message.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Original timestamp
            Text(
                text = "Sent at $origTimeFormatted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
