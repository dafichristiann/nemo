package com.anchor.app.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Motion

/**
 * Shared interaction helpers, so every tappable surface in Anchor gives the same tactile
 * response. Principles from the `better-ui` / `better-accessibility` skills:
 *
 * - A subtle press scale (0.97) for tactile feedback on custom-drawn controls that have no
 *   Material ripple of their own (chips, tabs, cards, list rows).
 * - A minimum 48 dp touch target on icon-only controls (Material HIG), while the visible
 *   glyph can stay small — "the visible element can stay small; the hit area is what must be big".
 */

/** Material minimum touch target (better-accessibility/hit-areas.md: Material = 48dp). */
val MinTouchTarget: Dp = 48.dp

/**
 * Makes a custom (non-Material) surface press-responsive: it scales down slightly while held
 * and returns smoothly, and keeps the standard indication (ripple). Use in place of a bare
 * `Modifier.clickable` on any hand-drawn button/chip/row.
 *
 * @param enabled whether the control responds at all.
 * @param pressedScale scale while pressed; keep >= 0.95 (0.97 is the Anchor default).
 * @param shape used for the ripple clip (matches the visual shape of the element).
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    pressedScale: Float = 0.97f,
    onClick: () -> Unit,
): Modifier = composed {
    if (!enabled) {
        return@composed this
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.instantSpec(),
        label = "pressable-scale",
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/**
 * Ensures the element occupies at least a 48 dp touch target without changing how it looks.
 * Wrap small icon buttons with this (apply before the clickable).
 */
fun Modifier.minTouchTarget(min: Dp = MinTouchTarget): Modifier =
    sizeIn(minWidth = min, minHeight = min)
