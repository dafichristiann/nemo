package com.anchor.app.core.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo

/**
 * Breathing ring component for contextual intervention:
 * 3 concentric rings pulsing with 4-4-4 rhythm (Breathe in 4s, Hold 4s, Breathe out 4s).
 */
@Composable
fun BreathingRing(
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    showGuideText: Boolean = true,
) {
    val colors = Nemo.colors

    val infiniteTransition = rememberInfiniteTransition(label = "breathing_cycle")

    // 12,000ms Box Breathing cycle: Inhale (0..4000ms), Hold (4000..8000ms), Exhale (8000..12000ms)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 12000
                0.65f at 0 using Motion.standardEase
                1.0f at 4000 using Motion.standardEase
                1.02f at 6000 using Motion.standardEase
                1.0f at 8000 using Motion.standardEase
                0.65f at 12000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathing_scale",
    )

    // Dynamic alpha synchronized with the 12s cycle
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 12000
                0.4f at 0 using Motion.standardEase
                1.0f at 4000 using Motion.standardEase
                1.0f at 8000 using Motion.standardEase
                0.4f at 12000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathing_alpha",
    )

    // Synchronized progress to mathematically tie guide text without coroutine clock drift
    val cycleMillis by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathing_cycle_time",
    )

    // Derived state prevents recomposition of BreathingRing on every animation frame
    val phaseText by remember {
        derivedStateOf {
            when {
                cycleMillis < 4000f -> "Breathe in…"
                cycleMillis < 8000f -> "Hold…"
                else -> "Breathe out…"
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(diameter),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(diameter)) {
                val center = this.center
                val baseRadius = (size.minDimension / 2f) * scale.coerceAtLeast(0f)

                // Ring 1 (outer)
                drawCircle(
                    color = colors.line.copy(alpha = (0.4f * ringAlpha).coerceIn(0f, 1f)),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // Ring 2 (mid)
                drawCircle(
                    color = colors.inkSecondary.copy(alpha = (0.6f * ringAlpha).coerceIn(0f, 1f)),
                    radius = baseRadius * 0.75f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Ring 3 (inner core)
                drawCircle(
                    color = colors.ink.copy(alpha = (0.4f + 0.6f * ringAlpha).coerceIn(0f, 1f)),
                    radius = baseRadius * 0.5f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        if (showGuideText) {
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedContent(
                targetState = phaseText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(Motion.standard, easing = Motion.standardEase)) togetherWith
                        fadeOut(animationSpec = tween(Motion.instant, easing = Motion.standardEase))
                },
                label = "breathing_phase_text",
            ) { targetText ->
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = GeistFamily,
                    color = colors.ink,
                )
            }
        }
    }
}
