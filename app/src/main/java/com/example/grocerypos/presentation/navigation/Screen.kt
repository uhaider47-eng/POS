package com.example.grocerypos.presentation.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Dashboard : Screen("dashboard")
    object Products : Screen("products")
    object AddProduct : Screen("products/add")
    object EditProduct : Screen("products/edit/{productId}") {
        fun createRoute(productId: String) = "products/edit/$productId"
    }
    object Settings : Screen("settings")
}
