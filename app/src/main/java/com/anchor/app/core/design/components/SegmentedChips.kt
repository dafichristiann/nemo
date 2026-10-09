package com.anchor.app.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Single-select chip row (docs/superpowers/design/06-design-system.md §7.7).
 *
 * Selected chip = `#0A0A0A` + white text; others = soft gray. Used for Task priority and
 * Focus duration selectors.
 *
 * Polish: the selected background/border/label colors cross-fade ([animateColorAsState]),
 * and each chip keeps a >= 48 dp touch target (`heightIn(min = 48.dp)`) so it is comfortably
 * tappable. A soft press ripple is preserved for feedback.
 */
@Composable
fun SegmentedChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) AnchorColors.ink else AnchorColors.surfaceMuted,
                animationSpec = Motion.standardSpec(),
                label = "chip-bg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) AnchorColors.surface else AnchorColors.textSecondary,
                animationSpec = Motion.standardSpec(),
                label = "chip-fg",
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(background)
                    .then(
                        if (selected) {
                            Modifier
                        } else {
                            Modifier.border(
                                width = 1.dp,
                                color = AnchorColors.border,
                                shape = RoundedCornerShape(Radius.chip),
                            )
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = Spacing.x4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = contentColor,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedChipsPreview() {
    AnchorTheme {
        SegmentedChips(
            options = listOf("Low", "Medium", "High", "Urgent"),
            selectedIndex = 2,
            onSelect = {},
        )
    }
}
