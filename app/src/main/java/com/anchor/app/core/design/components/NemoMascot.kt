package com.anchor.app.core.design.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.R
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class MascotExpression {
    NEUTRAL, HAPPY, SAD, ANGRY, SURPRISED, SLEEPY,
    CONFUSED, LAUGHING, SHY, THINKING, WINK, TALKING
}

/**
 * Mascot Nemo component representing the chibi ink head across 12 expressions.
 * In night mode, automatically wraps inside a clean circular white badge so the dark ink hair
 * remains crisp and visible against near-black backgrounds.
 */
@Composable
fun NemoMascot(
    expression: MascotExpression,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    contentDescription: String? = null,
) {
    val isDark = Nemo.colors.background == com.anchor.app.core.design.theme.NemoNight.background

    // Idle blink every 3-5 seconds (150ms squish to simulate eye blink)
    var isBlinking by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000L, 5000L))
            isBlinking = true
            delay(150L)
            isBlinking = false
        }
    }

    // Gentle motion based on expression per specification
    val infiniteTransition = rememberInfiniteTransition(label = "nemo_mascot_motion")

    val bounceOffset by when (expression) {
        MascotExpression.HAPPY, MascotExpression.LAUGHING -> {
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = Motion.standardEase),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bounce_offset",
            )
        }
        else -> remember { mutableStateOf(0f) }
    }

    val tiltAngle by when (expression) {
        MascotExpression.CONFUSED -> {
            infiniteTransition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = Motion.standardEase),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "tilt_angle",
            )
        }
        MascotExpression.THINKING -> {
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = Motion.standardEase),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "tilt_angle",
            )
        }
        else -> remember { mutableStateOf(0f) }
    }

    val drawableRes = when (expression) {
        MascotExpression.HAPPY -> R.drawable.nemo_mascot_happy
        MascotExpression.WINK -> R.drawable.nemo_mascot_shy
        MascotExpression.LAUGHING -> R.drawable.nemo_mascot_laughing
        MascotExpression.SLEEPY -> R.drawable.nemo_mascot_sleepy
        MascotExpression.SURPRISED -> R.drawable.nemo_mascot_eureka
        MascotExpression.CONFUSED -> R.drawable.nemo_mascot_eureka // gunakan eureka atau zen
        MascotExpression.THINKING -> R.drawable.nemo_mascot_thinking
        MascotExpression.ANGRY -> R.drawable.nemo_mascot_angry
        MascotExpression.TALKING -> R.drawable.nemo_mascot_talking
        MascotExpression.SHY -> R.drawable.nemo_mascot_shy
        MascotExpression.SAD -> R.drawable.nemo_mascot_sad
        MascotExpression.NEUTRAL -> R.drawable.nemo_mascot
    }

    val density = LocalDensity.current
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = with(density) { bounceOffset.dp.toPx() }
                rotationZ = tiltAngle
            }
            .then(
                if (isDark) {
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(size * 0.08f)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f
                    scaleY = if (isBlinking) 0.18f else 1f
                },
        )
    }
}
