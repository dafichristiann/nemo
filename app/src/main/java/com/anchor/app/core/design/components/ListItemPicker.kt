package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.R
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * A vertical, scrollable single-choice list picker (companion to the design system's chips).
 *
 * Unlike a wheel, each option is a full list row: tap a row to select it. The selected row is
 * highlighted (ink border + tinted background) and shown with a check mark. The list scrolls
 * when there are more options than fit in [maxHeight]; a plain tap never requires the keyboard.
 *
 * @param options ordered option labels (top → bottom). If labels are precomputed strings, the
 *   default [formatValue] identity works.
 * @param selectedIndex currently selected index into [options].
 * @param onSelect called with the tapped index.
 * @param maxHeight cap for the scroll area so the picker never dominates a form.
 */
@Composable
fun <T> ListItemPicker(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: androidx.compose.ui.unit.Dp = 220.dp,
    formatValue: (T) -> String = { it.toString() },
) {
    if (options.isEmpty()) return

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(RoundedCornerShape(Radius.card))
            .background(AnchorColors.surfaceSunken),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        itemsIndexed(options) { index, option ->
            ListPickerRow(
                label = formatValue(option),
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun ListPickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) AnchorColors.surface else AnchorColors.surfaceSunken,
        animationSpec = Motion.standardSpec(),
        label = "list-picker-bg",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) AnchorColors.textPrimary else AnchorColors.textSecondary,
        animationSpec = Motion.standardSpec(),
        label = "list-picker-fg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(background)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = AnchorColors.ink,
                        shape = RoundedCornerShape(Radius.field),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            color = foreground,
        )
        Spacer(modifier = Modifier.width(Spacing.x3))
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = AnchorColors.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ListItemPickerPreview() {
    AnchorTheme {
        ListItemPicker(
            options = listOf("Not set", "15 min", "30 min", "45 min", "60 min", "90 min", "120 min"),
            selectedIndex = 3,
            onSelect = {},
            modifier = Modifier.padding(Spacing.x4),
        )
    }
}
