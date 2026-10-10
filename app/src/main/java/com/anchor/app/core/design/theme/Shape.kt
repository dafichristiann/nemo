package com.anchor.app.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * NEMO corner shapes (docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md §5).
 *
 * Buttons, chips and pills are fully rounded (999 → [CircleShape] at the call site).
 * input 18 · card 24 · dialog 30 · sheet top 32.
 */
val NemoShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),   // input
    medium = RoundedCornerShape(24.dp),  // card
    large = RoundedCornerShape(30.dp),   // dialog
    extraLarge = RoundedCornerShape(32.dp),
)

/** Bottom-sheet top corners (design-spec.md §5: sheet atas 32). */
val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/** Fully rounded pill (999): buttons, chips, pills. */
val PillShape = RoundedCornerShape(percent = 50)
