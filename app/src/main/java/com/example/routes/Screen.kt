package com.example.routes

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Images : Screen("images")
    object History : Screen("history")
    object Settings : Screen("settings")
}
