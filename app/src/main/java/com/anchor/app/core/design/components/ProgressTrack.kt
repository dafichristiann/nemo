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
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Radius

/**
 * Thin progress track (design-spec.md §6): track `fog`, height 3, radius 3; fill `ink`.
 * The fill animates over 600ms (the progress duration, §8) so changes read as a gentle
 * sweep. The percentage label is rendered by the caller, not here.
 *
 * @param progress fraction in 0f..1f (clamped).
 */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 3.dp,
    animate: Boolean = true,
    fillColor: Color = Color.Unspecified,
) {
    val resolvedFill = if (fillColor == Color.Unspecified) Nemo.colors.ink else fillColor
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = Motion.progressSpec(),
        label = "progress-track-fill",
    )
    val fraction = if (animate) animated else clamped

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(Radius.track))
            .background(Nemo.colors.fog),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(Radius.track))
                    .background(resolvedFill),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressTrackPreview() {
    NemoTheme {
        Box(modifier = Modifier.width(311.dp), contentAlignment = Alignment.Center) {
            ProgressTrack(progress = 0.33f)
        }
    }
}
