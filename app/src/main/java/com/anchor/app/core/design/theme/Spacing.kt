package com.anchor.app.core.design.theme

import androidx.compose.ui.unit.dp

/**
 * NEMO spacing tokens (docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md §5).
 *
 * Scale: 4 · 8 · 12 · 16 · 24 · 32 · 48. Horizontal screen margin 24. Gap between
 * sections 28.
 */
object Spacing {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x12 = 48.dp

    /** Horizontal screen margin (design-spec.md §5: margin horizontal 24). */
    val screenHorizontal = 24.dp

    /** Gap between sections (design-spec.md §5: jarak antar bagian 28). */
    val section = 28.dp

    /** Default horizontal padding inside a card. */
    val cardHorizontal = 20.dp

    /** Default gap between list items. */
    val listGap = 12.dp
}
