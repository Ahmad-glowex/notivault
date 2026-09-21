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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.TealSecondary

enum class AppTab(val label: String, val packageName: String?) {
    ALL("All", null),
    WHATSAPP("WhatsApp", "com.whatsapp"),
    MESSENGER("Messenger", "com.facebook.orca"),
    INSTAGRAM("Instagram", "com.instagram.android"),
    TELEGRAM("Telegram", "org.telegram.messenger"),
    SAVED_MEDIA("Saved Media", "MEDIA_TAB")
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
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealSecondary,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurface,
                    labelColor = Color.White
                )
            )
        }
    }
}
