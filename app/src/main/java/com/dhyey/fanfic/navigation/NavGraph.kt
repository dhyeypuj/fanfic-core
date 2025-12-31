package com.dhyey.fanfic.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhyey.fanfic.ui.addfic.AddFicScreen
import com.dhyey.fanfic.ui.details.DetailsScreen
import com.dhyey.fanfic.ui.library.LibraryScreen
import com.dhyey.fanfic.ui.reader.ReaderScreen
import com.dhyey.fanfic.ui.settings.SettingsScreen

sealed class Route(val route: String) {
    data object Library : Route("library")
    data object AddFic : Route("add")
    data object Settings : Route("settings")
    data object Details : Route("details/{ficId}") {
        fun createRoute(ficId: String) = "details/$ficId"
    }
    data object Reader : Route("reader/{ficId}/{chapter}") {
        fun createRoute(ficId: String, chapter: Int) = "reader/$ficId/$chapter"
    }
}

@Composable
fun FanficNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Library.route
    ) {
        composable(Route.Library.route) {
            LibraryScreen(
                onFicClick = { ficId ->
                    navController.navigate(Route.Details.createRoute(ficId))
                },
                onAddClick = {
                    navController.navigate(Route.AddFic.route)
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings.route)
                }
            )
        }

        composable(Route.AddFic.route) {
            AddFicScreen(
                onNavigateBack = { navController.popBackStack() },
                onFicAdded = { ficId ->
                    navController.popBackStack()
                    navController.navigate(Route.Details.createRoute(ficId))
                }
            )
        }

        composable(Route.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Details.route,
            arguments = listOf(navArgument("ficId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ficId = backStackEntry.arguments?.getString("ficId") ?: return@composable
            DetailsScreen(
                ficId = ficId,
                onNavigateBack = { navController.popBackStack() },
                onChapterClick = { chapter ->
                    navController.navigate(Route.Reader.createRoute(ficId, chapter))
                }
            )
        }

        composable(
            route = Route.Reader.route,
            arguments = listOf(
                navArgument("ficId") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val ficId = backStackEntry.arguments?.getString("ficId") ?: return@composable
            val chapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            ReaderScreen(
                ficId = ficId,
                initialChapter = chapter,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
