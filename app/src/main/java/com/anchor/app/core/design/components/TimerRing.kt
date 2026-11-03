package com.anchor.app.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme

/**
 * Ring Timer for Focus Active (design-spec.md §6). A flat monochrome ring: `fog` track +
 * `ink` progress arc, with the large Fraunces digits (`displayLarge`) and a caption inside.
 * Diameter 220, stroke 6. The arc eases over 600ms (the progress duration, spec §8).
 *
 * @param progress fraction 0f..1f of the session elapsed (clamped).
 * @param time text shown large in the center (e.g. "19:22").
 * @param caption small label under the digits, e.g. "remaining" / "paused".
 */
@Composable
fun TimerRing(
    progress: Float,
    time: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    diameter: Dp = 220.dp,
    strokeWidth: Dp = 6.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = Motion.progressSpec(),
        label = "timer-ring-progress",
    )
    val ringInk = Nemo.colors.ink
    val ringTrack = Nemo.colors.fog

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            // Track.
            drawArc(
                color = ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Progress arc (starts at top, clockwise).
            if (animated > 0f) {
                drawArc(
                    color = ringInk,
                    startAngle = -90f,
                    sweepAngle = (360f * animated).coerceIn(0f, 360f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = GeistMonoFamily),
                color = Nemo.colors.ink,
                textAlign = TextAlign.Center,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = Nemo.colors.mute,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TimerRingPreview() {
    NemoTheme {
        TimerRing(progress = 0.65f, time = "19:22", caption = "remaining")
    }
}
