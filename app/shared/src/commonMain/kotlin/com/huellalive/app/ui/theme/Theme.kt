package com.huellalive.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HuellaColorScheme = darkColorScheme(
    primary              = DustyRose,
    onPrimary            = TextOnAccent,
    primaryContainer     = DustyRoseDark,
    onPrimaryContainer   = TextPrimary,
    secondary            = MintCream,
    onSecondary          = TextOnAccent,
    secondaryContainer   = StatusAvailableBg,
    onSecondaryContainer = TextPrimary,
    tertiary             = PeachWarm,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HuellaLiveTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = HuellaColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = HuellaShapes,
        content = content
    )
}
