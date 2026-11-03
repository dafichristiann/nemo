package com.anchor.app.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.PillShape
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
                targetValue = if (selected) Nemo.colors.ink else Nemo.colors.fog,
                animationSpec = Motion.standardSpec(),
                label = "chip-bg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) Nemo.colors.onInk else Nemo.colors.inkSecondary,
                animationSpec = Motion.standardSpec(),
                label = "chip-fg",
            )
            Box(
                modifier = Modifier
                    // Share the row width evenly so a longer label ("Urgent"/"Personal Growth")
                    // never wraps or clips on narrow screens; each chip stays a >= 48 dp target.
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(PillShape)
                    .background(background)
                    .then(
                        if (selected) {
                            Modifier
                        } else {
                            Modifier.border(
                                width = 1.dp,
                                color = Nemo.colors.line,
                                shape = PillShape,
                            )
                        },
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = Spacing.x2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GeistMonoFamily),
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedChipsPreview() {
    NemoTheme {
        SegmentedChips(
            options = listOf("Low", "Medium", "High", "Urgent"),
            selectedIndex = 2,
            onSelect = {},
        )
    }
}
