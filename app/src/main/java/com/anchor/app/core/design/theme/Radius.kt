package com.anchor.app.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens (docs/superpowers/design/06-design-system.md §4).
 *
 * Actual `.pen` scale runs 2→46. Practical rule:
 * track=2 · chip=10 · field=12 · button=16 · card=18 · sheet=28 · screen=32.
 */
object Radius {
    val track = 2.dp          // progress track/bar
    val hairline = 6.dp       // thick divider, mini bar
    val chip = 10.dp          // small badge, priority bar, dot
    val field = 12.dp         // input field, small card
    val button = 16.dp        // primary button (most used in mockup)
    val card = 18.dp          // card, panel
    val cardAlt = 20.dp       // large card
    val panel = 24.dp         // panel / sheet
    val sheet = 28.dp         // bottom sheet top
    val screen = 32.dp        // device frame corners
    val lg = 46.dp            // large decorative element (hero CTA)

    // Prebuilt shapes for the common cases.
    val trackShape = RoundedCornerShape(track)
    val chipShape = RoundedCornerShape(chip)
    val fieldShape = RoundedCornerShape(field)
    val buttonShape = RoundedCornerShape(button)
    val cardShape = RoundedCornerShape(card)
    val panelShape = RoundedCornerShape(panel)
    val sheetShape = RoundedCornerShape(sheet)
}
