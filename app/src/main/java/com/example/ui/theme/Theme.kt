package com.example.ui.theme

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
    primary = CosmicPrimaryDark,
    onPrimary = CosmicBackgroundDark,
    primaryContainer = CosmicSurfaceVariantDark,
    onPrimaryContainer = CosmicSecondaryDark,
    secondary = CosmicSecondaryDark,
    onSecondary = CosmicBackgroundDark,
    secondaryContainer = CosmicSurfaceVariantDark,
    onSecondaryContainer = CosmicPrimaryDark,
    tertiary = CosmicTertiaryDark,
    onTertiary = CosmicBackgroundDark,
    background = CosmicBackgroundDark,
    onBackground = CosmicBackgroundLight,
    surface = CosmicSurfaceDark,
    onSurface = CosmicBackgroundLight,
    surfaceVariant = CosmicSurfaceVariantDark,
    onSurfaceVariant = CosmicSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicPrimaryLight,
    onPrimary = CosmicSurfaceLight,
    primaryContainer = CosmicSurfaceVariantLight,
    onPrimaryContainer = CosmicPrimaryLight,
    secondary = CosmicSecondaryLight,
    onSecondary = CosmicSurfaceLight,
    secondaryContainer = CosmicSurfaceVariantLight,
    onSecondaryContainer = CosmicPrimaryLight,
    tertiary = CosmicTertiaryLight,
    onTertiary = CosmicSurfaceLight,
    background = CosmicBackgroundLight,
    onBackground = CosmicBackgroundDark,
    surface = CosmicSurfaceLight,
    onSurface = CosmicBackgroundDark,
    surfaceVariant = CosmicSurfaceVariantLight,
    onSurfaceVariant = CosmicPrimaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Standardize to use our spectacular space-blue theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

