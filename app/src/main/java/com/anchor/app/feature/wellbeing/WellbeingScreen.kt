package com.anchor.app.feature.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorElevation
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.LocalAnchorElevation
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x5),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AnchorColors.textPrimary,
        )

        when (state.availability) {
            WellbeingAvailability.PERMISSION_MISSING -> PermissionEmptyState(onOpenSettings = onOpenSettings)
            WellbeingAvailability.NO_DATA -> NoDataEmptyState()
            WellbeingAvailability.AVAILABLE -> {
                ScreenTimeCard(
                    todayMs = state.todayTotalMs,
                    yesterdayMs = state.yesterdayTotalMs,
                )
                if (state.topApps.isNotEmpty()) {
                    SectionHeader(title = stringResource(R.string.wellbeing_top_apps))
                    TopApps(state.topApps)
                }
                SectionHeader(title = stringResource(R.string.wellbeing_this_week))
                WeeklyChart(state.week)
                Text(
                    text = stringResource(R.string.wellbeing_local_only),
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = AnchorColors.textFaint,
                )
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(todayMs: Long, yesterdayMs: Long) {
    val elevation: AnchorElevation = LocalAnchorElevation.current
    val delta = todayMs - yesterdayMs
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation.card.dp,
                shape = RoundedCornerShape(Radius.card),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(Radius.card))
            .background(AnchorColors.surfaceCard)
            .padding(Spacing.cardHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_screen_time),
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
        )
        Text(
            text = formatDuration(todayMs),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            color = AnchorColors.textPrimary,
        )
        val comparison = when {
            yesterdayMs <= 0L -> stringResource(R.string.wellbeing_no_comparison)
            delta == 0L -> stringResource(R.string.wellbeing_same_as_yesterday)
            delta > 0L -> stringResource(R.string.wellbeing_more_than_yesterday, formatDuration(delta))
            else -> stringResource(R.string.wellbeing_less_than_yesterday, formatDuration(-delta))
        }
        Text(
            text = comparison,
            fontFamily = InterFamily,
            fontSize = 12.sp,
            color = AnchorColors.textMuted,
        )
    }
}

@Composable
private fun TopApps(apps: List<TopAppRow>) {
    val maxMs = apps.maxOf { it.totalMs }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
        apps.forEach { app ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AnchorColors.surfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app.label.take(1),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AnchorColors.textStrongAlt,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = AnchorColors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(app.totalMs.toFloat() / maxMs)
                            .height(3.dp)
                            .clip(RoundedCornerShape(Radius.track))
                            .background(AnchorColors.ink),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x3))
                Text(
                    text = formatDuration(app.totalMs),
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = AnchorColors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun WeeklyChart(week: List<DayBar>) {
    val maxMs = week.maxOf { it.totalMs }.coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height((8 + (72 * day.totalMs / maxMs).toInt()).dp)
                        .clip(RoundedCornerShape(Radius.track))
                        .background(if (day.isToday) AnchorColors.ink else AnchorColors.trackAlt),
                )
                Spacer(modifier = Modifier.height(Spacing.x2))
                Text(
                    text = day.label,
                    fontFamily = InterFamily,
                    fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = if (day.isToday) AnchorColors.textPrimary else AnchorColors.textMutedAlt,
                )
            }
        }
    }
}

@Composable
private fun PermissionEmptyState(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_perm_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = AnchorColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.wellbeing_perm_body),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = AnchorColors.textMuted,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            label = stringResource(R.string.wellbeing_perm_cta),
            onClick = onOpenSettings,
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
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_nodata_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = AnchorColors.textSecondary,
        )
        Text(
            text = stringResource(R.string.wellbeing_nodata_body),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = AnchorColors.textMuted,
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
