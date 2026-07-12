package com.guitarvault.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guitarvault.app.ui.screens.*

object Routes {
    const val COLLECTION = "collection"
    const val GUITAR_DETAIL = "guitar_detail/{guitarId}"
    const val ADD_EDIT_GUITAR = "add_edit_guitar/{guitarId}"
    const val WISHLIST = "wishlist"
    const val CAMERA = "camera/{guitarId}"
    const val SPEC_LOOKUP = "spec_lookup/{guitarId}"
    const val DAILY_SPEC = "daily_spec"

    fun guitarDetail(id: String) = "guitar_detail/$id"
    fun addEditGuitar(id: String?) = "add_edit_guitar/${id ?: "new"}"
    fun camera(id: String) = "camera/$id"
    fun specLookup(id: String) = "spec_lookup/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.COLLECTION) {
        composable(Routes.COLLECTION) {
            CollectionScreen(
                onGuitarClick = { id -> navController.navigate(Routes.guitarDetail(id)) },
                onAddGuitar = { navController.navigate(Routes.addEditGuitar(null)) },
                onWishlistClick = { navController.navigate(Routes.WISHLIST) },
                onDailySpec = { navController.navigate(Routes.DAILY_SPEC) }
            )
        }
        composable(
            Routes.GUITAR_DETAIL,
            arguments = listOf(navArgument("guitarId") { type = NavType.StringType })
        ) { backStackEntry ->
            val guitarId = backStackEntry.arguments?.getString("guitarId") ?: return@composable
            GuitarDetailScreen(
                guitarId = guitarId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.addEditGuitar(id)) },
                onDelete = { navController.popBackStack() },
                onOpenCamera = { id -> navController.navigate(Routes.camera(id)) },
                onSpecLookup = { id -> navController.navigate(Routes.specLookup(id)) }
            )
        }
        composable(
            Routes.ADD_EDIT_GUITAR,
            arguments = listOf(navArgument("guitarId") { type = NavType.StringType })
        ) { backStackEntry ->
            val guitarIdArg = backStackEntry.arguments?.getString("guitarId") ?: return@composable
            val guitarId = if (guitarIdArg == "new") null else guitarIdArg
            AddEditGuitarScreen(
                guitarId = guitarId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.WISHLIST) {
            WishlistScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.CAMERA,
            arguments = listOf(navArgument("guitarId") { type = NavType.StringType })
        ) { backStackEntry ->
            val guitarId = backStackEntry.arguments?.getString("guitarId") ?: return@composable
            CameraScreen(
                guitarId = guitarId,
                onBack = { navController.popBackStack() },
                onCaptureComplete = { navController.popBackStack() },
                onPhotoSaved = { photo ->
                    // Photo is saved by the ViewModel via callback in detail screen
                }
            )
        }
        composable(
            Routes.SPEC_LOOKUP,
            arguments = listOf(navArgument("guitarId") { type = NavType.StringType })
        ) { backStackEntry ->
            val guitarId = backStackEntry.arguments?.getString("guitarId") ?: return@composable
            SpecLookupScreen(
                guitarId = guitarId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DAILY_SPEC) {
            DailySpecScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
