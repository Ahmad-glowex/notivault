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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DeletedRedBg)
            .border(1.dp, DeletedBadgeBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DeleteForever,
            contentDescription = "Deleted",
            tint = DeletedRed,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "DELETED BY SENDER ($timeStr) • PRESERVED",
            style = MaterialTheme.typography.labelSmall,
            color = DeletedBadgeBorder,
            fontWeight = FontWeight.Bold
        )
    }
}
