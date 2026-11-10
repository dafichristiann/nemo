package com.anchor.app.core.design.components

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.R
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoNight
import kotlinx.coroutines.isActive

enum class MascotExpression {
    NEUTRAL, HAPPY, SAD, ANGRY, SURPRISED, SLEEPY,
    CONFUSED, LAUGHING, SHY, THINKING, WINK, TALKING
}

/**
 * Keyframe representation for body transform.
 * x, y: percentage of mascot box size (positive x = right, positive y = down).
 * rot: rotation degrees (clockwise positive).
 * sx, sy: scale (1.0 = normal).
 */
private data class BodyKeyframe(
    val t: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val rot: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
)

private data class BodyTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val rot: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
)

private data class FaceSwapSpec(
    val faceDrawableRes: Int,
    val showDuring: List<Pair<Float, Float>>,
    val durationMs: Int,
)

private data class FxKeyframe(
    val t: Float,
    val opacity: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val rot: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
)

private data class FxSpec(
    val fxDrawableRes: Int,
    val durationMs: Int,
    val originX: Float = 0.5f,
    val originY: Float = 0.5f,
    val easing: Easing = EaseInOut,
    val keyframes: List<FxKeyframe>? = null,
    val revealDirection: RevealDirection? = null,
    val revealKeyframes: List<Pair<Float, Float>>? = null,
    val moveKeyframes: List<FxKeyframe>? = null,
    val fadeKeyframes: List<Pair<Float, Float>>? = null,
)

private enum class RevealDirection {
    FROM_BOTTOM,
    FROM_LEFT,
}

private data class MascotAnimationSpec(
    val headDrawableRes: Int,
    val durationMs: Int,
    val easing: Easing = EaseInOut,
    val originX: Float = 0.5f,
    val originY: Float = 0.88f,
    val keyframes: List<BodyKeyframe>,
    val alternate: Boolean = false,
    val faceSwap: FaceSwapSpec? = null,
    val fx: FxSpec? = null,
)

// Standard CSS Easing curves per maskot-animasi-spec.md
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val HappyCubic = CubicBezierEasing(0.3f, 0f, 0.4f, 1f)
private val LinearEase = Easing { it }

