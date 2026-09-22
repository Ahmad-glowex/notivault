package com.notivault.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Modern Material 3 styled Switch with high-contrast tactile thumb and track colors.
 * When active, displays an elevated white circular thumb with a primary check icon sliding across a rich primary track.
 * When inactive, displays an elevated white thumb and smooth bordered track.
 * Emits tactile haptic feedback on toggle for premium physical feel.
 */
@Composable
fun VaultSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    Switch(
        checked = checked,
        onCheckedChange = { newState ->
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            onCheckedChange(newState)
        },
        enabled = enabled,
        modifier = modifier,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color.White,
            uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
            uncheckedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
            uncheckedIconColor = Color.Transparent,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledUncheckedThumbColor = if (isDark) Color(0xFF64748B) else Color(0xFFE2E8F0),
            disabledUncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        )
    )
}
