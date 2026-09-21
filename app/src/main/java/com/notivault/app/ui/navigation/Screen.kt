package com.notivault.app.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object MediaGallery : Screen("media_gallery")
    data object DeletedMessages : Screen("deleted_messages")
    data object Settings : Screen("settings")

    data object ChatDetail : Screen("chat_detail/{threadId}") {
        fun createRoute(threadId: String): String = "chat_detail/${Uri.encode(threadId)}"
    }
}
