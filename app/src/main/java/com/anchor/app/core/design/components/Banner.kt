package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Calm, non-blocking banner (docs/superpowers/design/06-design-system.md §7.12).
 *
 * Used for the overdue notice on Home. Tone is gentle — deliberately NOT red — and it
 * carries a secondary action. Content is passed via slots so callers control copy and
 * keep all text in string resources.
 */
@Composable
fun Banner(
    modifier: Modifier = Modifier,
    background: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    val resolvedBackground = if (background == Color.Unspecified) Nemo.colors.fog else background
    val resolvedBorder = if (borderColor == Color.Unspecified) Nemo.colors.line else borderColor
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(resolvedBackground)
            .border(1.dp, resolvedBorder, RoundedCornerShape(Radius.card))
            .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x4),
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun BannerPreview() {
    NemoTheme {
        Banner(
            modifier = Modifier.padding(Spacing.x4),
        ) {
            androidx.compose.material3.Text(text = "3 tasks from yesterday are still waiting.")
        }
    }
}
