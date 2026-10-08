package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Neutral badge / tag (docs/superpowers/design/06-design-system.md §7.6).
 *
 * Radius small, height ~22, font 11. Neutral gray tone — never a glowing color. A
 * `highlighted` variant is available for "selected"/emphasis states (e.g. current step).
 */
@Composable
fun TextPill(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(Radius.chip))
            .background(
                if (highlighted) AnchorColors.ink else AnchorColors.surfaceMuted,
            )
            .padding(horizontal = Spacing.x2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = if (highlighted) AnchorColors.surface else AnchorColors.textMuted,
        )
    }
}
