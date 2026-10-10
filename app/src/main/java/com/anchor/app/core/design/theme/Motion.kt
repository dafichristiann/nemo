package com.anchor.app.core.design.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * NEMO motion tokens (docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md §8).
 *
 * Durations: press 150 · transition 250 · sheet 400 · progress 600.
 * Easing `cubic-bezier(0.22, 1, 0.36, 1)`. No bounce, no shake, no flicker.
 *
 * Honor system "Remove animations": callers should fall back to a short fade (see
 * [instant]).
 */
object Motion {

    /** Press / tap feedback. */
    const val instant = 150

    /** Standard interactive transition. */
    const val standard = 250

    /** Bottom sheet / large surface movement. */
    const val large = 400

    /** Progress track / ring animation. */
    const val progress = 600

    /** The shared NEMO easing curve. */
    val standardEase: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Alias kept so existing call sites (`enterSpec`, `enterEase`) still compile. */
    val enterEase: Easing = standardEase

    // --- Legacy-compatible helpers (same names the screens already use) ---
    const val fast = 250
    const val enter = 250

    fun <T> standardSpec() = tween<T>(durationMillis = standard, easing = standardEase)
    fun <T> enterSpec() = tween<T>(durationMillis = standard, easing = standardEase)
    fun <T> instantSpec() = tween<T>(durationMillis = instant, easing = standardEase)
}
