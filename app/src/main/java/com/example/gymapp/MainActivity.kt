package com.example.gymapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.gymapp.navigation.AppNavigation
import com.example.gymapp.ui.theme.GymAppTheme
import com.example.gymapp.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(applicationContext)

        setContent {
            val darkTheme = when (ThemeManager.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            GymAppTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }
}