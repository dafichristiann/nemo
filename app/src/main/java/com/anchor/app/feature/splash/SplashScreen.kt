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
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.theme.Nemo
import kotlin.math.hypot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
// Buttery Smooth Iris Easing (smooth acceleration, buttery soft deceleration)
private val ButterySmoothIrisEase = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)
private const val NAME = "Nemo"
private const val SPLASH_MAX_MS = 6_000L

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
    var mascotExpression by remember { mutableStateOf(MascotExpression.NEUTRAL) }
    val irisProgress = remember { Animatable(0f) }
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
        // Entrance choreography: avatar & letters stagger in (0 - 800ms)
        launch {
            avatarProgress.animateTo(1f, tween(durationMillis = 600, easing = Ease))
        }

        charProgress.forEachIndexed { index, anim ->
            launch {
                delay(index * 60L)
                anim.animateTo(1f, tween(durationMillis = 350, easing = Ease))
            }
        }

        delay(NAME.length * 60L + 120L)
        taglineProgress.animateTo(1f, tween(durationMillis = 400, easing = Ease))

        // Mascot breathes and blinks naturally through its internal spec without conflicting state swaps
        delay(2200L) // Completes exactly 3.0s total initial showcase

        snapshotFlow { isReady }.first { it }

        // BUTTERY SMOOTH IRIS EXPANSION (750ms)
        // Lingkaran membesar memenuhi layar dengan kurva CubicBezier(0.2, 0.9, 0.1, 1)
        coroutineScope {
            launch {
                irisProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 750, easing = ButterySmoothIrisEase),
                )
            }
            if (targetBounds != null) {
                launch {
                    handoff.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 750, easing = ButterySmoothIrisEase),
                    )
                }
            }
        }

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
                // 1. Base backdrop before expansion (charcoal slate in Dark Mode, soft pewter in Day Mode)
                val baseBackdrop = if (isDark) Color(0xFF161920) else Color(0xFFE8EBEF)
                drawRect(color = baseBackdrop)

                // 2. Exact center of the mascot
                val circleCenter = avatarRootBounds?.center ?: center
                val p = irisProgress.value
                val baseRadius = 76.dp.toPx()

                // Calculate distance to all 4 screen corners to guarantee full-screen coverage
                val dTopLeft = hypot(circleCenter.x, circleCenter.y)
                val dTopRight = hypot(size.width - circleCenter.x, circleCenter.y)
                val dBottomLeft = hypot(circleCenter.x, size.height - circleCenter.y)
                val dBottomRight = hypot(size.width - circleCenter.x, size.height - circleCenter.y)
                val maxRadius = maxOf(dTopLeft, dTopRight, dBottomLeft, dBottomRight) + 24.dp.toPx()

                val currentRadius = baseRadius + (maxRadius - baseRadius) * p

                // 3. Buttery Smooth Expanding Circle (Iris) centered precisely on the mascot
                drawCircle(
                    color = bgColor,
                    radius = currentRadius,
                    center = circleCenter,
                )

                // 4. Subtle defining rim around the initial circle (fades away as expansion begins)
                if (p < 0.25f) {
                    val rimAlpha = ((1f - p * 4f) * (if (isDark) 0.22f else 0.12f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = inkColor.copy(alpha = rimAlpha),
                        radius = baseRadius,
                        center = circleCenter,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
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
                val ip = irisProgress.value
                alpha = (1f - ip * 1.8f).coerceIn(0f, 1f)
                scaleX = 1f + ip * 0.15f
                scaleY = 1f + ip * 0.15f
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Mascot Avatar (clean, relaxed, dynamic expression)
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
                    expression = mascotExpression,
                    size = mascotSize,
                )
            }

            // App Name & Tagline
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .graphicsLayer {
                        val ip = irisProgress.value
                        alpha = (1f - ip * 2.5f).coerceIn(0f, 1f)
                        translationY = ip * 30f
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
