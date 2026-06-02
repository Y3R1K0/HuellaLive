package com.huellalive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HuellaColorScheme = darkColorScheme(
    primary              = DustyRose,
    onPrimary            = TextOnAccent,
    primaryContainer     = DustyRoseDark,
    onPrimaryContainer   = TextPrimary,
    secondary            = LavenderSoft,
    onSecondary          = TextOnAccent,
    secondaryContainer   = SurfaceRaised,
    onSecondaryContainer = TextPrimary,
    tertiary             = MintCream,
    onTertiary           = TextOnAccent,
    background           = Background,
    onBackground         = TextPrimary,
    surface              = Surface,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceRaised,
    onSurfaceVariant     = TextSecondary,
    outline              = Outline,
    outlineVariant       = OutlineVariant,
    error                = Error,
    onError              = TextOnAccent,
    inverseSurface       = TextPrimary,
    inverseOnSurface     = Background
)

@Composable
fun HuellaLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HuellaColorScheme,
        typography  = Typography,
        content     = content
    )
}