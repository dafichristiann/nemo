package com.anchor.app.core.design.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens (docs/superpowers/design/06-design-system.md §5).
 *
 * Device reference width 375 px, main horizontal padding 32, card content width 311.
 */
object Spacing {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp

    /** Main horizontal screen padding (06-design-system.md §5). */
    val screenHorizontal = 32.dp

    /** Default gap between cards in a list (12–16). */
    val listGap = 12.dp

    /** Default horizontal padding inside a card (20–22). */
    val cardHorizontal = 20.dp
}
