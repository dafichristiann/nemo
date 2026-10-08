package com.anchor.app.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Anchor is **light-first, monochrome** (docs 06-design-system.md §1, §2).
 *
 * The token→Material mapping follows 06-design-system.md §10, e.g.
 * `ink #0A0A0A → onSurface`, `surface #FFFFFF → background/surface`,
 * `surface-sunken #F8F8F8 → surfaceVariant`.
 */
private val AnchorLightColorScheme = lightColorScheme(
    primary = AnchorColors.ink,
    onPrimary = AnchorColors.surface,
    primaryContainer = AnchorColors.inkAlt,
    onPrimaryContainer = AnchorColors.surface,

    secondary = AnchorColors.textSecondary,
    onSecondary = AnchorColors.surface,

    background = AnchorColors.surface,
    onBackground = AnchorColors.textPrimary,
    surface = AnchorColors.surface,
    onSurface = AnchorColors.ink,
    surfaceVariant = AnchorColors.surfaceSunken,
    onSurfaceVariant = AnchorColors.textSecondaryAlt,

    outline = AnchorColors.border,
    outlineVariant = AnchorColors.borderSoft,

    error = AnchorColors.accentDanger,
    onError = AnchorColors.surface,
    errorContainer = AnchorColors.dangerSurface,
    onErrorContainer = AnchorColors.accentDanger,
)

/**
 * A dark scheme is not part of the MVP design (monochrome, light-first), but we keep a
 * sensible fallback so the app never renders unstyled if the system is in dark mode.
 */
private val AnchorDarkColorScheme = darkColorScheme(
    primary = AnchorColors.surface,
    onPrimary = AnchorColors.ink,
    background = AnchorColors.ink,
    onBackground = AnchorColors.surface,
    surface = AnchorColors.ink,
    onSurface = AnchorColors.surface,
    surfaceVariant = AnchorColors.inkAlt,
    onSurfaceVariant = AnchorColors.textFaintSoft2,
    outline = AnchorColors.textStrongAlt,
    error = AnchorColors.accentDanger,
    onError = AnchorColors.surface,
)

/** Soft-3D shadow tokens (docs 06-design-system.md §6). */
data class AnchorElevation(
    val card: Int = 6,
    val cardRaised: Int = 8,
    val button: Int = 10,
    val screen: Int = 20,
    val avatar: Int = 16,
)

val LocalAnchorElevation = staticCompositionLocalOf { AnchorElevation() }

/**
 * Root theme. Defaults to the light monochrome scheme regardless of system setting
 * (light-first), but can be forced dark if ever needed.
 */
@Composable
fun AnchorTheme(
    darkTheme: Boolean = false && isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AnchorDarkColorScheme else AnchorLightColorScheme
    CompositionLocalProvider(LocalAnchorElevation provides AnchorElevation()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AnchorTypography,
            content = content,
        )
    }
}
