package com.editor.es.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.editor.es.R
import com.editor.es.ui.screens.HomeScreen
import com.editor.es.ui.screens.PlaceholderScreen

enum class EditorEsRoute(val path: String) {
    Home("home"),
    OpenProject("open_project"),
    Terminal("terminal"),
    Settings("settings")
}

@Composable
fun EditorEsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = EditorEsRoute.Home.path,
        modifier = modifier,
        enterTransition = {
            slideInVertically(initialOffsetY = { height -> height / 12 }, animationSpec = tween(320)) +
                fadeIn(animationSpec = tween(320))
        },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(260)) },
        popExitTransition = {
            slideOutVertically(targetOffsetY = { height -> height / 10 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(EditorEsRoute.Home.path) {
            HomeScreen(onNavigate = { route -> navController.navigate(route.path) })
        }
        composable(EditorEsRoute.OpenProject.path) {
            PlaceholderScreen(
                title = stringResource(R.string.open_project),
                icon = Icons.Outlined.FolderOpen,
                onBack = { navController.popBackStack() }
            )
        }
        composable(EditorEsRoute.Terminal.path) {
            PlaceholderScreen(
                title = stringResource(R.string.terminal),
                icon = Icons.Outlined.Terminal,
                onBack = { navController.popBackStack() }
            )
        }
        composable(EditorEsRoute.Settings.path) {
            PlaceholderScreen(
                title = stringResource(R.string.settings),
                icon = Icons.Outlined.Settings,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
