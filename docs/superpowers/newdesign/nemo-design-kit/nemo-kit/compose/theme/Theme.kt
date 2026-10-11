package com.anchor.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NemoSpacing(
    val xs: Dp = 4.dp, val sm: Dp = 8.dp, val md: Dp = 12.dp, val lg: Dp = 16.dp,
    val xl: Dp = 24.dp, val xxl: Dp = 32.dp, val xxxl: Dp = 48.dp,
    val screenH: Dp = 24.dp, val section: Dp = 28.dp,
)

/** Gerak lembut, tanpa bounce. */
object NemoMotion {
    val Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    const val Press = 150; const val Standard = 250; const val Sheet = 400; const val Progress = 600
}

val LocalNemoColors = staticCompositionLocalOf { NemoDay }
val LocalNemoSpacing = staticCompositionLocalOf { NemoSpacing() }

object Nemo {
    val colors: NemoColors @Composable @ReadOnlyComposable get() = LocalNemoColors.current
    val spacing: NemoSpacing @Composable @ReadOnlyComposable get() = LocalNemoSpacing.current
}

@Composable
fun NemoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) NemoNight else NemoDay
    val scheme = if (darkTheme) {
        darkColorScheme(primary = c.ink, onPrimary = c.onInk, background = c.background, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.fog, onSurfaceVariant = c.mute,
            outline = c.line, outlineVariant = c.line, error = c.danger, scrim = c.scrim)
    } else {
        lightColorScheme(primary = c.ink, onPrimary = c.onInk, background = c.background, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink, surfaceVariant = c.fog, onSurfaceVariant = c.mute,
            outline = c.line, outlineVariant = c.line, error = c.danger, scrim = c.scrim)
    }
    CompositionLocalProvider(LocalNemoColors provides c, LocalNemoSpacing provides NemoSpacing()) {
        MaterialTheme(colorScheme = scheme, typography = NemoTypography, shapes = NemoShapes, content = content)
    }
}
