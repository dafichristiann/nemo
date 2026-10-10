package com.anchor.app.feature.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import com.anchor.app.R
import com.anchor.app.core.common.DateTimeFormatter
import com.anchor.app.core.common.short
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.platform.usagestats.UsageAccessSettings

/**
 * 03 Digital Wellbeing (docs/superpowers/design/07-screens.md §2.7, PRD §10.2).
 *
 * Screen-time card, top apps, and a 7-day bar chart. All numbers come from Android usage
 * data (FR-W7). Honest states: permission missing → a Settings guide; no data → empty
 * message. Never crashes, never fakes a number (AC §10.2).
 */
@Composable
fun WellbeingScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: WellbeingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-read the permission + summaries whenever the screen comes back to the foreground, so
    // granting Usage access in system Settings (then returning) reflects immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The Usage-access permission is granted only in OS Settings, so the CTA opens the system
    // screen directly (PRD §14); the in-app settings remain reachable separately.
    val openUsageAccess: () -> Unit = { UsageAccessSettings.open(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x5),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_title),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Nemo.colors.ink,
        )

        when (state.availability) {
            WellbeingAvailability.PERMISSION_MISSING -> PermissionEmptyState(
                onOpenSettings = openUsageAccess,
                onOpenAppSettings = onOpenSettings,
            )
            WellbeingAvailability.NO_DATA -> NoDataEmptyState()
            WellbeingAvailability.AVAILABLE -> {
                val selectedLabel = if (state.selectedIsToday) {
                    stringResource(R.string.wellbeing_day_today)
                } else {
                    DateTimeFormatter.dateLabelComma(state.selectedDate)
                }
                val previousLabel = if (state.previousIsYesterday) {
                    stringResource(R.string.wellbeing_day_yesterday)
                } else {
                    state.previousDate.dayOfWeek.short()
                }
                ScreenTimeCard(
                    label = selectedLabel,
                    totalMs = state.totalMs,
                    previousMs = state.previousTotalMs,
                    previousLabel = previousLabel,
                )
                StatsOverview(state = state, previousLabel = previousLabel)
                if (state.topApps.isNotEmpty()) {
                    SectionHeader(title = stringResource(R.string.wellbeing_top_apps))
                    TopApps(state.topApps)
                }
                SectionHeader(title = stringResource(R.string.wellbeing_this_week))
                WeeklyChart(week = state.week, onSelectDay = viewModel::onSelectDay)
                Text(
                    text = stringResource(R.string.wellbeing_local_only),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                )
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(
    label: String,
    totalMs: Long,
    previousMs: Long,
    previousLabel: String,
) {
    val delta = totalMs - previousMs
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.surface)
            .padding(Spacing.cardHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.wellbeing_screen_time),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Nemo.colors.inkSecondary,
            )
            Text(
                text = label,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Nemo.colors.mute,
            )
        }
        Text(
            text = formatDuration(totalMs),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            color = Nemo.colors.ink,
        )
        val comparison = when {
            previousMs <= 0L -> stringResource(R.string.wellbeing_no_comparison)
            delta == 0L -> stringResource(R.string.wellbeing_same_as_previous, previousLabel)
            delta > 0L -> stringResource(R.string.wellbeing_more_than_previous, formatDuration(delta), previousLabel)
            else -> stringResource(R.string.wellbeing_less_than_previous, formatDuration(-delta), previousLabel)
        }
        Text(
            text = comparison,
            fontFamily = ManropeFamily,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
    }
}

@Composable
private fun StatsOverview(state: WellbeingUiState, previousLabel: String) {
    // Only render when there is at least one honest comparison to show.
    val hasVsYesterday = state.vsYesterday != null
    val hasVsAverage = state.vsWeekAverage != null
    val hasDistraction = state.distractionDeltaPctPoints != null
    if (!hasVsYesterday && !hasVsAverage && !hasDistraction) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.surface)
            .padding(Spacing.cardHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_stats_title),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
        )
        state.vsYesterday?.let { cmp ->
            StatRow(
                label = stringResource(R.string.wellbeing_stats_vs_yesterday, previousLabel),
                comparison = cmp,
            )
        }
        state.vsWeekAverage?.let { cmp ->
            StatRow(
                label = stringResource(R.string.wellbeing_stats_vs_avg, state.daysWithData),
                comparison = cmp,
            )
        }
        state.distractionDeltaPctPoints?.let { points ->
            DistractionRow(points = points, previousLabel = previousLabel)
        }
    }
}

@Composable
private fun StatRow(label: String, comparison: StatComparison) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(Spacing.x2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trendGlyph(comparison.direction),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = trendColor(comparison.direction),
            )
            Spacer(modifier = Modifier.width(Spacing.x1))
            Text(
                // Prefer the honest percentage; fall back to the absolute delta when the
                // baseline was zero (no percentage is defined).
                text = comparison.deltaPct?.let { "${if (it > 0) "+" else ""}$it%" }
                    ?: formatDelta(comparison.deltaMs),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = trendColor(comparison.direction),
            )
        }
    }
}

