package com.anchor.app.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.PillShape

/**
 * NEMO button (design-spec.md §6): flat, fully-rounded pill, no elevation.
 *
 * Variants: [NemoButtonStyle.Primary], Secondary, Outline, Danger, Text. One Primary per
 * screen. Height 54 (small 34); label uses `labelLarge` (`labelMedium` when small). When
 * disabled the whole button is drawn at 35% opacity (§6).
 */
enum class NemoButtonStyle { Primary, Secondary, Outline, Danger, Text }

@Composable
fun NemoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NemoButtonStyle = NemoButtonStyle.Primary,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val c = Nemo.colors
    val container: Color
    val content: Color
    var border: Color? = null
    when (style) {
        NemoButtonStyle.Primary -> { container = c.ink; content = c.onInk }
        NemoButtonStyle.Secondary -> { container = c.fog; content = c.ink }
        NemoButtonStyle.Outline -> { container = Color.Transparent; content = c.ink; border = c.line }
        NemoButtonStyle.Danger -> { container = Color.Transparent; content = c.danger; border = c.dangerLine }
        NemoButtonStyle.Text -> { container = Color.Transparent; content = c.mute }
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        modifier = modifier
            .height(if (small) 34.dp else 54.dp)
            .alpha(if (enabled) 1f else 0.35f),
        border = border?.let { BorderStroke(1.dp, it) },
        contentPadding = PaddingValues(horizontal = if (small) 16.dp else 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        elevation = null,
    ) {
        Text(
            text = text,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Primary CTA button — the default NEMO call to action. Kept as a named alias of
 * [NemoButton] with [NemoButtonStyle.Primary] so existing screens keep working.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    NemoButton(
        text = label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = NemoButtonStyle.Primary,
        enabled = enabled,
    )
}

/**
 * Secondary button — the outlined/low-emphasis companion to [PrimaryButton] (e.g. Focus
 * "Pause"). Same 54dp height and pill shape so the two align.
 */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    NemoButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        style = NemoButtonStyle.Outline,
        enabled = enabled,
    )
}

@Preview(showBackground = true)
@Composable
private fun NemoButtonPreview() {
    NemoTheme {
        Box(modifier = Modifier.background(Nemo.colors.background)) {
            androidx.compose.foundation.layout.Column {
                PrimaryButton(label = "Get started", onClick = {})
                NemoButton(text = "Secondary", onClick = {}, style = NemoButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                NemoButton(text = "Outline", onClick = {}, style = NemoButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                NemoButton(text = "Delete Task", onClick = {}, style = NemoButtonStyle.Danger, modifier = Modifier.fillMaxWidth())
                NemoButton(text = "Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
                NemoButton(text = "Small", onClick = {}, style = NemoButtonStyle.Outline, small = true)
            }
        }
    }
}
