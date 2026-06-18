package com.rhyn.reach.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Grey300,
    onPrimary = Black,
    secondary = Grey300,
    onSecondary = Black,
    background = Grey900,
    onBackground = White,
    surface = Grey800,
    onSurface = White,
    surfaceVariant = Grey900,
    onSurfaceVariant = Grey300
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Grey800,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = Grey100,
    onSurface = Black,
    surfaceVariant = White,
    onSurfaceVariant = Grey500
)

@Composable
fun ReachTheme(
    darkTheme: Boolean, // Removed default value to force explicit passing
    content: @Composable () -> Unit
) {
    // Determine scheme based solely on the passed boolean
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}