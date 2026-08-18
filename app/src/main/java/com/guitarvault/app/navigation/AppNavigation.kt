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
    const val ADD_EDIT_GUITAR = "add_edit_guitar/{guitarId}/{status}"
    const val CAMERA = "camera/{guitarId}"
    const val SPEC_LOOKUP = "spec_lookup/{guitarId}"
    const val RANDOM_SPEC = "random_spec"
    const val LEGAL = "legal"

    fun guitarDetail(id: String) = "guitar_detail/$id"
    fun addEditGuitar(id: String?, status: String = "OWNED") = "add_edit_guitar/${id ?: "new"}/$status"
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
                onAddGuitar = { status -> navController.navigate(Routes.addEditGuitar(null, status.name)) },
                onRandomSpec = { navController.navigate(Routes.RANDOM_SPEC) },
                onLegal = { navController.navigate(Routes.LEGAL) }
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
            arguments = listOf(
                navArgument("guitarId") { type = NavType.StringType },
                navArgument("status") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val guitarIdArg = backStackEntry.arguments?.getString("guitarId") ?: return@composable
            val guitarId = if (guitarIdArg == "new") null else guitarIdArg
            val statusArg = backStackEntry.arguments?.getString("status") ?: "OWNED"
            val initialStatus = try { com.guitarvault.app.data.model.GuitarStatus.valueOf(statusArg) } catch (e: Exception) { com.guitarvault.app.data.model.GuitarStatus.OWNED }
            AddEditGuitarScreen(
                guitarId = guitarId,
                initialStatus = initialStatus,
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
        composable(Routes.RANDOM_SPEC) {
            RandomSpecScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LEGAL) {
            LegalScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
