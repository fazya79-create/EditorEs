package com.editor.es.ui.navigation

import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.editor.es.R
import com.editor.es.ui.screens.EditorScreen
import com.editor.es.ui.screens.HomeScreen
import com.editor.es.ui.screens.PlaceholderScreen
import com.editor.es.ui.screens.ProjectFileListScreen
import java.io.File

enum class EditorEsRoute(val path: String) {
    Home("home"),
    OpenProject("open_project"),
    Terminal("terminal"),
    Settings("settings"),
    Files("files"),
    Editor("editor")
}

fun editorFilesRoute(path: String): String = EditorEsRoute.Files.path + "/" + Uri.encode(path)

fun editorFileRoute(path: String): String = EditorEsRoute.Editor.path + "/" + Uri.encode(path)

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
            HomeScreen(
                onNavigate = { route -> navController.navigate(route.path) },
                onProjectCreated = { path -> navController.navigate(editorFilesRoute(path)) }
            )
        }
        composable(
            route = EditorEsRoute.Files.path + "/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectPath = backStackEntry.arguments?.getString("path").orEmpty()
            ProjectFileListScreen(
                projectDir = File(projectPath),
                onBack = { navController.popBackStack() },
                onOpenFile = { filePath -> navController.navigate(editorFileRoute(filePath)) }
            )
        }
        composable(
            route = EditorEsRoute.Editor.path + "/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("path").orEmpty()
            EditorScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() }
            )
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
