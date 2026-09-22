package com.notivault.app.data.local

import com.notivault.app.data.local.entity.AppEntity

object CoreApps {
    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_W4B = "com.whatsapp.w4b"
    const val PACKAGE_MESSENGER = "com.facebook.orca"
    const val PACKAGE_TELEGRAM = "org.telegram.messenger"
    const val PACKAGE_INSTAGRAM = "com.instagram.android"

    val CORE_PACKAGES = setOf(
        PACKAGE_WHATSAPP,
        PACKAGE_WHATSAPP_W4B,
        PACKAGE_MESSENGER,
        PACKAGE_TELEGRAM,
        PACKAGE_INSTAGRAM
    )

    fun isCoreApp(packageName: String): Boolean = packageName in CORE_PACKAGES

    fun isWhatsApp(packageName: String): Boolean =
        packageName == PACKAGE_WHATSAPP || packageName == PACKAGE_WHATSAPP_W4B

    val DEFAULT_APPS = listOf(
        AppEntity(
            packageName = PACKAGE_WHATSAPP,
            appName = "WhatsApp",
            isEnabled = true,
            colorHex = "#25D366"
        ),
        AppEntity(
            packageName = PACKAGE_WHATSAPP_W4B,
            appName = "WhatsApp Business",
            isEnabled = true,
            colorHex = "#25D366"
        ),
        AppEntity(
            packageName = PACKAGE_MESSENGER,
            appName = "Messenger",
            isEnabled = true,
            colorHex = "#0084FF"
        ),
        AppEntity(
            packageName = PACKAGE_TELEGRAM,
            appName = "Telegram",
            isEnabled = true,
            colorHex = "#229ED9"
        ),
        AppEntity(
            packageName = PACKAGE_INSTAGRAM,
            appName = "Instagram",
            isEnabled = true,
            colorHex = "#E1306C"
        )
    )

    fun getDefaultColor(packageName: String): String {
        return when (packageName) {
            PACKAGE_WHATSAPP, PACKAGE_WHATSAPP_W4B -> "#25D366"
            PACKAGE_MESSENGER -> "#0084FF"
            PACKAGE_TELEGRAM -> "#229ED9"
            PACKAGE_INSTAGRAM -> "#E1306C"
            else -> "#0D9488"
        }
    }
}
