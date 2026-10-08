package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.AnchorColors

/**
 * Hairline divider (docs/superpowers/design/06-design-system.md §7.15 / §6.1).
 * Default color `border #E0E0E0`, 1 dp thick.
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: androidx.compose.ui.graphics.Color = AnchorColors.border,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}