private fun getSpecForExpression(expression: MascotExpression): MascotAnimationSpec {
    return when (expression) {
        MascotExpression.NEUTRAL -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_neutral,
            durationMs = 4000,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.5f, 0f, -4f, 0f, 1.015f, 1.015f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
            faceSwap = FaceSwapSpec(
                faceDrawableRes = R.drawable.mascot_wink,
                showDuring = listOf(
                    0.25f to 0.30f,
                    0.72f to 0.77f,
                ),
                durationMs = 4000,
            ),
        )

        MascotExpression.HAPPY -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_happy,
            durationMs = 1100,
            easing = HappyCubic,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1.07f, 0.93f),
                BodyKeyframe(0.15f, 0f, 0f, 0f, 0.96f, 1.05f),
                BodyKeyframe(0.45f, 0f, -16f, 0f, 0.98f, 1.04f),
                BodyKeyframe(0.75f, 0f, 0f, 0f, 1.07f, 0.93f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1.07f, 0.93f),
            ),
        )

        MascotExpression.SAD -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_sad,
            durationMs = 5000,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.5f, 0f, 7f, -5f, 0.97f, 0.97f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
        )

        MascotExpression.ANGRY -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_angry,
            durationMs = 2400,
            easing = LinearEase,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.54f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.57f, -3f, 0f, -2f, 1.04f, 1.04f),
                BodyKeyframe(0.61f, 3f, 0f, 2f, 1.04f, 1.04f),
                BodyKeyframe(0.65f, -3f, 0f, -2f, 1.04f, 1.04f),
                BodyKeyframe(0.69f, 3f, 0f, 2f, 1.04f, 1.04f),
                BodyKeyframe(0.73f, -3f, 0f, -2f, 1.04f, 1.04f),
                BodyKeyframe(0.77f, 3f, 0f, 2f, 1.04f, 1.04f),
                BodyKeyframe(0.82f, 0f, 0f, 0f, 1.04f, 1.04f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
        )

        MascotExpression.SURPRISED -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_surprised,
            durationMs = 2800,
            easing = EaseOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.55f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.60f, 0f, -6f, 0f, 1.18f, 1.18f),
                BodyKeyframe(0.68f, 0f, 0f, 0f, 0.96f, 0.96f),
                BodyKeyframe(0.76f, 0f, 0f, 0f, 1.07f, 1.07f),
                BodyKeyframe(0.86f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
            fx = FxSpec(
                fxDrawableRes = R.drawable.mascot_surprised_fx,
                durationMs = 2800,
                originX = 0.5f,
                originY = 0.5f,
                easing = EaseOut,
                keyframes = listOf(
                    FxKeyframe(0f, opacity = 0f, sx = 0.8f, sy = 0.8f),
                    FxKeyframe(0.55f, opacity = 0f, sx = 0.8f, sy = 0.8f),
                    FxKeyframe(0.60f, opacity = 1f, sx = 1.12f, sy = 1.12f),
                    FxKeyframe(0.86f, opacity = 1f, sx = 1f, sy = 1f),
                    FxKeyframe(0.96f, opacity = 0f, sx = 1f, sy = 1f),
                    FxKeyframe(1f, opacity = 0f, sx = 1f, sy = 1f),
                ),
            ),
        )

        MascotExpression.SLEEPY -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_sleepy,
            durationMs = 4500,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.96f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.48f, 0f, 4f, 14f, 1f, 1f),
                BodyKeyframe(0.57f, 0f, 4f, 14f, 1f, 1f),
                BodyKeyframe(0.63f, 0f, 0f, -3f, 1f, 1f),
                BodyKeyframe(0.72f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
            fx = FxSpec(
                fxDrawableRes = R.drawable.mascot_sleepy_fx,
                durationMs = 3000,
                originX = 0.5f,
                originY = 0.5f,
                revealDirection = RevealDirection.FROM_BOTTOM,
                revealKeyframes = listOf(
                    0f to 0f,
                    0.35f to 1f,
                    1f to 1f,
                ),
                moveKeyframes = listOf(
                    FxKeyframe(0f, x = 0f, y = 8f),
                    FxKeyframe(1f, x = 7f, y = -12f),
                ),
                fadeKeyframes = listOf(
                    0f to 0f,
                    0.25f to 1f,
                    0.70f to 1f,
                    1f to 0f,
                ),
            ),
        )

        MascotExpression.CONFUSED -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_confused,
            durationMs = 3600,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, -7f, 1f, 1f),
                BodyKeyframe(0.5f, 2f, 0f, 7f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, -7f, 1f, 1f),
            ),
            fx = FxSpec(
                fxDrawableRes = R.drawable.mascot_confused_fx,
                durationMs = 1800,
                originX = 0.5f,
                originY = 0.5f,
                easing = EaseInOut,
                keyframes = listOf(
                    FxKeyframe(0f, x = 0f, y = 0f, rot = 0f, sx = 1f, sy = 1f),
                    FxKeyframe(0.5f, x = 0f, y = -8f, rot = 14f, sx = 1.1f, sy = 1.1f),
                    FxKeyframe(1f, x = 0f, y = 0f, rot = 0f, sx = 1f, sy = 1f),
                ),
            ),
        )

        MascotExpression.LAUGHING -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_laughing,
            durationMs = 1800,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.06f, 0f, -7f, -7f, 1f, 1f),
                BodyKeyframe(0.14f, 0f, 1f, -5f, 1.03f, 0.97f),
                BodyKeyframe(0.22f, 0f, -7f, -7f, 1f, 1f),
                BodyKeyframe(0.30f, 0f, 1f, -5f, 1.03f, 0.97f),
                BodyKeyframe(0.38f, 0f, -7f, -7f, 1f, 1f),
                BodyKeyframe(0.46f, 0f, 1f, -5f, 1.03f, 0.97f),
                BodyKeyframe(0.56f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
        )

        MascotExpression.SHY -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_shy,
            durationMs = 4200,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.25f, -7f, 5f, 7f, 0.92f, 0.92f),
                BodyKeyframe(0.32f, -9f, 5f, 4f, 0.92f, 0.92f),
                BodyKeyframe(0.39f, -6f, 5f, 9f, 0.92f, 0.92f),
                BodyKeyframe(0.60f, -7f, 5f, 7f, 0.92f, 0.92f),
                BodyKeyframe(0.76f, 1f, 0f, -2f, 1.03f, 1.03f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
        )

        MascotExpression.THINKING -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_thinking,
            durationMs = 5000,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.30f, -2f, 0f, -6f, 1f, 1f),
                BodyKeyframe(0.70f, -2f, 0f, -6f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
            fx = FxSpec(
                fxDrawableRes = R.drawable.mascot_thinking_fx,
                durationMs = 2400,
                originX = 0.5f,
                originY = 0.5f,
                revealDirection = RevealDirection.FROM_LEFT,
                revealKeyframes = listOf(
                    0f to 0f,
                    0.80f to 1f,
                    1f to 1f,
                ),
            ),
        )

        MascotExpression.WINK -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_wink,
            durationMs = 2600,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.25f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.32f, 0f, -6f, -5f, 1.08f, 1.08f),
                BodyKeyframe(0.45f, 0f, 0f, -4f, 1.05f, 1.05f),
                BodyKeyframe(0.68f, 0f, 0f, -4f, 1.05f, 1.05f),
                BodyKeyframe(0.78f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
            faceSwap = FaceSwapSpec(
                faceDrawableRes = R.drawable.mascot_neutral,
                showDuring = listOf(
                    0f to 0.28f,
                    0.72f to 1f,
                ),
                durationMs = 2600,
            ),
        )

        MascotExpression.TALKING -> MascotAnimationSpec(
            headDrawableRes = R.drawable.mascot_talking,
            durationMs = 1000,
            easing = EaseInOut,
            originX = 0.5f,
            originY = 0.88f,
            alternate = true,
            keyframes = listOf(
                BodyKeyframe(0f, 0f, 0f, 0f, 1f, 1f),
                BodyKeyframe(0.5f, 0f, -2.5f, 0f, 1f, 1f),
                BodyKeyframe(1f, 0f, 0f, 0f, 1f, 1f),
            ),
            faceSwap = FaceSwapSpec(
                faceDrawableRes = R.drawable.mascot_neutral,
                showDuring = listOf(
                    0.10f to 0.18f,
                    0.34f to 0.42f,
                    0.52f to 0.60f,
                    0.78f to 1.00f,
                ),
                durationMs = 2000,
            ),
        )
    }
}

