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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorElevation
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.LocalAnchorElevation
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Radius

/**
 * Primary CTA button with the mockup's soft-3D "Base/Front" pattern
 * (docs/superpowers/design/06-design-system.md §7.1).
 *
 * Front: `#0A0A0A`, radius 16, height 56. The darker Base sits behind, offset +6 down,
 * giving a lifted look without images. Label 16/600, white.
 *
 * Polish (2026-10): the whole button scales subtly on press and the Base layer fades out
 * together with the Front when [enabled] is false, so a disabled CTA reads as disabled
 * instead of an enabled black button (the old Base stayed solid and broke the affordance).
 *
 * Named "PrimaryButton" to match the reusable-component list in 07-screens.md §4.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val elevation: AnchorElevation = LocalAnchorElevation.current
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
        // Soft-3D "Base" layer behind the front button. Hidden when disabled so the button
        // does not read as active.
        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 6.dp)
                    .height(56.dp)
                    .shadow(
                        elevation = elevation.button.dp,
                        shape = Radius.buttonShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                    )
                    .background(color = AnchorColors.depthBaseDark, shape = Radius.buttonShape),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Radius.button))
                .background(
                    if (enabled) AnchorColors.ink else AnchorColors.surfaceMuted,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (enabled) AnchorColors.surface else AnchorColors.textFaint,
            )
        }
    }
}

/**
 * Secondary button — an outlined companion to [PrimaryButton] for the cases that need two
 * levels of emphasis (e.g. Focus "Pause" next to the primary "Finish"). Same 56 dp height and
 * radius so the two align; a thin border instead of a fill, and a subtle press scale.
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
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(Radius.button))
            .background(if (enabled) AnchorColors.surface else AnchorColors.surfaceSunken)
            .border(1.dp, AnchorColors.border, RoundedCornerShape(Radius.button))
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
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = AnchorColors.textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    AnchorTheme {
        PrimaryButton(label = "Get started", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonDisabledPreview() {
    AnchorTheme {
        PrimaryButton(label = "Save Task", onClick = {}, enabled = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryButtonPreview() {
    AnchorTheme {
        SecondaryButton(label = "Pause", onClick = {})
    }
}
