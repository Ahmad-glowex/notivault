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

        // Ensure WhatsApp tab is first if any WhatsApp variant is enabled
        val hasWhatsApp = enabledApps.any { CoreApps.isWhatsApp(it.packageName) && it.isEnabled }
        if (hasWhatsApp) {
            list.add(DynamicTabItem("WhatsApp", CoreApps.PACKAGE_WHATSAPP))
            enabledApps.filter { CoreApps.isWhatsApp(it.packageName) }.forEach { seen.add(it.packageName) }
        }

        // Messenger
        val hasMessenger = enabledApps.any { CoreApps.isMessenger(it.packageName) && it.isEnabled }
        if (hasMessenger) {
            list.add(DynamicTabItem("Messenger", CoreApps.PACKAGE_MESSENGER))
            enabledApps.filter { CoreApps.isMessenger(it.packageName) }.forEach { seen.add(it.packageName) }
        }

        // Telegram
        val hasTelegram = enabledApps.any { CoreApps.isTelegram(it.packageName) && it.isEnabled }
        if (hasTelegram) {
            list.add(DynamicTabItem("Telegram", CoreApps.PACKAGE_TELEGRAM))
            enabledApps.filter { CoreApps.isTelegram(it.packageName) }.forEach { seen.add(it.packageName) }
        }

        // Instagram
        val hasInstagram = enabledApps.any { CoreApps.isInstagram(it.packageName) && it.isEnabled }
        if (hasInstagram) {
            list.add(DynamicTabItem("Instagram", CoreApps.PACKAGE_INSTAGRAM))
            enabledApps.filter { CoreApps.isInstagram(it.packageName) }.forEach { seen.add(it.packageName) }
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
            val isSelected = when {
                tab.packageName == null -> selectedPackage == null
                CoreApps.isWhatsApp(tab.packageName) -> CoreApps.isWhatsApp(selectedPackage ?: "")
                CoreApps.isTelegram(tab.packageName) -> CoreApps.isTelegram(selectedPackage ?: "")
                CoreApps.isMessenger(tab.packageName) -> CoreApps.isMessenger(selectedPackage ?: "")
                CoreApps.isInstagram(tab.packageName) -> CoreApps.isInstagram(selectedPackage ?: "")
                else -> selectedPackage == tab.packageName
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