/**
 * High-performance keyframe interpolation across piecewise segments.
 */
private fun interpolateKeyframes(
    keyframes: List<BodyKeyframe>,
    progress: Float,
    easing: Easing,
): BodyTransform {
    if (keyframes.isEmpty()) return BodyTransform()
    if (keyframes.size == 1) {
        val k = keyframes[0]
        return BodyTransform(k.x, k.y, k.rot, k.sx, k.sy)
    }
    val p = progress.coerceIn(0f, 1f)
    var i = 0
    while (i < keyframes.size - 2 && keyframes[i + 1].t < p) {
        i++
    }
    val k0 = keyframes[i]
    val k1 = keyframes[i + 1]
    val segmentRange = k1.t - k0.t
    val rawFrac = if (segmentRange > 0.0001f) ((p - k0.t) / segmentRange).coerceIn(0f, 1f) else 0f
    val eased = easing.transform(rawFrac)

    return BodyTransform(
        x = k0.x + (k1.x - k0.x) * eased,
        y = k0.y + (k1.y - k0.y) * eased,
        rot = k0.rot + (k1.rot - k0.rot) * eased,
        sx = k0.sx + (k1.sx - k0.sx) * eased,
        sy = k0.sy + (k1.sy - k0.sy) * eased,
    )
}

/**
 * Checks system setting for reduced motion / animation disabled.
 */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Dynamic Nemo Mascot Component.
 * - 12 Chibi expressions powered by code keyframes, face swaps, and FX layers.
 * - Respects system reduced-motion and [paused] parameter.
 * - Smooth 120ms crossfade between expression changes.
 * - Automatic white circle badge on dark backgrounds for crisp ink visibility.
 */
