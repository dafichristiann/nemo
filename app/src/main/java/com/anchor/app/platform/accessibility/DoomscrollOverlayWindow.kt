package com.anchor.app.platform.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.anchor.app.R

/**
 * Non-coercive intervention overlay for continuous doomscrolling (specs 2026-11-01).
 *
 * Rendered using [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY].
 * Reminds users mindfully about prolonged scrolling without force-stopping apps.
 */
class DoomscrollOverlayWindow(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null

    val isShowing: Boolean get() = view != null

    @SuppressLint("SetTextI18n")
    fun show(
        thresholdMinutes: Int,
        onStop: (() -> Unit)? = null,
        onSnooze: () -> Unit,
    ): Boolean {
        if (view != null) return true

        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        fun color(resId: Int) = ContextCompat.getColor(context, resId)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(color(R.color.nemo_surface))
            }
        }

        val mascot = ImageView(context).apply {
            setImageResource(R.drawable.nemo_mascot_thinking)
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(16)
            }
        }

        val title = TextView(context).apply {
            text = context.getString(R.string.doomscroll_overlay_title, thresholdMinutes)
            setTextColor(color(R.color.nemo_ink))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val body = TextView(context).apply {
            text = context.getString(R.string.doomscroll_overlay_body)
            setTextColor(color(R.color.nemo_ink_secondary))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val stopButton = Button(context).apply {
            text = context.getString(R.string.doomscroll_overlay_stop)
            setTextColor(color(R.color.nemo_on_ink))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            stateListAnimator = null
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(color(R.color.nemo_ink))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply {
                topMargin = dp(20)
            }
            setOnClickListener {
                hide()
                navigateHome()
                onStop?.invoke()
            }
        }

        val snoozeButton = Button(context).apply {
            text = context.getString(R.string.doomscroll_overlay_snooze)
            setTextColor(color(R.color.nemo_ink))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            stateListAnimator = null
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(color(R.color.nemo_fog))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply {
                topMargin = dp(10)
            }
            setOnClickListener {
                hide()
                onSnooze()
            }
        }

        card.addView(mascot)
        card.addView(title)
        card.addView(body)
        card.addView(stopButton)
        card.addView(snoozeButton)

        val root = object : LinearLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    hide()
                    navigateHome()
                    onStop?.invoke()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#B3000000"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
            filterTouchesWhenObscured = true
            setOnClickListener {
                hide()
                navigateHome()
                onStop?.invoke()
            }
            card.setOnClickListener { /* Consume click so touching the card does not dismiss */ }
            addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        return runCatching { windowManager.addView(root, params) }
            .onSuccess { view = root }
            .isSuccess
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        view = null
    }

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(homeIntent) }
    }
}
