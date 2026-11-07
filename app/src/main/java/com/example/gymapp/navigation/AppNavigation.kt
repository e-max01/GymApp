package com.example.gymapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymapp.screens.DietScreen
import com.example.gymapp.screens.ExercisesScreen
import com.example.gymapp.screens.FoodListScreen
import com.example.gymapp.screens.HomeScreen
import com.example.gymapp.screens.ProfileScreen
import com.example.gymapp.screens.SettingsScreen
import com.example.gymapp.screens.WishlistScreen
import com.example.gymapp.screens.WorkoutPlansScreen

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = "main",
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable("main") {
            MainScreen(rootNavController = rootNavController)
        }
        composable("schede") {
            WorkoutPlansScreen(navController = rootNavController)
        }
        composable("dieta") {
            DietScreen(navController = rootNavController)
        }
        composable("food_list") {
            FoodListScreen(navController = rootNavController)
        }
        composable("esercizi") {
            ExercisesScreen(navController = rootNavController)
        }
        composable("wishlist") {
            WishlistScreen(navController = rootNavController)
        }
    }
}

@Composable
fun MainScreen(rootNavController: NavController) {
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = { GymBottomBar(navController = tabNavController) }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable("home") {
                HomeScreen(navController = rootNavController)
            }
            composable("profilo") {
                ProfileScreen()
            }
            composable("impostazioni") {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun GymBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        NavigationBar(
            modifier = Modifier
                .height(67.dp)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                indicatorColor = Color.Transparent
            )

            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                selected = currentRoute == "home",
                onClick = {
                    if (currentRoute != "home") {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                },
                colors = itemColors
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profilo") },
                selected = currentRoute == "profilo",
                onClick = {
                    if (currentRoute != "profilo") {
                        navController.navigate("profilo") {
                            popUpTo("home")
                        }
                    }
                },
                colors = itemColors
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
                selected = currentRoute == "impostazioni",
                onClick = {
                    if (currentRoute != "impostazioni") {
                        navController.navigate("impostazioni") {
                            popUpTo("home")
                        }
                    }
                },
                colors = itemColors
            )
        }
    }
}