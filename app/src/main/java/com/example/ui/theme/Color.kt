package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FamilyTreeColorScheme(
    val primary: Color,
    val primaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val genderMale: Color,
    val genderFemale: Color,
    val deceasedTone: Color,
    val secondSpouseTone: Color,
    val lineEffectColor: Color
)

val LightFamilyTreeColors = FamilyTreeColorScheme(
    primary = Color(0xFF2C6B4F),
    primaryContainer = Color(0xFFD3EFE0),
    background = Color(0xFFF7FBF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8F1EB),
    onSurface = Color(0xFF14201A),
    onSurfaceVariant = Color(0xFF405249),
    textPrimary = Color(0xFF14201A),
    textSecondary = Color(0xFF4A6557),
    success = Color(0xFF2E7D32),
    warning = Color(0xFFEF6C00),
    danger = Color(0xFFC62828),
    genderMale = Color(0xFF2B6CB0),
    genderFemale = Color(0xFFD53F8C),
    deceasedTone = Color(0xFF5A6E64),
    secondSpouseTone = Color(0xFFD97706),
    lineEffectColor = Color(0xFFCBE3D8)
)

val LocalFamilyTreeColors = staticCompositionLocalOf { LightFamilyTreeColors }

object FamilyTreeTheme {
    val colors: FamilyTreeColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalFamilyTreeColors.current
}
