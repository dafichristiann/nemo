package com.anchor.app.core.design.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens (docs/superpowers/design/06-design-system.md §1.6: "Motion minim.
 * Transisi halus; tidak ada animasi berat").
 *
 * The design system asks for *calm, smooth* transitions rather than none. These tokens give
 * every screen the same rhythm so the app feels deliberate instead of static:
 *
 * - Short, quiet durations — never showy. The "calm" principle means motion should register
 *   subconsciously, not perform. High-frequency taps use [instant] feedback (~120ms);
 *   one-shot entrances use [enter] (~260ms).
 * - A single easing family ([standardEase]) so movement is consistent across the app.
 *
 * Guidance borrowed from the `better-ui` skill: brief and precise beats prominent; motion is
 * never the only feedback channel; durations stay short.
 */
object Motion {

    /** Tap / pressed feedback — quick enough to feel instant. */
    const val instant = 120

    /** List / content reflow (insert, remove, reorder). */
    const val fast = 180

    /** Standard interactive transition (selected state, expand/collapse). */
    const val standard = 220

    /** One-shot entrance of a section or screen. */
    const val enter = 260

    /** Larger surface movement (sheet, overlay). */
    const val large = 320

    /**
     * The shared easing curve. A gentle ease-out (mirrors the mockup's "soft" feel):
     * fast to start, settling smoothly. `FastOutSlowInEasing` is the Material standard,
     * but [CubicBezierEasing] with (0.2, 0, 0, 1) reads slightly calmer; we use it for
     * entrances and the standard intercept.
     */
    val standardEase: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** For entrances that should decelerate into place. */
    val enterEase: Easing = LinearOutSlowInEasing

    /** Convenience spec for the common interactive transition. */
    fun <T> standardSpec() = tween<T>(durationMillis = standard, easing = standardEase)

    fun <T> enterSpec() = tween<T>(durationMillis = enter, easing = enterEase)

    fun <T> fastSpec() = tween<T>(durationMillis = fast, easing = FastOutSlowInEasing)

    fun <T> instantSpec() = tween<T>(durationMillis = instant, easing = standardEase)

    /** Vertical slide distance for subtle enter/exit (per better-ui: ~12px, not full height). */
    val slideOffset = IntOffset(0, 12)
}
