package com.notivault.app.ui.screens.viewonce

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.notivault.app.data.local.entity.MediaEntity
import com.notivault.app.ui.screens.media.components.MediaViewerModal
import com.notivault.app.ui.theme.DarkBackground
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.DarkSurfaceVariant
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

private const val WHATSAPP_WEB_URL = "https://web.whatsapp.com"

private val INJECTED_HOOK_JS = """
    (function() {
        if (window.__notiVaultActive) return;
        window.__notiVaultActive = true;

        // 1. Intercept URL.createObjectURL for blob media
        const origCreateObjectURL = window.URL.createObjectURL;
        window.URL.createObjectURL = function(blob) {
            const url = origCreateObjectURL.call(this, blob);
            try {
                if (blob && (blob.type.startsWith('image/') || blob.type.startsWith('video/'))) {
                    const reader = new FileReader();
                    reader.onloadend = function() {
                        if (reader.result && window.NotiVaultBridge) {
                            window.NotiVaultBridge.onMediaCaptured(reader.result, blob.type, true);
                        }
                    };
                    reader.readAsDataURL(blob);
                }
            } catch(e) {}
            return url;
        };

        // Single Phone Helper: Trigger 'Link with phone number'
        window.__notiVaultLinkWithPhone = function() {
            try {
                const elements = Array.from(document.querySelectorAll('*'));
                for (const el of elements) {
                    if (el.children.length === 0) {
                        const t = (el.textContent || '').trim().toLowerCase();
                        if (t === 'link with phone number' || t.includes('link with phone number') ||
                            t.includes('ফোন নম্বর') || t.includes('ফোন নম্বর দিয়ে') ||
                            t.includes('vincular con') || t.includes('número de teléfono') ||
                            t.includes('ফোন নম্বর দিয়ে লিঙ্ক') ||
                            t.includes('ربط باستخدام رقم الهاتف') || t.includes('फोन नंबर')) {
                            el.click();
                            return true;
                        }
                    }
                }
                const btns = Array.from(document.querySelectorAll('span[role="button"], div[role="button"], button'));
                for (const b of btns) {
                    const t = (b.textContent || '').toLowerCase();
                    if (t.includes('phone') || t.includes('ফোন') || t.includes('número')) {
                        b.click();
                        return true;
                    }
                }
            } catch(e) {}
            return false;
        };

        // 2. Periodic DOM scanner for QR code, connection state, pairing code, and View Once elements
        function pollState() {
            try {
                const qrCanvas = document.querySelector('canvas[aria-label*="QR"], canvas[role="img"], div[data-ref]');
                const mainChatList = document.getElementById('pane-side') || 
                                     document.querySelector('[aria-label="Chat list"]') ||
                                     document.querySelector('[aria-label="Chats"]');

                if (mainChatList) {
                    if (window.NotiVaultBridge) window.NotiVaultBridge.onStatusUpdate('AUTHENTICATED');
                } else if (qrCanvas) {
                    if (window.NotiVaultBridge) window.NotiVaultBridge.onStatusUpdate('QR_READY');
                }

                // Scan for WhatsApp 8-character pairing code
                const codeContainers = document.querySelectorAll('[data-link-code], [data-testid="link-code-input"], [data-testid="link-device-phone-number-code-screen"]');
                let foundCode = null;
                for (const c of codeContainers) {
                    const attr = c.getAttribute('data-link-code');
                    if (attr && attr.length >= 8) {
                        foundCode = attr;
                        break;
                    }
                }
                if (!foundCode) {
                    const spans = document.querySelectorAll('span, div');
                    for (const s of spans) {
                        if (s.children.length === 0) {
                            const text = (s.textContent || '').trim();
                            const match = text.match(/\b([A-Z0-9]{4}[-\s][A-Z0-9]{4})\b/);
                            if (match) {
                                foundCode = match[1];
                                break;
                            }
                        }
                    }
                }
                if (foundCode && window.NotiVaultBridge) {
                    window.NotiVaultBridge.onPairingCode(foundCode);
                }

                // Intercept visible view once media images and videos
                const mediaNodes = document.querySelectorAll('img[src^="blob:"], video[src^="blob:"]');
                mediaNodes.forEach(node => {
                    if (node.__nv_vaulted) return;
                    node.__nv_vaulted = true;
                    const src = node.src;
                    if (src && src.startsWith('blob:')) {
                        fetch(src).then(res => res.blob()).then(blob => {
                            const reader = new FileReader();
                            reader.onloadend = function() {
                                if (reader.result && window.NotiVaultBridge) {
                                    window.NotiVaultBridge.onMediaCaptured(reader.result, blob.type, true);
                                }
                            };
                            reader.readAsDataURL(blob);
                        }).catch(e => {});
                    }
                });
            } catch(e) {}
        }

        setInterval(pollState, 1500);
        pollState();
    })();
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnceVaultScreen(
    onNavigateBack: () -> Unit,
    viewModel: ViewOnceViewModel = viewModel()
) {
    val context = LocalContext.current
    val viewOnceMedia by viewModel.viewOnceMedia.collectAsState()
    val viewOnceCount by viewModel.viewOnceCount.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isGuideExpanded by viewModel.isGuideExpanded.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val lastCapturedTime by viewModel.lastCapturedTime.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoadingWeb by remember { mutableStateOf(true) }

    LaunchedEffect(lastCapturedTime) {
        if (lastCapturedTime != null) {
            Toast.makeText(context, "📸 View-Once media captured & saved to vault!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View-Once Vault",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TealSecondary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LINKED ENGINE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = TealSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "$viewOnceCount items saved permanently",
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
                    IconButton(onClick = { viewModel.toggleGuide() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Pairing Guide",
                            tint = if (isGuideExpanded) TealSecondary else TextPrimaryDark
                        )
                    }
                    IconButton(onClick = {
                        webViewRef?.reload()
                        isLoadingWeb = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Web",
                            tint = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = {
                        // Clear session cookies & cache
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        webViewRef?.clearCache(true)
                        webViewRef?.clearFormData()
                        webViewRef?.loadUrl(WHATSAPP_WEB_URL)
                        Toast.makeText(context, "Session cleared. Ready to re-link.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Clear Session",
                            tint = TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Real-time Connection Status Strip
            ConnectionStatusStrip(
                status = connectionStatus,
                isLoading = isLoadingWeb,
                onClickStatus = { viewModel.toggleGuide() }
            )

            // Step-by-Step Connection Guide Banner (collapsible)
            AnimatedVisibility(
                visible = isGuideExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                QrConnectionGuideCard(onDismiss = { viewModel.toggleGuide() })
            }

            // Sub-tabs: "Linked WhatsApp" vs "Captured Vault Gallery"
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = TealSecondary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealSecondary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Linked Web Client")
                        }
                    },
                    selectedContentColor = TealSecondary,
                    unselectedContentColor = TextSecondaryDark
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vault Gallery ($viewOnceCount)")
                        }
                    },
                    selectedContentColor = TealSecondary,
                    unselectedContentColor = TextSecondaryDark
                )
            }

            // Dual view: Both kept alive in memory so WebView session persists seamlessly
            Box(modifier = Modifier.fillMaxSize()) {
                // Tab 0: Embedded WhatsApp Web
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (selectedTab == 0) 1f else 0f)
                ) {
                    if (isLoadingWeb && selectedTab == 0) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = TealSecondary,
                            trackColor = DarkSurface
                        )
                    }

                    // Single-Phone Pairing Banner & Code Card
                    if (connectionStatus != "AUTHENTICATED") {
                        if (pairingCode != null) {
                            PairingCodeCard(
                                pairingCode = pairingCode!!,
                                onCopyCode = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("WhatsApp Pairing Code", pairingCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Pairing code copied!", Toast.LENGTH_SHORT).show()
                                },
                                onOpenWhatsApp = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            SinglePhoneLinkBanner(
                                onLinkWithPhone = {
                                    webViewRef?.evaluateJavascript(
                                        "window.__notiVaultLinkWithPhone && window.__notiVaultLinkWithPhone();",
                                        null
                                    )
                                    Toast.makeText(context, "Requesting phone number link on screen...", Toast.LENGTH_SHORT).show()
                                },
                                onOpenWhatsApp = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            createConfiguredWebView(
                                context = ctx,
                                onLoadingChanged = { isLoadingWeb = it },
                                onMediaCaptured = { b64, mime, isVo ->
                                    viewModel.saveCapturedMedia(b64, mime, isVo)
                                },
                                onStatusUpdate = { status ->
                                    viewModel.updateConnectionStatus(status)
                                },
                                onPairingCode = { code ->
                                    viewModel.updatePairingCode(code)
                                }
                            ).also { webViewRef = it }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                // Tab 1: Captured View-Once Vault Gallery
                if (selectedTab == 1) {
                    ViewOnceGalleryView(
                        mediaList = viewOnceMedia,
                        onMediaClick = { viewModel.openMediaModal(it) },
                        onClearAll = { viewModel.clearAllViewOnceMedia() }
                    )
                }
            }

            // Fullscreen Modal Viewer
            selectedMedia?.let { mediaItem ->
                MediaViewerModal(
                    media = mediaItem,
                    onDismiss = { viewModel.closeMediaModal() },
                    onDelete = { id -> viewModel.deleteMedia(id) }
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusStrip(
    status: String,
    isLoading: Boolean,
    onClickStatus: () -> Unit
) {
    val (statusColor, statusText, statusIcon) = when {
        isLoading -> Triple(Color(0xFF38BDF8), "Loading WhatsApp Web client...", Icons.Default.Refresh)
        status == "AUTHENTICATED" -> Triple(Color(0xFF22C55E), "Linked Device Active • View-Once Auto-Saver Armed", Icons.Default.CheckCircle)
        status == "QR_READY" -> Triple(Color(0xFFFBBF24), "Ready to Link • Link with phone number or scan QR", Icons.Default.QrCodeScanner)
        status == "ERROR" -> Triple(DeletedRed, "Connection issue • Tap reload in top bar", Icons.Default.Warning)
        else -> Triple(TealSecondary, "Initializing Linked Engine...", Icons.Default.AutoAwesome)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusColor.copy(alpha = 0.12f))
            .clickable { onClickStatus() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun QrConnectionGuideCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How View-Once Vault Works",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealSecondary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val steps = listOf(
                "Method 1: Single Phone Link (No QR scan needed):",
                "• Tap 'Link Phone #' above in the banner or on WhatsApp Web.",
                "• Enter your WhatsApp phone number on screen.",
                "• Copy the 8-character pairing code that appears.",
                "• Tap 'Open WhatsApp' > Menu (⋮) > Linked Devices > Link a Device > 'Link with phone number instead' > Enter code!",
                "",
                "Method 2: Using another device:",
                "• Open WhatsApp on another phone and scan the QR code displayed below.",
                "",
                "Once linked, any View-Once photo or video opened is captured in original quality and vaulted permanently!"
            )

            steps.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step.startsWith("Method")) TealSecondary else TextSecondaryDark,
                    fontWeight = if (step.startsWith("Method")) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewOnceGalleryView(
    mediaList: List<MediaEntity>,
    onMediaClick: (MediaEntity) -> Unit,
    onClearAll: () -> Unit
) {
    if (mediaList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No View-Once Media Captured Yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Once linked, any View-Once photo or video opened in WhatsApp is automatically intercepted and saved here in full original quality, surviving sender deletion and view-once limits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mediaList.size} View-Once files safely saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryDark
                )
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear View Once Media",
                        tint = TextSecondaryDark
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = mediaList,
                    key = { it.id }
                ) { media ->
                    ViewOnceGridItem(media = media, onClick = { onMediaClick(media) })
                }
            }
        }
    }
}

@Composable
private fun ViewOnceGridItem(
    media: MediaEntity,
    onClick: () -> Unit
) {
    val file = File(media.internalSavedPath)
    val isVideo = media.mediaType.contains("VIDEO", ignoreCase = true) || media.mimeType.contains("video", ignoreCase = true)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = file,
            contentDescription = media.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // View-Once Badge Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF22C55E).copy(alpha = 0.9f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "VIEW ONCE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isVideo) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Bottom File Size Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(6.dp)
        ) {
            val sizeStr = when {
                media.fileSizeBytes < 1024 -> "${media.fileSizeBytes} B"
                media.fileSizeBytes < 1024 * 1024 -> "${media.fileSizeBytes / 1024} KB"
                else -> String.format(Locale.US, "%.1f MB", media.fileSizeBytes / (1024.0 * 1024.0))
            }
            Text(
                text = sizeStr,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    onLoadingChanged: (Boolean) -> Unit,
    onMediaCaptured: (String, String, Boolean) -> Unit,
    onStatusUpdate: (String) -> Unit,
    onPairingCode: (String) -> Unit
): WebView {
    val webView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val settings = webView.settings
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        userAgentString = DESKTOP_USER_AGENT
        cacheMode = WebSettings.LOAD_DEFAULT
        allowFileAccess = true
        allowContentAccess = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mediaPlaybackRequiresUserGesture = false
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

    // Attach JavaScript Interface Bridge
    val bridge = object {
        @JavascriptInterface
        fun onMediaCaptured(base64Data: String, mimeType: String, isViewOnce: Boolean) {
            onMediaCaptured(base64Data, mimeType, isViewOnce)
        }

        @JavascriptInterface
        fun onStatusUpdate(status: String) {
            onStatusUpdate(status)
        }

        @JavascriptInterface
        fun onPairingCode(code: String) {
            onPairingCode(code)
        }

        @JavascriptInterface
        fun log(msg: String) {
            android.util.Log.d("NotiVaultBridge", msg)
        }
    }
    webView.addJavascriptInterface(bridge, "NotiVaultBridge")

    // Download listener for blob downloads
    webView.setDownloadListener { url, _, _, mimetype, _ ->
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            val script = """
                (function() {
                    fetch('$url').then(r => r.blob()).then(b => {
                        const reader = new FileReader();
                        reader.onloadend = () => {
                            if (window.NotiVaultBridge) {
                                window.NotiVaultBridge.onMediaCaptured(reader.result, b.type, true);
                            }
                        };
                        reader.readAsDataURL(b);
                    }).catch(e => {});
                })();
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onLoadingChanged(true)
            onStatusUpdate("LOADING")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onLoadingChanged(false)
            if (url?.contains("web.whatsapp.com") == true) {
                view?.evaluateJavascript(INJECTED_HOOK_JS, null)
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            onLoadingChanged(false)
            onStatusUpdate("ERROR")
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            request?.grant(request.resources)
        }
    }

    webView.loadUrl(WHATSAPP_WEB_URL)
    return webView
}

@Composable
private fun PairingCodeCard(
    pairingCode: String,
    onCopyCode: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WhatsApp Pairing Code (No Camera Needed)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C2920))
                    .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pairingCode,
                    style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 4.sp),
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopyCode,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Code", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onOpenWhatsApp,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "১. কোডটি কপি করুন। ২. Open WhatsApp চাপুন -> Menu (⋮) -> Linked Devices -> Link a Device -> 'Link with phone number instead' এ গিয়ে কোডটি বসিয়ে দিন।",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun SinglePhoneLinkBanner(
    onLinkWithPhone: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Single Phone Linking",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Can't scan your own phone's screen? Link with phone number instead!",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLinkWithPhone,
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Link Phone #", color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
