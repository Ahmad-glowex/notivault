package com.notivault.app.ui.screens.viewonce

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.notivault.app.NotiVaultApp
import com.notivault.app.service.media.MediaCacheManager
import com.notivault.app.ui.theme.DarkBackground
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnceCompanionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var connectionStatus by remember { mutableStateOf("Initializing...") }
    val cacheManager = remember { MediaCacheManager(context) }
    val app = context.applicationContext as NotiVaultApp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "View-Once Companion Bridge",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = connectionStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectionStatus.contains("Connected", ignoreCase = true)) TealSecondary else TextSecondaryDark
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
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = {
                        CookieManager.getInstance().removeAllCookies(null)
                        webViewRef?.clearCache(true)
                        webViewRef?.loadUrl("https://web.whatsapp.com")
                        connectionStatus = "Logged out. Scan QR to reconnect."
                        Toast.makeText(context, "Session disconnected", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Unlink",
                            tint = DeletedRed
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
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = TealSecondary
                )
            }

            // Info & Guide Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WhatsApp Multi-Device View-Once Bridge",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Link via WhatsApp > Linked Devices > Link a Device. View-Once media will auto-save to your vault upon receiving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // WebView Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        createCompanionWebView(
                            context = ctx,
                            onLoadingChange = { loading -> isLoading = loading },
                            onStatusChange = { status -> connectionStatus = status },
                            onMediaIntercepted = { base64Data, mimeType, caption ->
                                coroutineScope.launch {
                                    val savedFile = cacheManager.cacheBase64Data(
                                        base64Data = base64Data,
                                        mimeType = mimeType,
                                        prefix = "view_once"
                                    )
                                    if (savedFile != null) {
                                        val mediaType = if (mimeType.startsWith("video")) "VIEW_ONCE_VIDEO" else "VIEW_ONCE_IMAGE"
                                        val now = System.currentTimeMillis()
                                        val msgDao = app.database.messageDao()
                                        val pendingMsg = msgDao.getLatestPendingMediaMessage("com.whatsapp", now - 3600000L)

                                        if (pendingMsg != null) {
                                            msgDao.updateMessageMedia(pendingMsg.id, savedFile.absolutePath, mimeType)
                                        }

                                        app.mediaRepository.saveCachedMedia(
                                            packageName = "com.whatsapp",
                                            originalPath = "companion_stream_${System.currentTimeMillis()}",
                                            internalSavedPath = savedFile.absolutePath,
                                            fileName = savedFile.name,
                                            mimeType = mimeType,
                                            fileSizeBytes = savedFile.length(),
                                            mediaType = mediaType,
                                            threadId = pendingMsg?.threadId ?: "com.whatsapp_view_once",
                                            messageId = pendingMsg?.id
                                        )

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Preserved 1 View-Once media in Vault!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        ).also { webViewRef = it }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCompanionWebView(
    context: Context,
    onLoadingChange: (Boolean) -> Unit,
    onStatusChange: (String) -> Unit,
    onMediaIntercepted: (String, String, String) -> Unit
): WebView {
    val webView = WebView(context)
    val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    webView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        userAgentString = desktopUserAgent
        useWideViewPort = true
        loadWithOverviewMode = true
        allowFileAccess = false
        allowContentAccess = false
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }

    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }

    val bridge = object {
        @JavascriptInterface
        fun onMediaCaptured(base64Data: String, mimeType: String, caption: String) {
            onMediaIntercepted(base64Data, mimeType, caption)
        }

        @JavascriptInterface
        fun onStatusUpdate(status: String) {
            onStatusChange(status)
        }
    }
    webView.addJavascriptInterface(bridge, "NotiVaultBridge")

    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onLoadingChange(true)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onLoadingChange(false)
            onStatusChange("Connected / Ready")

            // Inject media download interceptor script
            val interceptorScript = """
                (function() {
                    if (window.__notiVaultInjected) return;
                    window.__notiVaultInjected = true;

                    // Intercept blob creation and media fetch
                    const originalFetch = window.fetch;
                    window.fetch = async function(...args) {
                        const response = await originalFetch.apply(this, args);
                        try {
                            const clone = response.clone();
                            const contentType = clone.headers.get('content-type') || '';
                            if (contentType.startsWith('image/') || contentType.startsWith('video/')) {
                                clone.blob().then(blob => {
                                    if (blob.size > 2048) {
                                        const reader = new FileReader();
                                        reader.onloadend = () => {
                                            if (window.NotiVaultBridge) {
                                                window.NotiVaultBridge.onMediaCaptured(reader.result, blob.type, '');
                                            }
                                        };
                                        reader.readAsDataURL(blob);
                                    }
                                }).catch(e => {});
                            }
                        } catch(e) {}
                        return response;
                    };
                })();
            """.trimIndent()
            view?.evaluateJavascript(interceptorScript, null)
        }
    }

    webView.webChromeClient = WebChromeClient()
    webView.loadUrl("https://web.whatsapp.com")
    return webView
}
