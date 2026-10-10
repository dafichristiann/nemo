package com.anchor.app.core.design.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.anchor.app.R

/**
 * The four top-level tabs (docs/superpowers/design/07-screens.md §2, §6 — "Nav bar 4 tab").
 *
 * Labels stay English by default (PRD §12). Icons are the NEMO line icons (design-spec.md §6,
 * 1.6 stroke) bundled as vector drawables in `res/drawable/`.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    TASKS("tasks", R.string.nav_tasks, R.drawable.ic_tasks),
    WELLBEING("wellbeing", R.string.nav_wellbeing, R.drawable.ic_wellbeing),
    FOCUS("focus", R.string.nav_focus, R.drawable.ic_focus),
    INSIGHTS("insights", R.string.nav_insights, R.drawable.ic_insights),
}
