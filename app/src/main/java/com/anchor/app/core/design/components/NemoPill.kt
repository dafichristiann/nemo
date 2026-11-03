package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Spacing

/**
 * Neutral badge / tag in the NEMO style (design-spec.md §6): a compact, fully-rounded pill.
 *
 * Neutral = `fog` with `inkSecondary` text; filled (selected/emphasis) = `ink` with `onInk`
 * text. Label uses `labelSmall` (Manrope 600, 10.5sp, tracking). Height is a minimum, not a
 * fixed size, so the pill grows with larger accessibility font scales (§9).
 *
 * @param filled true for the emphasis (ink) variant; false for the neutral (fog) variant.
 */
@Composable
fun NemoPill(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 22.dp)
            .clip(PillShape)
            .background(
                if (filled) Nemo.colors.ink else Nemo.colors.fog,
            )
            .padding(horizontal = Spacing.x2 + Spacing.x1, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = GeistMonoFamily),
            color = if (filled) Nemo.colors.onInk else Nemo.colors.inkSecondary,
        )
    }
}
