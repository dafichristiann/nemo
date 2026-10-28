package com.anchor.app.feature.splash

import android.media.MediaPlayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.app.R
import com.anchor.app.core.design.theme.Nemo
import kotlinx.coroutines.delay

/**
 * Nemo splash screen: displays the branded mascot sequence (Neutral -> Happy with gentle bounce)
 * and plays the short chime, then hands off to [onFinished].
 *
 * Implemented in pure Compose without video player dependency.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colors = Nemo.colors

    var isHappy by remember { mutableStateOf(false) }
    var isBlinking by remember { mutableStateOf(false) }

    // Optional chime audio playback via native MediaPlayer
    DisposableEffect(Unit) {
        val player = try {
            MediaPlayer.create(context, R.raw.nemo_splash)?.apply {
                setOnCompletionListener { mp -> mp.release() }
                start()
            }
        } catch (_: Exception) {
            null
        }

        onDispose {
            try {
                player?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
            } catch (_: Exception) {
                // Ignore disposal errors
            }
        }
    }

    // Sequence timeline:
    // 0ms - 750ms: Neutral
    // 750ms - 900ms: Blink (150ms)
    // 900ms - 1500ms: Neutral
    // 1500ms - 3500ms: Happy with gentle bounce
    // 3500ms: onFinished
    LaunchedEffect(Unit) {
        delay(750L)
        isBlinking = true
        delay(150L)
        isBlinking = false
        delay(600L)
        isHappy = true
        delay(2_000L)
        onFinished()
    }

    // Bounded safety timeout ceiling
    LaunchedEffect(Unit) {
        delay(SPLASH_MAX_MS)
        onFinished()
    }

    // Happy bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_bounce")
    val bounceOffset by if (isHappy) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bounce_y",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Scale effect during transition
    val mascotScale by animateDpAsState(
        targetValue = if (isHappy) 124.dp else 120.dp,
        animationSpec = tween(durationMillis = 200),
        label = "mascot_scale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Mascot container (in dark mode, wrapped in a white circular badge so black ink hair is visible)
            Box(
                modifier = Modifier
                    .offset(y = bounceOffset.dp)
                    .size(mascotScale)
                    .then(
                        if (isDark) {
                            Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(12.dp)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        id = if (isHappy) R.drawable.nemo_mascot_happy else R.drawable.nemo_mascot
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(
                            scaleX = 1f,
                            scaleY = if (isBlinking) 0.15f else 1f,
                        ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = colors.ink,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Personal Focus Assistant",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mute,
            )
        }
    }
}

/** Fallback ceiling (ms) — guarantees the splash never blocks navigation. */
private const val SPLASH_MAX_MS = 4_500L
