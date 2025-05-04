package com.example.bestbikeday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

object ThemeManager {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }
}

val LocalThemeManager = staticCompositionLocalOf { ThemeManager }

@Composable
fun BestBikeDayTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        ThemeManager.isDarkTheme() -> darkColorScheme(
            primary = Color(0xFF81C784),
            secondary = Color(0xFF4CAF50),
            tertiary = Color(0xFF2E7D32),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White
        )
        else -> lightColorScheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFF81C784),
            tertiary = Color(0xFF2E7D32),
            background = Color.White,
            surface = Color(0xFFF5F5F5),
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
} 