package com.notivault.app.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.notivault.app.ui.theme.DeletedBadgeBorder
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.DeletedRedBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeletedBadge(
    deletedTimestamp: Long?,
    modifier: Modifier = Modifier
) {
    val timeStr = deletedTimestamp?.let {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
    } ?: "recently"

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val badgeBgColor = if (isDark) DeletedRed.copy(alpha = 0.18f) else Color(0xFFFEE2E2)
    val badgeBorderColor = if (isDark) DeletedRed.copy(alpha = 0.45f) else Color(0xFFFECACA)
    val badgeTextColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
    val iconTint = if (isDark) DeletedRed else Color(0xFFDC2626)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeBgColor)
            .border(1.dp, badgeBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = "Deleted",
            tint = iconTint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "Preserved Unsent Message • $timeStr",
            style = MaterialTheme.typography.labelSmall,
            color = badgeTextColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
