package com.notivault.app.ui.screens.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.widget.Toast
import com.notivault.app.data.local.entity.MessageEntity
import com.notivault.app.service.media.MediaObserverService
import com.notivault.app.service.media.RootViewOnceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivault.app.data.local.entity.MediaEntity
import com.notivault.app.ui.screens.chat.components.MessageBubble
import com.notivault.app.ui.screens.media.components.MediaViewerModal
import com.notivault.app.ui.theme.DarkBackground
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    threadId: String,
    viewModel: ChatDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val thread by viewModel.thread.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val exportIntent by viewModel.exportIntent.collectAsState()
    val listState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var viewingMedia by remember { mutableStateOf<MediaEntity?>(null) }
    var recoveryMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var isRoot by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val root = RootViewOnceManager.isRootAvailable()
            withContext(Dispatchers.Main) {
                isRoot = root
            }
        }
    }

    LaunchedEffect(exportIntent) {
        exportIntent?.let { intent ->
            context.startActivity(intent)
            viewModel.clearExportIntent()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val deletedInThread = messages.count { it.isDeleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = thread?.chatTitle ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${messages.size} messages logged • ${thread?.packageName ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextPrimaryDark
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export as JSON") },
                            onClick = {
                                showMenu = false
                                viewModel.exportToJson(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as CSV") },
                            onClick = {
                                showMenu = false
                                viewModel.exportToCsv(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear This Conversation") },
                            onClick = {
                                showMenu = false
                                viewModel.clearChat()
                                onNavigateBack()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Deleted Messages Recovery Header Banner
            if (deletedInThread > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = DeletedRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$deletedInThread deleted message(s) preserved in this chat",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages logged yet in this conversation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isGroup = thread?.isGroup ?: false,
                            onMediaClick = { path ->
                                val isVid = message.mediaMimeType?.startsWith("video") == true ||
                                        path.endsWith(".mp4", ignoreCase = true) ||
                                        path.endsWith(".mkv", ignoreCase = true) ||
                                        path.endsWith(".3gp", ignoreCase = true)
                                viewingMedia = MediaEntity(
                                    id = message.id,
                                    threadId = message.threadId,
                                    packageName = message.packageName,
                                    originalPath = path,
                                    internalSavedPath = path,
                                    fileName = File(path).name,
                                    mimeType = message.mediaMimeType ?: if (isVid) "video/mp4" else "image/jpeg",
                                    fileSizeBytes = File(path).length(),
                                    timestamp = message.timestamp,
                                    mediaType = if (isVid) "VIDEO" else "IMAGE"
                                )
                            },
                            onMissingMediaClick = { msg ->
                                recoveryMessage = msg
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

            recoveryMessage?.let { targetMsg ->
                ViewOnceRecoveryDialog(
                    message = targetMsg,
                    isRoot = isRoot,
                    onDismiss = { recoveryMessage = null },
                    onTriggerRootRecovery = {
                        coroutineScope.launch {
                            Toast.makeText(context, "রুট স্যান্ডবক্স স্নাইপার স্ক্যান শুরু হচ্ছে...", Toast.LENGTH_SHORT).show()
                            val recovered = RootViewOnceManager.extractViewOnceFiles(context, targetMsg.packageName)
                            recoveryMessage = null
                            if (recovered > 0) {
                                Toast.makeText(context, "$recovered টি ভিউ-ওয়ান্স মিডিয়া উদ্ধার হয়েছে এবং চ্যাট বাবলে যুক্ত হয়েছে!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "স্যান্ডবক্সে ফাইলটি পাওয়া যায়নি। (হয়তো WhatsApp ইতিমধ্যে মুছে ফেলেছে)", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onTriggerStorageScan = {
                        coroutineScope.launch {
                            MediaObserverService.triggerViewOnceSniff(context, targetMsg.packageName)
                            recoveryMessage = null
                            Toast.makeText(context, "স্টোরেজ স্নাইপার স্ক্যান চালু করা হয়েছে...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ViewOnceRecoveryDialog(
    message: MessageEntity,
    isRoot: Boolean,
    onDismiss: () -> Unit,
    onTriggerRootRecovery: () -> Unit,
    onTriggerStorageScan: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimaryDark,
        textContentColor = Color(0xFFCBD5E1),
        icon = {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = TealSecondary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "ভিউ-ওয়ান্স মিডিয়া স্ট্যাটাস ও রিকভারি",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isRoot) {
                    Text(
                        text = "✅ আপনার ডিভাইসে Root Privilege সক্রিয় আছে!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                    Text(
                        text = "NotiVault সরাসরি WhatsApp-এর এনক্রিপ্টেড স্যান্ডবক্স ফোল্ডার (/data/data/com.whatsapp/files/ViewOnce) অথবা অভ্যন্তরীণ ডেটাবেস থেকে আসল ছবি উদ্ধার করার চেষ্টা করতে পারে।",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "ℹ️ ভিউ-ওয়ান্স মিডিয়া অ্যান্ড্রয়েড স্যান্ডবক্সে সংরক্ষিত",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "অ্যান্ড্রয়েডের লিনাক্স সিকিউরিটি কাঠামোর কারণে কোনো সাধারণ অ্যাপ সরাসরি WhatsApp-এর অভ্যন্তরীণ ফোল্ডার (/data/data/com.whatsapp) রিড করতে পারে না।\n\n" +
                                "ভিউ-ওয়ান্স ছবি স্বয়ংক্রিয়ভাবে পেতে নিম্নলিখিত উপায়গুলো কাজ করে:\n" +
                                "১. Root Access: ফোন রুটেড থাকলে NotiVault সরাসরি স্যান্ডবক্স থেকে আসল ছবি উদ্ধার করে বাবলে শো করায়।\n" +
                                "২. LSPatch + WaEnhancer: নন-রুট ডিভাইসে LSPatch-এর মাধ্যমে WaEnhancer মডিউল ব্যবহার করলে WhatsApp স্বয়ংক্রিয়ভাবে ভিউ-ওয়ান্স ছবি আপনার গ্যালারিতে সেভ করে ফেলে, এবং NotiVault তা চ্যাট বাবলে সরাসরি শো করে।\n" +
                                "৩. সাধারণ ছবি: সেন্ডার যদি সাধারণ ছবি হিসেবে পাঠায় (যেমন উপরে পাঠানো গণিতের নোটস), NotiVault তা মুহূর্তের মধ্যেই চ্যাট বাবলে সুন্দরভাবে প্রদর্শন করে।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (isRoot) {
                Button(
                    onClick = onTriggerRootRecovery,
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("স্যান্ডবক্স থেকে উদ্ধার করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onTriggerStorageScan,
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("স্টোরেজ স্ক্যান করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = TextSecondaryDark)
            }
        }
    )
}
