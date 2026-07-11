package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.providers.MainViewModel
import com.example.providers.MainViewModelFactory
import com.example.routes.Screen
import com.example.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.widgets.ImageViewerDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(applicationContext))
            
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Handle Fullscreen Image Viewer Overlays
                val activeViewerItem by viewModel.activeViewerItem.collectAsState()
                activeViewerItem?.let { item ->
                    ImageViewerDialog(
                        item = item,
                        viewModel = viewModel,
                        onDismiss = { viewModel.activeViewerItem.value = null }
                    )
                }

                val mainTabs = listOf(
                    Screen.Home,
                    Screen.Images,
                    Screen.History,
                    Screen.Settings
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Only show bottom navigation on primary screens, not on generator screen
                        if (currentRoute in mainTabs.map { it.route }) {
                            NavigationBar {
                                mainTabs.forEach { tab ->
                                    val isSelected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != tab.route) {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    Screen.Home -> Icons.Default.Home
                                                    Screen.Images -> Icons.Default.Collections
                                                    Screen.History -> Icons.Default.History
                                                    Screen.Settings -> Icons.Default.Settings
                                                },
                                                contentDescription = tab.route
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = when (tab) {
                                                    Screen.Home -> "Home"
                                                    Screen.Images -> "Images"
                                                    Screen.History -> "History"
                                                    Screen.Settings -> "Settings"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToGenerator = { navController.navigate("generator") },
                                onNavigateToGallery = { navController.navigate(Screen.Images.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) }
                            )
                        }
                        composable("generator") {
                            ImageGeneratorScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Images.route) {
                            GalleryScreen(
                                viewModel = viewModel,
                                onNavigateToGenerator = { navController.navigate("generator") }
                            )
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToGenerator = { navController.navigate("generator") }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
