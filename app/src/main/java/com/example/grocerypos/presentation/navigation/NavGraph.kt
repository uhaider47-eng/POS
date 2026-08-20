package com.example.grocerypos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.grocerypos.presentation.dashboard.DashboardScreen
import com.example.grocerypos.presentation.pos.PosScreen
import com.example.grocerypos.presentation.products.AddEditProductScreen
import com.example.grocerypos.presentation.products.ProductListScreen
import com.example.grocerypos.presentation.settings.SettingsScreen
import com.example.grocerypos.presentation.setup.ShopSetupScreen

@Composable
fun GroceryPosNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            ShopSetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPos = {
                    navController.navigate(Screen.Pos.route)
                },
                onNavigateToProducts = {
                    navController.navigate(Screen.Products.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Pos.route) {
            PosScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Products.route) {
            ProductListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.route) },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct.createRoute(productId))
                }
            )
        }

        composable(Screen.AddProduct.route) {
            AddEditProductScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) {
            AddEditProductScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
