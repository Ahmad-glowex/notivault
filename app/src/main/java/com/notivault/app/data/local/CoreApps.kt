package com.notivault.app.data.local

import com.notivault.app.data.local.entity.AppEntity

object CoreApps {
    // WhatsApp packages
    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_W4B = "com.whatsapp.w4b"
    const val PACKAGE_GBWHATSAPP = "com.gbwhatsapp"
    const val PACKAGE_FMWHATSAPP = "com.fmwhatsapp"
    const val PACKAGE_YOWHATSAPP = "com.yowhatsapp"

    // Messenger packages
    const val PACKAGE_MESSENGER = "com.facebook.orca"
    const val PACKAGE_MESSENGER_LITE = "com.facebook.mlite"

    // Telegram packages
    const val PACKAGE_TELEGRAM = "org.telegram.messenger"
    const val PACKAGE_TELEGRAM_WEB = "org.telegram.messenger.web"
    const val PACKAGE_TELEGRAM_BETA = "org.telegram.messenger.beta"
    const val PACKAGE_TELEGRAM_X = "org.thunderdog.challegram"
    const val PACKAGE_TELEGRAM_PLUS = "org.telegram.plus"
    const val PACKAGE_NEKOGRAM = "nekox.rubymemory.nekogram"

    // Instagram packages
    const val PACKAGE_INSTAGRAM = "com.instagram.android"
    const val PACKAGE_INSTAGRAM_LITE = "com.instagram.lite"

    val CORE_PACKAGES = setOf(
        PACKAGE_WHATSAPP,
        PACKAGE_WHATSAPP_W4B,
        PACKAGE_GBWHATSAPP,
        PACKAGE_FMWHATSAPP,
        PACKAGE_YOWHATSAPP,
        PACKAGE_MESSENGER,
        PACKAGE_MESSENGER_LITE,
        PACKAGE_TELEGRAM,
        PACKAGE_TELEGRAM_WEB,
        PACKAGE_TELEGRAM_BETA,
        PACKAGE_TELEGRAM_X,
        PACKAGE_TELEGRAM_PLUS,
        PACKAGE_NEKOGRAM,
        PACKAGE_INSTAGRAM,
        PACKAGE_INSTAGRAM_LITE
    )

    fun isWhatsApp(packageName: String): Boolean =
        packageName in setOf(
            PACKAGE_WHATSAPP,
            PACKAGE_WHATSAPP_W4B,
            PACKAGE_GBWHATSAPP,
            PACKAGE_FMWHATSAPP,
            PACKAGE_YOWHATSAPP
        )

    fun isTelegram(packageName: String): Boolean =
        packageName in setOf(
            PACKAGE_TELEGRAM,
            PACKAGE_TELEGRAM_WEB,
            PACKAGE_TELEGRAM_BETA,
            PACKAGE_TELEGRAM_X,
            PACKAGE_TELEGRAM_PLUS,
            PACKAGE_NEKOGRAM
        )

    fun isMessenger(packageName: String): Boolean =
        packageName in setOf(PACKAGE_MESSENGER, PACKAGE_MESSENGER_LITE)

    fun isInstagram(packageName: String): Boolean =
        packageName in setOf(PACKAGE_INSTAGRAM, PACKAGE_INSTAGRAM_LITE)

    fun isCoreApp(packageName: String): Boolean = packageName in CORE_PACKAGES

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
        return when {
            isWhatsApp(packageName) -> "#25D366"
            isMessenger(packageName) -> "#0084FF"
            isTelegram(packageName) -> "#229ED9"
            isInstagram(packageName) -> "#E1306C"
            else -> "#0D9488"
        }
    }
}
