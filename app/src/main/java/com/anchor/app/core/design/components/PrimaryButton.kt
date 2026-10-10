package com.anchor.app.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.PillShape

/**
 * Primary CTA button in the NEMO style: a static, flat, fully-rounded pill.
 *
 * Fill `ink`, label `onInk`, height 54, pill radius. No soft-3D base, no shadow — NEMO is flat.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = Motion.instantSpec(),
        label = "primary-button-scale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(PillShape)
                .background(
                    if (enabled) Nemo.colors.ink else Nemo.colors.fog,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (enabled) Nemo.colors.onInk else Nemo.colors.mute,
            )
        }
    }
}

/**
 * Secondary button — an outlined companion to [PrimaryButton]. Same 54 dp height and pill
 * shape so the two align; a thin border instead of a fill, and a subtle press scale.
 */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = Motion.instantSpec(),
        label = "secondary-button-scale",
    )
    Box(
        modifier = modifier
            .height(54.dp)
            .scale(scale)
            .clip(PillShape)
            .background(if (enabled) Nemo.colors.surface else Nemo.colors.fog)
            .border(1.dp, Nemo.colors.line, PillShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Nemo.colors.ink,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    NemoTheme {
        PrimaryButton(label = "Get started", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonDisabledPreview() {
    NemoTheme {
        PrimaryButton(label = "Save Task", onClick = {}, enabled = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryButtonPreview() {
    NemoTheme {
        SecondaryButton(label = "Pause", onClick = {})
    }
}
