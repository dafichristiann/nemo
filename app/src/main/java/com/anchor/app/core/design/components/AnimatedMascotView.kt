package com.anchor.app.core.design.components

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.provider.Settings
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.anchor.app.R

fun interface MascotEasing {
    fun transform(fraction: Float): Float
}

class CubicBezier(
    private val x1: Float,
    private val y1: Float,
    private val x2: Float,
    private val y2: Float,
) : MascotEasing {
    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        var start = 0f
        var end = 1f
        for (i in 0 until 8) {
            val mid = (start + end) / 2f
            val x = sampleCurve(mid, x1, x2)
            if (x < fraction) {
                start = mid
            } else {
                end = mid
            }
        }
        val t = (start + end) / 2f
        return sampleCurve(t, y1, y2)
    }

    private fun sampleCurve(t: Float, c1: Float, c2: Float): Float {
        val oneMinusT = 1f - t
        return 3f * oneMinusT * oneMinusT * t * c1 + 3f * oneMinusT * t * t * c2 + t * t * t
    }
}

/**
 * Android View-based Dynamic Animated Mascot component (specs: maskot-animasi-spec.md).
 * Used in overlay windows (BlockOverlayWindow, DoomscrollOverlayWindow) and standard View layouts.
 *
 * Drives animations purely through 2D transforms and canvas clipping on static PNG assets.
 * Respects system reduced motion settings and stops animations when detached to avoid leaks.
 */
class AnimatedMascotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val headView: ImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private var fxRevealRatio: Float = 1f

    private val fxView: ImageView = object : ImageView(context) {
        override fun onDraw(canvas: Canvas) {
            val saveCount = canvas.save()
            // Reveal from left clipping
            val clipWidth = width * fxRevealRatio
            canvas.clipRect(0f, 0f, clipWidth, height.toFloat())
            super.onDraw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }.apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private var currentExpression: MascotExpression = MascotExpression.NEUTRAL
    private var bodyAnimator: ValueAnimator? = null
    private var fxAnimator: ValueAnimator? = null

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setWillNotDraw(false)
        addView(headView)
        addView(fxView)
    }

    private fun isNightMode(): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onDraw(canvas: Canvas) {
        if (isNightMode() && width > 0 && height > 0) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) / 1.5f
            glowPaint.shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    Color.argb((0.18f * 255).toInt(), 255, 255, 255),
                    Color.argb((0.06f * 255).toInt(), 255, 255, 255),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, glowPaint)
        }
        super.onDraw(canvas)
    }

    data class Keyframe(
        val t: Float,
        val x: Float = 0f,
        val y: Float = 0f,
        val rot: Float = 0f,
        val sx: Float = 1f,
        val sy: Float = 1f,
    )

    data class MascotConfig(
        val headRes: Int,
        val fxRes: Int? = null,
        val bodyDurationMs: Long,
        val bodyEasing: MascotEasing,
        val originX: Float = 0.5f,
        val originY: Float = 0.88f,
        val keyframes: List<Keyframe>,
        val fxDurationMs: Long = 0L,
    )

    companion object {
        val LinearEase = MascotEasing { it }
        val EaseInOut = CubicBezier(0.42f, 0f, 0.58f, 1f)
        val EaseOut = CubicBezier(0f, 0f, 0.58f, 1f)
        val HappyCubic = CubicBezier(0.3f, 0f, 0.4f, 1f)

        fun getConfig(expression: MascotExpression): MascotConfig {
            return when (expression) {
                MascotExpression.ANGRY -> MascotConfig(
                    headRes = R.drawable.mascot_angry,
                    fxRes = null,
                    bodyDurationMs = 2400L,
                    bodyEasing = LinearEase,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.54f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.57f, -3f, 0f, -2f, 1.04f, 1.04f),
                        Keyframe(0.61f, 3f, 0f, 2f, 1.04f, 1.04f),
                        Keyframe(0.65f, -3f, 0f, -2f, 1.04f, 1.04f),
                        Keyframe(0.69f, 3f, 0f, 2f, 1.04f, 1.04f),
                        Keyframe(0.73f, -3f, 0f, -2f, 1.04f, 1.04f),
                        Keyframe(0.77f, 3f, 0f, 2f, 1.04f, 1.04f),
                        Keyframe(0.82f, 0f, 0f, 0f, 1.04f, 1.04f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )

                MascotExpression.THINKING -> MascotConfig(
                    headRes = R.drawable.mascot_thinking,
                    fxRes = R.drawable.mascot_thinking_fx,
                    bodyDurationMs = 5000L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.30f, -2f, 0f, -6f, 1f, 1f),
                        Keyframe(0.70f, -2f, 0f, -6f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                    fxDurationMs = 2400L,
                )

                MascotExpression.HAPPY -> MascotConfig(
                    headRes = R.drawable.mascot_happy,
                    fxRes = null,
                    bodyDurationMs = 1100L,
                    bodyEasing = HappyCubic,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1.07f, 0.93f),
                        Keyframe(0.15f, 0f, 0f, 0f, 0.96f, 1.05f),
                        Keyframe(0.45f, 0f, -16f, 0f, 0.98f, 1.04f),
                        Keyframe(0.75f, 0f, 0f, 0f, 1.07f, 0.93f),
                        Keyframe(1f, 0f, 0f, 0f, 1.07f, 0.93f),
                    ),
                )

                MascotExpression.SAD -> MascotConfig(
                    headRes = R.drawable.mascot_sad,
                    fxRes = null,
                    bodyDurationMs = 5000L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.5f, 0f, 7f, -5f, 0.97f, 0.97f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )

                MascotExpression.CONFUSED -> MascotConfig(
                    headRes = R.drawable.mascot_confused,
                    fxRes = R.drawable.mascot_confused_fx,
                    bodyDurationMs = 3600L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, -7f, 1f, 1f),
                        Keyframe(0.5f, 2f, 0f, 7f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, -7f, 1f, 1f),
                    ),
                    fxDurationMs = 1800L,
                )

                MascotExpression.SURPRISED -> MascotConfig(
                    headRes = R.drawable.mascot_surprised,
                    fxRes = R.drawable.mascot_surprised_fx,
                    bodyDurationMs = 2800L,
                    bodyEasing = EaseOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.55f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.60f, 0f, -6f, 0f, 1.18f, 1.18f),
                        Keyframe(0.68f, 0f, 0f, 0f, 0.96f, 0.96f),
                        Keyframe(0.76f, 0f, 0f, 0f, 1.07f, 1.07f),
                        Keyframe(0.86f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                    fxDurationMs = 2800L,
                )

                MascotExpression.SLEEPY -> MascotConfig(
                    headRes = R.drawable.mascot_sleepy,
                    fxRes = R.drawable.mascot_sleepy_fx,
                    bodyDurationMs = 4500L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.96f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.48f, 0f, 4f, 14f, 1f, 1f),
                        Keyframe(0.57f, 0f, 4f, 14f, 1f, 1f),
                        Keyframe(0.63f, 0f, 0f, -3f, 1f, 1f),
                        Keyframe(0.72f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                    fxDurationMs = 3000L,
                )

                MascotExpression.LAUGHING -> MascotConfig(
                    headRes = R.drawable.mascot_laughing,
                    fxRes = null,
                    bodyDurationMs = 1800L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.06f, 0f, -7f, -7f, 1f, 1f),
                        Keyframe(0.14f, 0f, 1f, -5f, 1.03f, 0.97f),
                        Keyframe(0.22f, 0f, -7f, -7f, 1f, 1f),
                        Keyframe(0.30f, 0f, 1f, -5f, 1.03f, 0.97f),
                        Keyframe(0.38f, 0f, -7f, -7f, 1f, 1f),
                        Keyframe(0.46f, 0f, 1f, -5f, 1.03f, 0.97f),
                        Keyframe(0.56f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )

                MascotExpression.SHY -> MascotConfig(
                    headRes = R.drawable.mascot_shy,
                    fxRes = null,
                    bodyDurationMs = 4200L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.25f, -7f, 5f, 7f, 0.92f, 0.92f),
                        Keyframe(0.32f, -9f, 5f, 4f, 0.92f, 0.92f),
                        Keyframe(0.39f, -6f, 5f, 9f, 0.92f, 0.92f),
                        Keyframe(0.60f, -7f, 5f, 7f, 0.92f, 0.92f),
                        Keyframe(0.76f, 1f, 0f, -2f, 1.03f, 1.03f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )

                MascotExpression.WINK -> MascotConfig(
                    headRes = R.drawable.mascot_wink,
                    fxRes = null,
                    bodyDurationMs = 2600L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.25f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.32f, 0f, -6f, -5f, 1.08f, 1.08f),
                        Keyframe(0.45f, 0f, 0f, -4f, 1.05f, 1.05f),
                        Keyframe(0.68f, 0f, 0f, -4f, 1.05f, 1.05f),
                        Keyframe(0.78f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )

                MascotExpression.TALKING -> MascotConfig(
                    headRes = R.drawable.mascot_talking,
                    fxRes = null,
                    bodyDurationMs = 1000L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.5f, 0f, -2.5f, 0f, 1f, 1f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )

                MascotExpression.NEUTRAL -> MascotConfig(
                    headRes = R.drawable.mascot_neutral,
                    fxRes = null,
                    bodyDurationMs = 4000L,
                    bodyEasing = EaseInOut,
                    originX = 0.5f,
                    originY = 0.88f,
                    keyframes = listOf(
                        Keyframe(0f, 0f, 0f, 0f, 1f, 1f),
                        Keyframe(0.5f, 0f, -4f, 0f, 1.015f, 1.015f),
                        Keyframe(1f, 0f, 0f, 0f, 1f, 1f),
                    ),
                )
            }
        }

        fun interpolate(keyframes: List<Keyframe>, t: Float, easing: MascotEasing): Keyframe {
            if (keyframes.isEmpty()) return Keyframe(0f)
            if (keyframes.size == 1) return keyframes[0]
            val clampedT = t.coerceIn(0f, 1f)

            var i = 0
            while (i < keyframes.size - 1 && keyframes[i + 1].t < clampedT) {
                i++
            }
            if (i >= keyframes.size - 1) return keyframes.last()

            val k0 = keyframes[i]
            val k1 = keyframes[i + 1]
            val segmentSpan = (k1.t - k0.t).coerceAtLeast(1e-5f)
            val segmentFraction = ((clampedT - k0.t) / segmentSpan).coerceIn(0f, 1f)
            val eased = easing.transform(segmentFraction)

            return Keyframe(
                t = clampedT,
                x = k0.x + (k1.x - k0.x) * eased,
                y = k0.y + (k1.y - k0.y) * eased,
                rot = k0.rot + (k1.rot - k0.rot) * eased,
                sx = k0.sx + (k1.sx - k0.sx) * eased,
                sy = k0.sy + (k1.sy - k0.sy) * eased,
            )
        }
    }

    fun setExpression(expression: MascotExpression) {
        currentExpression = expression
        val config = getConfig(expression)
        headView.setImageResource(config.headRes)

        if (config.fxRes != null) {
            fxView.setImageResource(config.fxRes)
            fxView.visibility = VISIBLE
        } else {
            fxView.setImageDrawable(null)
            fxView.visibility = GONE
        }

        startAnimation()
    }

    private fun isReducedMotion(): Boolean {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    fun startAnimation() {
        stopAnimation()

        if (isReducedMotion()) {
            resetTransforms()
            return
        }

        val config = getConfig(currentExpression)

        // Body animator
        bodyAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = config.bodyDurationMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val kf = interpolate(config.keyframes, t, config.bodyEasing)
                applyTransform(kf, config.originX, config.originY)
            }
            start()
        }

        // FX animator (if FX layer present)
        if (config.fxRes != null && config.fxDurationMs > 0L) {
            fxAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = config.fxDurationMs
                repeatCount = ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { anim ->
                    val t = anim.animatedValue as Float
                    if (currentExpression == MascotExpression.THINKING) {
                        // Steps(3, end) from 0 to 0.8
                        fxRevealRatio = if (t < 0.8f) {
                            val step = (t / 0.8f * 3f).toInt().coerceIn(0, 3)
                            step / 3f
                        } else {
                            1f
                        }
                        fxView.invalidate()
                    } else {
                        fxRevealRatio = 1f
                        fxView.invalidate()
                    }
                }
                start()
            }
        }
    }

    fun stopAnimation() {
        bodyAnimator?.cancel()
        bodyAnimator = null
        fxAnimator?.cancel()
        fxAnimator = null
    }

    private fun applyTransform(kf: Keyframe, originX: Float, originY: Float) {
        val w = width.toFloat().takeIf { it > 0f } ?: return
        val h = height.toFloat().takeIf { it > 0f } ?: return

        pivotX = w * originX
        pivotY = h * originY

        translationX = (kf.x / 100f) * w
        translationY = (kf.y / 100f) * h
        rotation = kf.rot
        scaleX = kf.sx
        scaleY = kf.sy
    }

    private fun resetTransforms() {
        translationX = 0f
        translationY = 0f
        rotation = 0f
        scaleX = 1f
        scaleY = 1f
        fxRevealRatio = 1f
        fxView.invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }
}
