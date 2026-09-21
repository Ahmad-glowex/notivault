package com.notivault.app.ui.screens.home.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.notivault.app.ui.theme.DeletedBadgeBorder
import com.notivault.app.ui.theme.DeletedRed
import com.notivault.app.ui.theme.DeletedRedBg

fun isNotificationAccessGranted(context: Context): Boolean {
    val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledListeners.contains(context.packageName)
}

@Composable
fun PermissionStatusBanner(
    isGranted: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGranted) return

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DeletedRedBg)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = DeletedRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Notification Access Required",
                style = MaterialTheme.typography.titleSmall,
                color = DeletedBadgeBorder,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "NotiVault needs Notification Listener permission to intercept messages and detect deleted notifications locally on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE2E8F0)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                    onRefresh()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeletedRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Grant Permission",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
