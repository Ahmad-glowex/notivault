package com.notivault.app.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notivault.app.ui.screens.chat.ChatDetailScreen
import com.notivault.app.ui.screens.chat.ChatDetailViewModel
import com.notivault.app.ui.screens.deleted.DeletedMessagesScreen
import com.notivault.app.ui.screens.home.HomeScreen
import com.notivault.app.ui.screens.media.MediaGalleryScreen
import com.notivault.app.ui.screens.settings.SettingsScreen

@Composable
fun NotiVaultNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToChat = { threadId ->
                    navController.navigate(Screen.ChatDetail.createRoute(threadId))
                },
                onNavigateToMedia = {
                    navController.navigate(Screen.MediaGallery.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDeleted = {
                    navController.navigate(Screen.DeletedMessages.route)
                }
            )
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("threadId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawThreadId = backStackEntry.arguments?.getString("threadId") ?: ""
            val threadId = Uri.decode(rawThreadId)
            val chatViewModel = viewModel<ChatDetailViewModel>(
                key = threadId,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ChatDetailViewModel(app, threadId) as T
                    }
                }
            )

            ChatDetailScreen(
                threadId = threadId,
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MediaGallery.route) {
            MediaGalleryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeletedMessages.route) {
            DeletedMessagesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { threadId ->
                    navController.navigate(Screen.ChatDetail.createRoute(threadId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
