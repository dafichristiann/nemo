package com.anchor.app.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Radius

/**
 * Thin progress track (docs/superpowers/design/06-design-system.md §7.5).
 *
 * Track `#EEEEEE`, radius 2, height 3–4; fill `#0A0A0A`. The fill width animates smoothly
 * between values (calm motion, §1.6) so progress changes read as a gentle sweep rather
 * than a jump. Percentage label is rendered by the caller, not here.
 *
 * @param progress fraction in 0f..1f (clamped).
 */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    animate: Boolean = true,
    fillColor: Color = AnchorColors.ink,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = Motion.standardSpec(),
        label = "progress-track-fill",
    )
    val fraction = if (animate) animated else clamped

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(Radius.track))
            .background(AnchorColors.track),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(Radius.track))
                    .background(fillColor),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressTrackPreview() {
    AnchorTheme {
        Box(modifier = Modifier.width(311.dp), contentAlignment = Alignment.Center) {
            ProgressTrack(progress = 0.33f)
        }
    }
}
