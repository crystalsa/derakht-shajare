package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = LightFamilyTreeColors.primary,
    onPrimary = LightFamilyTreeColors.surface,
    primaryContainer = LightFamilyTreeColors.primaryContainer,
    background = LightFamilyTreeColors.background,
    onBackground = LightFamilyTreeColors.onSurface,
    surface = LightFamilyTreeColors.surface,
    onSurface = LightFamilyTreeColors.onSurface,
    surfaceVariant = LightFamilyTreeColors.surfaceVariant,
    onSurfaceVariant = LightFamilyTreeColors.onSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFamilyTreeColors.primary,
    onPrimary = DarkFamilyTreeColors.surface,
    primaryContainer = DarkFamilyTreeColors.primaryContainer,
    background = DarkFamilyTreeColors.background,
    onBackground = DarkFamilyTreeColors.onSurface,
    surface = DarkFamilyTreeColors.surface,
    onSurface = DarkFamilyTreeColors.onSurface,
    surfaceVariant = DarkFamilyTreeColors.surfaceVariant,
    onSurfaceVariant = DarkFamilyTreeColors.onSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    val familyColors = if (darkTheme) DarkFamilyTreeColors else LightFamilyTreeColors

    CompositionLocalProvider(
        LocalFamilyTreeColors provides familyColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
