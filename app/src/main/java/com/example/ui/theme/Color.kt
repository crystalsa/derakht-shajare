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

val DarkFamilyTreeColors = FamilyTreeColorScheme(
    primary = Color(0xFF6BBA93),
    primaryContainer = Color(0xFF1C4733),
    background = Color(0xFF121A16),
    surface = Color(0xFF1C2822),
    surfaceVariant = Color(0xFF25352E),
    onSurface = Color(0xFFE8F1EB),
    onSurfaceVariant = Color(0xFFA5B9AE),
    textPrimary = Color(0xFFE8F1EB),
    textSecondary = Color(0xFFA5B9AE),
    success = Color(0xFF81C784),
    warning = Color(0xFFFFB74D),
    danger = Color(0xFFE57373),
    genderMale = Color(0xFF63B3ED),
    genderFemale = Color(0xFFF687B3),
    deceasedTone = Color(0xFF78909C),
    secondSpouseTone = Color(0xFFF6AD55),
    lineEffectColor = Color(0xFF2D4439)
)

val LocalFamilyTreeColors = staticCompositionLocalOf { LightFamilyTreeColors }

object FamilyTreeTheme {
    val colors: FamilyTreeColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalFamilyTreeColors.current
}
