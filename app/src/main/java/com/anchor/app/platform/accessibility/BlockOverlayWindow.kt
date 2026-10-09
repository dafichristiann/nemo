package com.anchor.app.platform.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.anchor.app.MainActivity
import com.anchor.app.R

/**
 * The "back to focus" interlude, drawn as an accessibility overlay window
 * (`TYPE_ACCESSIBILITY_OVERLAY`, PRD v0.4 §10.5).
 *
 * Why an overlay window rather than an Activity: starting an Activity from a background
 * AccessibilityService is subject to Android 10+ background-activity-start restrictions and is
 * unreliable across OEMs. `TYPE_ACCESSIBILITY_OVERLAY` is granted to AccessibilityServices and is
 * always drawn above the blocked app without those restrictions — the canonical, dependable
 * pattern for a non-coercive nudge. It reads no screen content; it only renders our own view.
 *
 * Deliberately non-coercive: it explains and offers a way back, but the user can always dismiss it
 * (Awareness > Control, PRD §4).
 */
internal class BlockOverlayWindow(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null

    /** Whether the overlay is currently attached (used to debounce re-shows). */
    val isShowing: Boolean get() = view != null

    /**
     * Shows the overlay. Returns true when the view is attached (or was already showing). Returns
     * false when `addView` failed, so the caller can keep the entry un-debounced and retry rather
     * than leaving a blocked app entered with no overlay.
     */
    @SuppressLint("SetTextI18n", "InflateParams")
    fun show(onBackToFocus: () -> Unit): Boolean {
        if (view != null) return true

        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.WHITE)
            }
        }

        val title = TextView(context).apply {
            text = context.getString(R.string.focus_block_overlay_title)
            setTextColor(Color.parseColor("#0A0A0A"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val body = TextView(context).apply {
            text = context.getString(R.string.focus_block_overlay_body)
            setTextColor(Color.parseColor("#555555"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        val hint = TextView(context).apply {
            text = context.getString(R.string.focus_block_overlay_exit_hint)
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        val back = Button(context).apply {
            text = context.getString(R.string.focus_block_overlay_back)
            setOnClickListener {
                hide()
                openFocusScreen()
                onBackToFocus()
            }
        }

        card.addView(title)
        card.addView(body)
        card.addView(hint)
        card.addView(back)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC0A0A0A"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        // Focusable so the "back to focus" button actually receives input (FLAG_NOT_FOCUSABLE
        // would make the only interactive control dead, defeating the "user can always dismiss
        // it" contract). Still non-fullscreen-modal: the user keeps their own way back.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            0,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        // Only mark the entry as handled once the view is actually attached; a failed addView
        // (e.g. transient BadTokenException) must let the caller retry on the next event.
        return runCatching { windowManager.addView(root, params) }
            .onSuccess { view = root }
            .isSuccess
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        view = null
    }

    /** Brings Anchor to the foreground on its Focus tab (the active session lives there). */
    private fun openFocusScreen() {
        runCatching {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, "focus")
            }
            context.startActivity(intent)
        }
    }
}
