package com.anchor.app.core.design.theme

import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens, retuned for NEMO
 * (docs/superpowers/newdesign/nemo-design-kit/nemo-kit/docs/design-spec.md §5).
 *
 * Practical scale: track=3 · pill=999 (use [PillShape]) · input=18 · card=24 · dialog=30 ·
 * sheet=32. Kept as an object with the same member names the screens already reference so
 * the migration does not churn every call site.
 */
object Radius {
    val track = 3.dp          // progress track/bar
    val field = 18.dp         // input field
    val card = 24.dp          // card, panel
    val panel = 30.dp         // panel / dialog
    val sheet = 32.dp         // bottom sheet top

    // Prebuilt shapes for the common cases.
    val trackShape = androidx.compose.foundation.shape.RoundedCornerShape(track)
    val fieldShape = androidx.compose.foundation.shape.RoundedCornerShape(field)
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(card)
    val panelShape = androidx.compose.foundation.shape.RoundedCornerShape(panel)
    val sheetShape = SheetShape
}
