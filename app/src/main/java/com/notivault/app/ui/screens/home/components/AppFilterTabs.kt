package com.notivault.app.ui.screens.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivault.app.data.local.CoreApps
import com.notivault.app.data.local.entity.AppEntity

enum class AppTab(val label: String, val packageName: String?) {
    ALL("All", null),
    WHATSAPP("WhatsApp", "com.whatsapp"),
    TELEGRAM("Telegram", "org.telegram.messenger"),
    MESSENGER("Messenger", "com.facebook.orca"),
    INSTAGRAM("Instagram", "com.instagram.android")
}

data class DynamicTabItem(
    val label: String,
    val packageName: String?
)

@Composable
fun DynamicAppFilterTabs(
    enabledApps: List<AppEntity>,
    selectedPackage: String?,
    onPackageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val tabs = remember(enabledApps) {
        val list = mutableListOf(DynamicTabItem("All", null))
        val seen = mutableSetOf<String>()

        // Ensure WhatsApp tab is first if either WhatsApp or WhatsApp Business is enabled
        val hasWhatsApp = enabledApps.any { CoreApps.isWhatsApp(it.packageName) && it.isEnabled }
        if (hasWhatsApp) {
            list.add(DynamicTabItem("WhatsApp", CoreApps.PACKAGE_WHATSAPP))
            seen.add(CoreApps.PACKAGE_WHATSAPP)
            seen.add(CoreApps.PACKAGE_WHATSAPP_W4B)
        }

        // Messenger
        val messengerApp = enabledApps.find { it.packageName == CoreApps.PACKAGE_MESSENGER && it.isEnabled }
        if (messengerApp != null) {
            list.add(DynamicTabItem("Messenger", CoreApps.PACKAGE_MESSENGER))
            seen.add(CoreApps.PACKAGE_MESSENGER)
        }

        // Telegram
        val telegramApp = enabledApps.find { it.packageName == CoreApps.PACKAGE_TELEGRAM && it.isEnabled }
        if (telegramApp != null) {
            list.add(DynamicTabItem("Telegram", CoreApps.PACKAGE_TELEGRAM))
            seen.add(CoreApps.PACKAGE_TELEGRAM)
        }

        // Instagram
        val instagramApp = enabledApps.find { it.packageName == CoreApps.PACKAGE_INSTAGRAM && it.isEnabled }
        if (instagramApp != null) {
            list.add(DynamicTabItem("Instagram", CoreApps.PACKAGE_INSTAGRAM))
            seen.add(CoreApps.PACKAGE_INSTAGRAM)
        }

        // Any custom added enabled apps
        for (app in enabledApps) {
            if (app.isEnabled && !seen.contains(app.packageName)) {
                seen.add(app.packageName)
                list.add(DynamicTabItem(app.appName, app.packageName))
            }
        }
        list
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = if (tab.packageName == null) {
                selectedPackage == null
            } else if (tab.packageName == CoreApps.PACKAGE_WHATSAPP) {
                selectedPackage == CoreApps.PACKAGE_WHATSAPP || selectedPackage == CoreApps.PACKAGE_WHATSAPP_W4B
            } else {
                selectedPackage == tab.packageName
            }

            FilterChip(
                selected = isSelected,
                onClick = { onPackageSelected(tab.packageName) },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun AppFilterTabs(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
