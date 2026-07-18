package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
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

@Composable
fun MyApplicationTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    val familyColors = LightFamilyTreeColors

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
