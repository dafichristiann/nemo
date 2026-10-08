package com.anchor.app.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * Monochrome color tokens extracted from `gamabaran.pen`
 * (docs/superpowers/design/06-design-system.md §2).
 *
 * Identity: "Black & White Minimalist · 3D Depth System". Only one accent exists —
 * `accentDanger` (#CC3333) — reserved for genuinely critical states. Overdue tasks and
 * drift are NOT shown in red (PRD §10.1, design system §2.3).
 */
object AnchorColors {

    // --- §2.1 Core surfaces ---
    val ink = Color(0xFF0A0A0A)          // primary text / main button / active icon
    val surface = Color(0xFFFFFFFF)      // screen & white card background
    val surfaceSunken = Color(0xFFF8F8F8) // card & input background
    val surfaceCard = Color(0xFFF9F9F9)   // permission card front
    val surfaceMuted = Color(0xFFF5F5F5)  // avatar / track / soft background
    val track = Color(0xFFEEEEEE)         // progress track, hairline divider
    val trackAlt = Color(0xFFEAEAEA)
    val border = Color(0xFFE0E0E0)        // lines, dividers
    val borderSoft = Color(0xFFF0F0F0)    // very soft divider, 3D base
    val borderSoftAlt = Color(0xFFE5E5E5)
    val inkAlt = Color(0xFF262626)        // 3D "depth base" for buttons / dark layers
    val inkDeep = Color(0xFF222222)       // 3D "depth base" for CTA / hero

    // --- §2.2 Text hierarchy ---
    val textPrimary = Color(0xFF0A0A0A)
    val textStrongAlt = Color(0xFF333333)
    val textStrongAlt2 = Color(0xFF444444)
    val textSecondary = Color(0xFF555555)
    val textSecondaryAlt = Color(0xFF666666)
    val textMuted = Color(0xFF777777)
    val textMutedAlt = Color(0xFF888888)
    val textFaintAlt = Color(0xFF999999)
    val textFaint = Color(0xFFAAAAAA)
    val textFaintSoft = Color(0xFFBBBBBB)
    val textFaintSoft2 = Color(0xFFCCCCCC)

    // --- §2.3 Accent (used sparingly) ---
    val accentDanger = Color(0xFFCC3333)
    val accentDangerSoft = Color(0x15CC3333) // #CC333315
    val dangerSurface = Color(0xFFFFF5F5)

    // --- §2.4 Soft-3D depth layers ---
    /** Neutral base used behind light cards (offset +6..8 in the mockup). */
    val depthBaseLight = Color(0xFFE5E5E5)

    /** Dark base used behind dark buttons/CTAs. */
    val depthBaseDark = Color(0xFF222222)
}
