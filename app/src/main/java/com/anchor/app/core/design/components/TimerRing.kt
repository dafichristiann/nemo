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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme

/**
 * Ring Timer for Focus Active (docs/superpowers/design/06-design-system.md §7.13).
 *
 * A thick monochrome ring: gray track + `#0A0A0A` progress arc, with the large time digits
 * and a sub-label centered inside. A very soft outer glow is drawn behind the ring. The arc
 * animates smoothly as [progress] changes (calm motion, §1.6).
 *
 * @param progress fraction 0f..1f of the session elapsed (clamped).
 * @param time seconds text shown large in the center (e.g. "19:22").
 * @param subLabel optional caption under the digits.
 * @param diameter overall ring size.
 */
@Composable
fun TimerRing(
    progress: Float,
    time: String,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    diameter: Dp = 232.dp,
    strokeWidth: Dp = 10.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = Motion.standardSpec(),
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

            // Soft outer glow ring (very low alpha — §7.13 "glow sangat halus"). Kept only
            // slightly larger than the stroke so it reads as a faint halo, not a heavy band.
            drawArc(
                color = ringInk.copy(alpha = 0.035f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke * 1.9f, cap = StrokeCap.Round),
            )

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

            // Progress arc (starts at top, clockwise). A tiny floor keeps the rounded cap
            // visible at the very start so the arc never disappears entirely.
            if (animated > 0f) {
                drawArc(
                    color = ringInk,
                    startAngle = -90f,
                    sweepAngle = 360f * animated.coerceAtLeast(0.02f),
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
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Light,
                fontSize = 48.sp,
                color = Nemo.colors.ink,
                textAlign = TextAlign.Center,
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
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
        TimerRing(progress = 0.65f, time = "19:22", subLabel = "remaining")
    }
}
