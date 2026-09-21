package com.notivault.app.ui.screens.viewonce

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.notivault.app.NotiVaultApp
import com.notivault.app.service.media.MediaObserverService
import com.notivault.app.service.media.RootViewOnceManager
import com.notivault.app.service.media.ViewOnceSimulator
import com.notivault.app.ui.theme.DarkBackground
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnceCompanionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as NotiVaultApp
    val coroutineScope = rememberCoroutineScope()
    val viewOnceCount by app.mediaRepository.getViewOnceMediaCount().collectAsState(initial = 0)
    var isSimulating by remember { mutableStateOf(false) }
    val isRoot = remember { RootViewOnceManager.isRootAvailable() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "View-Once Vault Hub",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRoot) "Root Sandbox Sniper: ARMED" else "Dual Pipeline: Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealSecondary
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRoot) Icons.Default.FlashOn else Icons.Default.Radar,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isRoot) "Root Sandbox Extractor" else "Direct Staging Observer",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isRoot) "Direct /data/data sandbox access" else "Monitoring media pipeline & staging",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Preserved View-Once Media",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "$viewOnceCount Items Preserved",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onNavigateToGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Gallery", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Test & Verification Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2E2B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ভেরিফিকেশন টেস্ট (Chat Bubble Test)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "এই বাটনে চাপ দিলে একটি রিয়েল টেস্ট ইমেজ তৈরি হয়ে আপনার লেটেস্ট ভিউ-ওয়ান্স মেসেজে সরাসরি যুক্ত হবে। এরপর চ্যাট স্ক্রিনে ঢুকলেই দেখতে পাবেন '📷 ① Sent a photo' টেক্সট আর নেই, সরাসরি ছবি দেখা যাচ্ছে!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSimulating = true
                                val success = ViewOnceSimulator.injectTestViewOnceMedia(context)
                                isSimulating = false
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "টেস্ট সফল! এখন চ্যাট ওপেন করে ভিউ-ওয়ান্স ছবি সরাসরি দেখুন।",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(context, "টেস্ট ব্যর্থ হয়েছে।", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSimulating,
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSimulating) "ইনজেকশন চলছে..." else "চ্যাট বাবল টেস্ট চালান (Inject Test Image)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Operations & Manual Trigger Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How View-Once Extraction Works",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "১. স্বয়ংক্রিয় ডিটেকশন: সেন্ডার যখন '① Photo' পাঠায়, NotiVault তাৎক্ষণিক হাই-স্পিড স্নাইপার সক্রিয় করে।\n\n" +
                                "২. রুট স্যান্ডবক্স মোড: ফোন রুটেড থাকলে NotiVault সরাসরি WhatsApp-এর এনক্রিপ্টেড ফোল্ডার (/data/data/com.whatsapp/files/ViewOnce) থেকে বাইনারি ম্যাজিক বাইটস ডিটেক্ট করে ছবি সংগ্রহ করে চ্যাট বাবলে শো করায়।\n\n" +
                                "৩. নন-রুট মোড (LSPatch / WaEnhancer): নন-রুট ডিভাইসে WhatsApp-এর ইন্টিগ্রেশন চালু থাকলে WhatsApp স্বয়ংক্রিয়ভাবে ছবি সেভ করে এবং NotiVault তা মেসেজে সংযুক্ত করে ফেলে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val rootExtracted = if (isRoot) RootViewOnceManager.extractViewOnceFiles(context) else 0
                                MediaObserverService.triggerViewOnceSniff(context, "com.whatsapp")
                                if (rootExtracted > 0) {
                                    Toast.makeText(context, "$rootExtracted টি View-Once মিডিয়া রুট দিয়ে উদ্ধার হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "স্নাইপার স্ক্যান সফলভাবে সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ম্যানুয়াল স্নাইপার স্ক্যান চালান")
                    }
                }
            }

            // Architecture Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ডিভাইস সাপোর্ট ও পারমিশন স্ট্যাটাস",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRoot) {
                            "✅ আপনার ডিভাইসে Root Privilege সক্রিয় আছে। NotiVault সরাসরি রুট এক্সিকিউশনের মাধ্যমে অভ্যন্তরীণ স্যান্ডবক্স থেকে ভিউ-ওয়ান্স ফাইল উদ্ধার করতে সক্ষম।"
                        } else {
                            "ℹ️ স্ট্যান্ডার্ড (নন-রুট) অ্যান্ড্রয়েড সিকিউরিটিতে কোনো অ্যাপ সরাসরি অন্য অ্যাপের অভ্যন্তরীণ ডেটা অ্যাক্সেস করতে পারে না। নন-রুট ডিভাইসে LSPatch + WaEnhancer ব্যবহার করলে ভিউ-ওয়ান্স ছবি সরাসরি সাধারণ ছবির মতো চলে আসে এবং NotiVault তা চ্যাট বাবলে সরাসরি শো করে।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            // Privacy Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1F0F766E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "জিরো-নেটওয়ার্ক গ্যারান্টি: সম্পূর্ণ প্রসেসিং আপনার ফোনের ইন্টারনাল মেমোরিতে সম্পন্ন হয়। NotiVault কোনো সার্ভারে ডাটা পাঠায় না।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}
