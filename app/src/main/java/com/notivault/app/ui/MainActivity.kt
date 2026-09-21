package com.notivault.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.notivault.app.NotiVaultApp
import com.notivault.app.security.BiometricAuthManager
import com.notivault.app.ui.navigation.NotiVaultNavGraph
import com.notivault.app.ui.theme.DarkBackground
import com.notivault.app.ui.theme.NotiVaultTheme
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthManager = BiometricAuthManager(this)

        val app = application as NotiVaultApp
        val settingsRepo = app.settingsRepository

        lifecycleScope.launch {
            try {
                if (settingsRepo.isMediaBackupEnabled.first()) {
                    com.notivault.app.service.media.MediaObserverService.start(this@MainActivity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            val isSecureWindow by settingsRepo.isSecureWindowEnabled.collectAsState(initial = false)
            val isBiometricEnabled by settingsRepo.isBiometricLockEnabled.collectAsState(initial = false)

            var isUnlocked by remember { mutableStateOf(!settingsRepo.isAppLocked()) }

            LaunchedEffect(isSecureWindow) {
                if (isSecureWindow) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isUnlocked) {
                    promptUnlock { isUnlocked = true }
                }
            }

            NotiVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBiometricEnabled && !isUnlocked) {
                        LockedScreen(
                            onUnlockClick = {
                                promptUnlock { isUnlocked = true }
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        NotiVaultNavGraph(navController = navController)
                    }
                }
            }
        }
    }

    private fun promptUnlock(onSuccess: () -> Unit) {
        if (biometricAuthManager.canAuthenticate()) {
            biometricAuthManager.promptBiometric(
                onSuccess = onSuccess,
                onError = { /* Keep locked */ }
            )
        } else {
            // Biometric not configured or unavailable on device, allow access
            onSuccess()
        }
    }
}

@Composable
fun LockedScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Locked",
                tint = TealSecondary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NotiVault is Locked",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authenticate to access private message logs",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
            ) {
                Text(text = "Unlock Vault", color = Color.Black)
            }
        }
    }
}
