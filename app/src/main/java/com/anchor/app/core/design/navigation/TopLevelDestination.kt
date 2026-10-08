package com.anchor.app.core.design.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.anchor.app.R

/**
 * The four top-level tabs (docs/superpowers/design/07-screens.md §2, §6 — "Nav bar 4 tab").
 *
 * Labels stay English by default (PRD §12). Icons follow Material Symbols Rounded
 * equivalents available in Compose's rounded icon set (06-design-system.md §8).
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TASKS("tasks", R.string.nav_tasks, Icons.Rounded.Checklist),
    WELLBEING("wellbeing", R.string.nav_wellbeing, Icons.Rounded.BarChart),
    FOCUS("focus", R.string.nav_focus, Icons.Rounded.Timer),
    INSIGHTS("insights", R.string.nav_insights, Icons.Rounded.Insights),
}
