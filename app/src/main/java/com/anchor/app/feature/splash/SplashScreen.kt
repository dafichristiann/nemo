package com.anchor.app.feature.splash

import android.media.MediaPlayer
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.anchor.app.R
import com.anchor.app.core.design.theme.Nemo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private const val NAME = "Nemo"
private const val SPLASH_MAX_MS = 5_000L

private fun Modifier.focusIn(
    progress: () -> Float,
    maxBlur: Float = 16f,
): Modifier = this.graphicsLayer {
    val p = progress().coerceIn(0f, 1f)
    alpha = p
    val r = (1f - p) * maxBlur
    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && r > 0.5f) {
        BlurEffect(r, r, TileMode.Decal)
    } else null
}

@Composable
fun NemoSplash(
    targetBounds: Rect?,
    isReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    val bgColor = Nemo.colors.background
    val inkColor = Nemo.colors.ink
    val muteColor = Nemo.colors.mute
    val isDark = Nemo.colors.background == com.anchor.app.core.design.theme.NemoNight.background

    val avatarProgress = remember { Animatable(0f) }
    val charProgress = remember { List(NAME.length) { Animatable(0f) } }
    val taglineProgress = remember { Animatable(0f) }
    val handoff = remember { Animatable(0f) }

    var avatarRootBounds by remember { mutableStateOf<Rect?>(null) }

    // Chime audio
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

    // Safety timeout fallback
    LaunchedEffect(Unit) {
        delay(SPLASH_MAX_MS)
        currentOnFinished()
    }

    // Animation timeline
    LaunchedEffect(Unit) {
        avatarProgress.animateTo(1f, tween(durationMillis = 600, easing = Ease))

        charProgress.forEachIndexed { index, anim ->
            launch {
                delay(index * 60L)
                anim.animateTo(1f, tween(durationMillis = 350, easing = Ease))
            }
        }

        delay(NAME.length * 60L + 120L)

        taglineProgress.animateTo(1f, tween(durationMillis = 400, easing = Ease))

        // Relaxed pause: mascot breathes and blinks in NEUTRAL
        delay(1600L)

        snapshotFlow { isReady }.first { it }

        handoff.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))

        currentOnFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .drawBehind {
                drawRect(
                    color = bgColor,
                    alpha = (1f - handoff.value).coerceIn(0f, 1f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val currentBounds = avatarRootBounds
        val hasFlyTarget = targetBounds != null && currentBounds != null

        val avatarModifier = if (hasFlyTarget) {
            val deltaX = targetBounds.center.x - currentBounds.center.x
            val deltaY = targetBounds.center.y - currentBounds.center.y
            val targetScale = (targetBounds.width / currentBounds.width).coerceIn(0.1f, 3f)
            Modifier.graphicsLayer {
                val h = handoff.value
                translationX = deltaX * h
                translationY = deltaY * h
                val s = 1f + (targetScale - 1f) * h
                scaleX = s
                scaleY = s
                alpha = (1f - h * 0.5f).coerceIn(0f, 1f)
            }
        } else {
            Modifier.graphicsLayer {
                alpha = (1f - handoff.value).coerceIn(0f, 1f)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Mascot Avatar (clean, relaxed, without surrounding ring)
            val mascotSize = 136.dp
            Box(
                modifier = avatarModifier
                    .size(mascotSize)
                    .focusIn({ avatarProgress.value })
                    .onGloballyPositioned { coordinates ->
                        avatarRootBounds = coordinates.boundsInRoot()
                    },
                contentAlignment = Alignment.Center,
            ) {
                com.anchor.app.core.design.components.NemoMascot(
                    expression = com.anchor.app.core.design.components.MascotExpression.NEUTRAL,
                    size = mascotSize,
                )
            }

            // App Name & Tagline
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .graphicsLayer {
                        alpha = (1f - handoff.value * 2f).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Name "Nemo"
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NAME.forEachIndexed { index, char ->
                            Text(
                                text = char.toString(),
                                color = inkColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.05.em,
                                modifier = Modifier.focusIn({ charProgress[index].value }),
                            )
                        }
                    }

                    // Tagline
                    Text(
                        text = stringResource(id = R.string.splash_subtitle),
                        color = muteColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.02.em,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .focusIn({ taglineProgress.value }),
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NemoSplash(
        targetBounds = null,
        isReady = true,
        onFinished = onFinished,
        modifier = modifier,
    )
}
