package com.anchor.app.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * NEMO spacing data holder (mirrors docs/superpowers/newdesign/nemo-design-kit/nemo-kit
 * design-spec.md §5). Screens may read this via [Nemo.spacing] or use [Spacing] directly.
 */
data class NemoSpacing(
    val xs: Dp = 4.dp, val sm: Dp = 8.dp, val md: Dp = 12.dp, val lg: Dp = 16.dp,
    val xl: Dp = 24.dp, val xxl: Dp = 32.dp, val xxxl: Dp = 48.dp,
    val screenH: Dp = 24.dp, val section: Dp = 28.dp,
)

val LocalNemoColors = staticCompositionLocalOf { NemoDay }
val LocalNemoSpacing = staticCompositionLocalOf { NemoSpacing() }

/** Access point for NEMO tokens: `Nemo.colors`, `Nemo.spacing`. */
object Nemo {
    val colors: NemoColors @Composable @ReadOnlyComposable get() = LocalNemoColors.current
    val spacing: NemoSpacing @Composable @ReadOnlyComposable get() = LocalNemoSpacing.current
}

/**
 * Root theme. NEMO supports a Day and a Night palette and follows the **system** setting
 * (`design-spec.md` §7 Screens #1 "Ikuti tema sistem"); Night is the recommended theme.
 */
@Composable
fun NemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) NemoNight else NemoDay
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.ink, onPrimary = c.onInk,
            background = c.background, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.fog, onSurfaceVariant = c.inkSecondary,
            outline = c.line, outlineVariant = c.line,
            error = c.danger, onError = c.onInk, scrim = c.scrim,
        )
    } else {
        lightColorScheme(
            primary = c.ink, onPrimary = c.onInk,
            background = c.background, onBackground = c.ink,
            surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.fog, onSurfaceVariant = c.inkSecondary,
            outline = c.line, outlineVariant = c.line,
            error = c.danger, onError = c.onInk, scrim = c.scrim,
        )
    }
    CompositionLocalProvider(
        LocalNemoColors provides c,
        LocalNemoSpacing provides NemoSpacing(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NemoTypography,
            shapes = NemoShapes,
            content = content,
        )
    }
}
