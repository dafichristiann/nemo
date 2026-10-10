package com.anchor.app.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * NEMO color tokens — pure neutral: black, white, thin grey. No accent color.
 *
 * Source of truth: docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md §3.
 * Priority and danger are expressed through bar length, font weight and labels — never
 * through a bright color. Overdue tasks and drift are NOT red.
 */
@Immutable
data class NemoColors(
    val background: Color,   // screen background
    val surface: Color,      // sheet, dialog, soft card
    val fog: Color,          // input, track, secondary button, neutral pill
    val line: Color,         // divider, chip border
    val mute: Color,         // secondary text, meta, inactive icon
    val placeholder: Color,
    val inkSecondary: Color, // secondary body text
    val ink: Color,          // primary text, primary button, completed node, FAB
    val onInk: Color,        // text/icon on top of ink
    val danger: Color,       // reserved: "Delete Task", "Delete all data"
    val dangerLine: Color,
    val scrim: Color,
    val scrimAlpha: Float,
)

/** Day palette (system light). */
val NemoDay = NemoColors(
    background = Color(0xFFFFFFFF), surface = Color(0xFFFAFAFA), fog = Color(0xFFF5F5F5),
    line = Color(0xFFE9E9E9), mute = Color(0xFF767676), placeholder = Color(0xFF8C8C8C),
    inkSecondary = Color(0xFF444444), ink = Color(0xFF111111), onInk = Color(0xFFFFFFFF),
    danger = Color(0xFF8A3B3B), dangerLine = Color(0xFFEAD9D9),
    scrim = Color(0xFF000000), scrimAlpha = 0.42f,
)

/** Night palette (system dark). This is the recommended theme. */
val NemoNight = NemoColors(
    background = Color(0xFF0A0A0A), surface = Color(0xFF141414), fog = Color(0xFF1B1B1B),
    line = Color(0xFF262626), mute = Color(0xFF8A8A8A), placeholder = Color(0xFF6B6B6B),
    inkSecondary = Color(0xFFCFCFCF), ink = Color(0xFFF2F2F2), onInk = Color(0xFF0A0A0A),
    danger = Color(0xFFD9A0A0), dangerLine = Color(0xFF3A2A2A),
    scrim = Color(0xFF000000), scrimAlpha = 0.67f,
)