@Composable
fun NemoMascot(
    expression: MascotExpression,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    paused: Boolean = false,
    contentDescription: String? = null,
) {
    val isDark = Nemo.colors.background == NemoNight.background
    val reduceMotion = rememberReduceMotion()
    val isStatic = paused || reduceMotion

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isDark) {
                    Modifier
                        .background(Color.White, CircleShape)
                        .padding(size * 0.08f)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = expression,
            animationSpec = tween(durationMillis = 120),
            label = "nemo_mascot_crossfade",
        ) { currentExpression ->
            val spec = remember(currentExpression) { getSpecForExpression(currentExpression) }

            if (isStatic) {
                // Static render for accessibility / paused state
                Image(
                    painter = painterResource(id = spec.headDrawableRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MascotAnimatedLayers(
                    spec = spec,
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

@Composable
private fun MascotAnimatedLayers(
    spec: MascotAnimationSpec,
    contentDescription: String?,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(spec) {
        val startNano = System.nanoTime()
        while (isActive) {
            withFrameNanos { nowNano ->
                elapsedMs = (nowNano - startNano) / 1_000_000L
            }
        }
    }

    // Body transform calculated during draw/layout phase inside graphicsLayer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val cycleMs = spec.durationMs
                val cyclePos = (elapsedMs % cycleMs).toFloat() / cycleMs
                val currentTransform = interpolateKeyframes(spec.keyframes, cyclePos, spec.easing)

                val boxSize = size.width
                translationX = (currentTransform.x / 100f) * boxSize
                translationY = (currentTransform.y / 100f) * boxSize
                rotationZ = currentTransform.rot
                scaleX = currentTransform.sx
                scaleY = currentTransform.sy
                transformOrigin = TransformOrigin(spec.originX, spec.originY)
            },
        contentAlignment = Alignment.Center,
    ) {
        // Layer 1: Base Head
        Image(
            painter = painterResource(id = spec.headDrawableRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Face Swap (e.g. blink or talking mouth movement)
        if (spec.faceSwap != null) {
            val swap = spec.faceSwap
            Image(
                painter = painterResource(id = swap.faceDrawableRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val swapPos = (elapsedMs % swap.durationMs).toFloat() / swap.durationMs
                        val isVisible = swap.showDuring.any { (start, end) ->
                            swapPos in start..end
                        }
                        alpha = if (isVisible) 1f else 0f
                    },
            )
        }

        // Layer 3: FX Symbols (garis kejut, zzz, tanda tanya, titik elipsis)
        if (spec.fx != null) {
            val fx = spec.fx
            Image(
                painter = painterResource(id = fx.fxDrawableRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val fxCycle = (elapsedMs % fx.durationMs).toFloat() / fx.durationMs
                        transformOrigin = TransformOrigin(fx.originX, fx.originY)

                        if (fx.keyframes != null) {
                            val kfs = fx.keyframes
                            var i = 0
                            while (i < kfs.size - 2 && kfs[i + 1].t < fxCycle) {
                                i++
                            }
                            val k0 = kfs[i]
                            val k1 = kfs[(i + 1).coerceAtMost(kfs.size - 1)]
                            val seg = k1.t - k0.t
                            val raw = if (seg > 0.0001f) ((fxCycle - k0.t) / seg).coerceIn(0f, 1f) else 0f
                            val eased = fx.easing.transform(raw)

                            alpha = k0.opacity + (k1.opacity - k0.opacity) * eased
                            val boxSize = size.width
                            translationX = (k0.x + (k1.x - k0.x) * eased) / 100f * boxSize
                            translationY = (k0.y + (k1.y - k0.y) * eased) / 100f * boxSize
                            rotationZ = k0.rot + (k1.rot - k0.rot) * eased
                            scaleX = k0.sx + (k1.sx - k0.sx) * eased
                            scaleY = k0.sy + (k1.sy - k0.sy) * eased
                        } else {
                            // Handled via separate move/fade specs
                            var fxAlpha = 1f
                            if (fx.fadeKeyframes != null) {
                                val fks = fx.fadeKeyframes
                                var fi = 0
                                while (fi < fks.size - 2 && fks[fi + 1].first < fxCycle) {
                                    fi++
                                }
                                val f0 = fks[fi]
                                val f1 = fks[fi + 1]
                                val fseg = f1.first - f0.first
                                val fraw = if (fseg > 0.0001f) ((fxCycle - f0.first) / fseg).coerceIn(0f, 1f) else 0f
                                fxAlpha = f0.second + (f1.second - f0.second) * fraw
                            }
                            alpha = fxAlpha

                            if (fx.moveKeyframes != null) {
                                val mk0 = fx.moveKeyframes[0]
                                val mk1 = fx.moveKeyframes[1]
                                val eased = EaseInOut.transform(fxCycle)
                                val boxSize = size.width
                                translationX = (mk0.x + (mk1.x - mk0.x) * eased) / 100f * boxSize
                                translationY = (mk0.y + (mk1.y - mk0.y) * eased) / 100f * boxSize
                            }
                        }
                    }
                    .drawWithContent {
                        if (fx.revealDirection != null && fx.revealKeyframes != null) {
                            val fxCycle = (elapsedMs % fx.durationMs).toFloat() / fx.durationMs
                            val rEnd = fx.revealKeyframes[1].first
                            val fraction = (fxCycle / rEnd).coerceIn(0f, 1f)

                            // steps(3, end): 3 discrete jumps
                            val stepFraction = if (fraction >= 1f) {
                                1f
                            } else {
                                val step = (fraction * 3f).toInt().coerceIn(0, 2)
                                (step + 1) / 3f
                            }

                            when (fx.revealDirection) {
                                RevealDirection.FROM_BOTTOM -> {
                                    val topClip = size.height * (1f - stepFraction)
                                    clipRect(
                                        left = 0f,
                                        top = topClip,
                                        right = size.width,
                                        bottom = size.height,
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                                RevealDirection.FROM_LEFT -> {
                                    val rightClip = size.width * stepFraction
                                    clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = rightClip,
                                        bottom = size.height,
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            }
                        } else {
                            drawContent()
                        }
                    },
            )
        }
    }
}
