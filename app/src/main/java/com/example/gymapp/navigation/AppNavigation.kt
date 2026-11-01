package com.example.gymapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymapp.screens.DietScreen
import com.example.gymapp.screens.ExercisesScreen
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

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
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
            }
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
            }
        )
    }
}