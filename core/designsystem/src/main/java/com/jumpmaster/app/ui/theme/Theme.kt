package com.jumpmaster.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = JumpMasterVitalityPurple,
        onPrimary = Color.White,
        primaryContainer = JumpMasterVitalityPurple.copy(alpha = 0.22f),
        onPrimaryContainer = Color(0xFF1A1040),
        secondary = JumpMasterFluorescentGreen,
        onSecondary = Color(0xFF002114),
        secondaryContainer = JumpMasterFluorescentGreen.copy(alpha = 0.22f),
        onSecondaryContainer = Color(0xFF002114),
        tertiary = JumpMasterFluorescentGreen,
        onTertiary = Color(0xFF002114),
        background = JumpMasterSurfaceTint,
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = JumpMasterSurfaceContainerTinted,
        onSurfaceVariant = Color(0xFF46464F),
        surfaceContainerLowest = JumpMasterSurfaceTint,
        surfaceContainerLow = Color(0xFFF7F4FA),
        surfaceContainer = JumpMasterSurfaceContainerTinted,
        surfaceContainerHigh = Color(0xFFE8E3F4),
        surfaceContainerHighest = Color(0xFFDCD6EC),
        outline = JumpMasterVitalityPurple.copy(alpha = 0.28f),
        outlineVariant = Color(0xFFC8C5D0),
    )

private val DarkColors =
    darkColorScheme(
        primary = JumpMasterVitalityPurple,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF3D2B6E),
        onPrimaryContainer = Color(0xFFE8DEFF),
        secondary = JumpMasterFluorescentGreen,
        onSecondary = Color(0xFF002114),
        background = Color(0xFF121018),
        onBackground = Color(0xFFE6E1F0),
        surface = Color(0xFF1B1824),
        onSurface = Color(0xFFE6E1F0),
        surfaceContainer = Color(0xFF262232),
        surfaceContainerHigh = Color(0xFF302C3C),
        errorContainer = Color(0xFF5C1A28),
        onErrorContainer = Color(0xFFFFDAD8),
    )

@Composable
fun JumpMasterTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = JumpMasterShapes,
        content = content,
    )
}
