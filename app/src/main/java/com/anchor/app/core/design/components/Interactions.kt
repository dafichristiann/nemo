package com.anchor.app.core.design.components

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Motion

/**
 * Shared interaction helpers, so every tappable surface in Nemo gives the same tactile
 * response. Principles from the `better-ui` / `better-accessibility` skills:
 *
 * - A subtle press scale (0.97) for tactile feedback on custom-drawn controls that have no
 *   Material ripple of their own (chips, tabs, cards, list rows).
 * - A minimum 48 dp touch target on icon-only controls (Material HIG), while the visible
 *   glyph can stay small — "the visible element can stay small; the hit area is what must be big".
 */

/** Material minimum touch target (better-accessibility/hit-areas.md: Material = 48dp). */
val MinTouchTarget: Dp = 48.dp

/** Shared default shape so `pressable`'s default arg does not allocate a new Shape per call. */
private val DefaultPressableShape: Shape = RoundedCornerShape(12.dp)

/**
 * Makes a custom (non-Material) surface press-responsive: it scales down slightly while held
 * and returns smoothly, and keeps the standard indication (ripple). Use in place of a bare
 * `Modifier.clickable` on any hand-drawn button/chip/row.
 *
 * @param enabled whether the control responds at all.
 * @param pressedScale scale while pressed; keep >= 0.95 (0.97 is the Nemo default).
 * @param shape used for the ripple clip (matches the visual shape of the element).
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    shape: Shape = DefaultPressableShape,
    pressedScale: Float = 0.97f,
    debounceTimeMs: Long = 400L,
    indication: androidx.compose.foundation.Indication? = null,
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
    var lastClickTime by remember { mutableLongStateOf(0L) }
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = {
                if (debounceTimeMs <= 0L) {
                    onClick()
                } else {
                    val currentTime = SystemClock.uptimeMillis()
                    if (currentTime - lastClickTime >= debounceTimeMs) {
                        lastClickTime = currentTime
                        onClick()
                    }
                }
            },
        )
}

/**
 * Ensures the element occupies at least a 48 dp touch target without changing how it looks.
 * Wrap small icon buttons with this (apply before the clickable).
 */
fun Modifier.minTouchTarget(min: Dp = MinTouchTarget): Modifier =
    sizeIn(minWidth = min, minHeight = min)

/**
 * Clickable modifier that debounces clicks to prevent accidental double-taps or rapid repeated triggers.
 *
 * @param debounceTimeMs minimum time window in milliseconds between allowed clicks (default 400ms).
 * @param enabled whether the click listener is active.
 * @param onClick action to invoke when clicked after debounce window passes.
 */
fun Modifier.debouncedClickable(
    debounceTimeMs: Long = 400L,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    this.clickable(enabled = enabled) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastClickTime >= debounceTimeMs) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