@Composable
private fun DistractionRow(points: Float, previousLabel: String) {
    val direction = when {
        points > 0f -> TrendDirection.UP
        points < 0f -> TrendDirection.DOWN
        else -> TrendDirection.FLAT
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.wellbeing_stats_distraction, previousLabel),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(Spacing.x2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trendGlyph(direction),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = trendColor(direction),
            )
            Spacer(modifier = Modifier.width(Spacing.x1))
            Text(
                text = "${if (points > 0f) "+" else ""}${kotlin.math.round(points)}pp",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = trendColor(direction),
            )
        }
    }
}

private fun trendGlyph(direction: TrendDirection): String = when (direction) {
    TrendDirection.UP -> "\u25B2"
    TrendDirection.DOWN -> "\u25BC"
    TrendDirection.FLAT -> "\u2013"
}

/** Up = more screen time (a nudge, not a scold); down = less. Neutral grey when flat. */
@Composable
private fun trendColor(direction: TrendDirection): Color = when (direction) {
    TrendDirection.UP -> Nemo.colors.ink
    TrendDirection.DOWN -> Nemo.colors.mute
    TrendDirection.FLAT -> Nemo.colors.mute
}

/** "+25m" / "-1h 5m" for the absolute-delta fallback. */
private fun formatDelta(ms: Long): String {
    if (ms == 0L) return "0m"
    val sign = if (ms > 0) "+" else "-"
    return sign + formatDuration(kotlin.math.abs(ms))
}

@Composable
private fun TopApps(apps: List<TopAppRow>) {
    // Remembered: an O(n) scan that would otherwise re-run on every recomposition of this section.
    val maxMs = remember(apps) { apps.maxOf { it.totalMs } }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
        apps.forEach { app ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Nemo.colors.fog),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app.label.take(1),
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Nemo.colors.inkSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Nemo.colors.ink,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(app.totalMs.toFloat() / maxMs)
                            .height(3.dp)
                            .clip(RoundedCornerShape(Radius.track))
                            .background(Nemo.colors.ink),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x3))
                Text(
                    text = formatDuration(app.totalMs),
                    fontFamily = ManropeFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.mute,
                )
            }
        }
    }
}

@Composable
private fun WeeklyChart(week: List<DayBar>, onSelectDay: (Long) -> Unit) {
    val maxMs = remember(week) { week.maxOf { it.totalMs } }.coerceAtLeast(1L)
    // Animate the chart in on first composition: bars grow from the baseline (calm motion).
    var appeared by remember { mutableStateOf(false) }
    // Remember the key so the effect isn't keyed on a freshly-allocated list each recomposition.
    val weekKey = remember(week) { week.map { it.epochDay } }
    LaunchedEffect(weekKey) { appeared = true }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { day ->
            val targetFraction = (8 + (72 * day.totalMs / maxMs).toInt()).dp
            val animatedHeight by animateDpAsState(
                targetValue = if (appeared) targetFraction else 8.dp,
                animationSpec = Motion.enterSpec(),
                label = "weekly-bar",
            )
            val emphasized = day.isSelected || day.isToday
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .pressable(shape = PillShape) { onSelectDay(day.epochDay) }
                    .padding(vertical = Spacing.x1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(animatedHeight)
                        .clip(RoundedCornerShape(Radius.track))
                        .background(if (emphasized) Nemo.colors.ink else Nemo.colors.fog),
                )
                Spacer(modifier = Modifier.height(Spacing.x2))
                Text(
                    text = day.label,
                    fontFamily = ManropeFamily,
                    fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (emphasized) Nemo.colors.ink else Nemo.colors.mute,
                )
            }
        }
    }
}

@Composable
private fun PermissionEmptyState(onOpenSettings: () -> Unit, onOpenAppSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Nemo.colors.fog),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = Nemo.colors.mute,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(R.string.wellbeing_perm_title),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Nemo.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.wellbeing_perm_body),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            label = stringResource(R.string.wellbeing_perm_cta),
            onClick = onOpenSettings,
        )
        Text(
            text = stringResource(R.string.wellbeing_perm_app_settings),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            modifier = Modifier
                .clip(PillShape)
                .pressable(shape = PillShape, onClick = onOpenAppSettings)
                .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
        )
    }
}

@Composable
private fun NoDataEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Nemo.colors.fog),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = Nemo.colors.mute,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(R.string.wellbeing_nodata_title),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Nemo.colors.inkSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.wellbeing_nodata_body),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            textAlign = TextAlign.Center,
        )
    }
}

/** "2h 15m", "45m", "0m". */
private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
