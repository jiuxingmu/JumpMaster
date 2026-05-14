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
        primaryContainer = Color(0xFFC9EDE5),
        onPrimaryContainer = Color(0xFF0F3D35),
        secondary = JumpMasterIndigoAccent,
        onSecondary = Color.White,
        secondaryContainer = JumpMasterIndigoContainer,
        onSecondaryContainer = JumpMasterOnIndigoContainer,
        tertiary = JumpMasterIndigoMuted,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD9ECF5),
        onTertiaryContainer = Color(0xFF2A4558),
        background = JumpMasterSurfaceTint,
        onBackground = Color(0xFF2A3331),
        surface = Color(0xFFFFFEFE),
        onSurface = Color(0xFF2A3331),
        surfaceVariant = JumpMasterSurfaceContainerTinted,
        onSurfaceVariant = Color(0xFF5A6D6A),
        surfaceContainerLowest = JumpMasterSurfaceTint,
        surfaceContainerLow = Color(0xFFEDF7F4),
        surfaceContainer = JumpMasterSurfaceContainerTinted,
        surfaceContainerHigh = Color(0xFFD3E8E2),
        surfaceContainerHighest = Color(0xFFC2DED6),
        outline = JumpMasterVitalityPurple.copy(alpha = 0.22f),
        outlineVariant = Color(0xFFC5D5D0),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8FD4C8),
        onPrimary = Color(0xFF00332C),
        primaryContainer = Color(0xFF1F4A43),
        onPrimaryContainer = Color(0xFFBCECE2),
        secondary = Color(0xFF9EC9EB),
        onSecondary = Color(0xFF0F2840),
        secondaryContainer = Color(0xFF2A4A63),
        onSecondaryContainer = Color(0xFFD9EDFA),
        tertiary = Color(0xFFA8C4D6),
        onTertiary = Color(0xFF0F2028),
        tertiaryContainer = Color(0xFF2F4555),
        onTertiaryContainer = Color(0xFFD5E6F0),
        background = Color(0xFF0F1615),
        onBackground = Color(0xFFE2EEEB),
        surface = Color(0xFF141C1B),
        onSurface = Color(0xFFE2EEEB),
        surfaceContainer = Color(0xFF1B2624),
        surfaceContainerHigh = Color(0xFF24302E),
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
