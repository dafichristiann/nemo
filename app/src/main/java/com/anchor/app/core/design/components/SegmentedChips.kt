package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Single-select chip row (docs/superpowers/design/06-design-system.md §7.7).
 *
 * Selected chip = `#0A0A0A` + white text; others = soft gray. Used for Task priority
 * and Focus duration selectors.
 */
@Composable
fun SegmentedChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .padding(end = Spacing.x2)
                    .height(38.dp)
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(
                        if (selected) AnchorColors.ink else AnchorColors.surfaceMuted,
                    )
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
                    .clickable { onSelect(index) }
                    .padding(horizontal = Spacing.x4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (selected) AnchorColors.surface else AnchorColors.textSecondary,
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
