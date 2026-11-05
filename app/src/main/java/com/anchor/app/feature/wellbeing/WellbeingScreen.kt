package com.anchor.app.feature.wellbeing

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.anchor.app.R
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.components.NemoMascot
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSkeletonCard
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.usecase.circadian.CircadianCurveData
import com.anchor.app.core.domain.usecase.circadian.ComputeCircadianCurveUseCase
import com.anchor.app.platform.usagestats.UsageAccessSettings
import java.time.LocalTime

/**
 * 03 Digital Wellbeing (NEMO specs: QiGaq dark, QbeX4 light).
 *
 * Header: ATTENTION RHYTHM, Circadian Wellbeing, Siklus energi mental harian.
 * Circadian Arc Card: 7 circadian hour bars + NOW pin.
 * Peak Focus Window Card: 09:00 - 11:30.
 * Nemo Recommendation Card: on-device inference insight with sleepy mascot.
 * Stat Cards: Screen Time & Focus Efficiency.
 * Sync Note: UsageStatsManager local processing guarantee.
 */
@Composable
fun WellbeingScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: WellbeingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openUsageAccess: () -> Unit = { UsageAccessSettings.open(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        // --- 1. Header Block ---
        NemoScreenHeader(
            eyebrow = stringResource(R.string.wellbeing_header_eyebrow),
            title = stringResource(R.string.wellbeing_header_title),
            subtitle = stringResource(R.string.wellbeing_header_subtitle),
        )

        if (state.loading) {
            NemoSkeletonCard(height = 180.dp)
            NemoSkeletonCard(height = 80.dp)
            NemoSkeletonCard(height = 130.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NemoSkeletonCard(modifier = Modifier.weight(1f), height = 90.dp)
                NemoSkeletonCard(modifier = Modifier.weight(1f), height = 90.dp)
            }
        } else {
            // --- Permission Banner if missing ---
            if (state.availability == WellbeingAvailability.PERMISSION_MISSING) {
                PermissionCard(
                    onOpenSettings = openUsageAccess,
                    onOpenAppSettings = onOpenSettings,
                )
            }

            // --- 2. Circadian Arc Card ---
            AdaptiveCircadianCurveCard(
                curve = state.circadianCurve,
                onUpdateWakeTime = viewModel::onUpdateManualWakeTime,
            )

            // --- 3. Peak Focus Window Card ---
            PeakFocusWindowCard()

            // --- 4. Nemo Recommendation Card ---
            NemoRecommendationCard(state = state)

            // --- 5. Dua Kartu Metrik (Stat Cards) ---
            StatCardsRow(state = state)
        }

        // --- 6. Sync Note ---
        Text(
            text = stringResource(R.string.wellbeing_sync_note),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = Nemo.colors.mute,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.x2),
        )
    }
}

@Composable
private fun AdaptiveCircadianCurveCard(
    curve: CircadianCurveData?,
    onUpdateWakeTime: (hour: Int, minute: Int) -> Unit,
) {
    val context = LocalContext.current
    val wakeTime = curve?.wakeResult?.time ?: LocalTime.of(8, 0)
    val isManual = curve?.wakeResult?.isManual == true

    var touchedHoursFromWake by remember { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Header row: Title + Pill status ADAPTIF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.wellbeing_capacity_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Nemo.colors.ink,
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Nemo.colors.ink)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                )
                Text(
                    text = stringResource(R.string.wellbeing_status_adaptive),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Nemo.colors.onInk,
                )
            }
        }

        // 2. Interactive Sub-header (Wake Anchor)
        val wakeStr = String.format("%02d:%02d", wakeTime.hour, wakeTime.minute)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isManual) {
                    stringResource(R.string.wellbeing_wake_manual, wakeStr)
                } else {
                    stringResource(R.string.wellbeing_wake_auto, wakeStr)
                },
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .pressable(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute -> onUpdateWakeTime(hour, minute) },
                                wakeTime.hour,
                                wakeTime.minute,
                                true,
                            ).show()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stat_nemo),
                    contentDescription = stringResource(R.string.wellbeing_edit_wake_time),
                    tint = Nemo.colors.ink,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // Tooltip feedback when touching/dragging
        touchedHoursFromWake?.let { h ->
            val touchedMin = (h * 60f).toInt()
            val wallTime = wakeTime.plusMinutes(touchedMin.toLong())
            val cap = curve?.evaluate(h) ?: 0.5f
            val capPct = (cap * 100).toInt()
            val zone = ComputeCircadianCurveUseCase.resolveZoneLabel(h, cap)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Nemo.colors.fog)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = String.format("%02d:%02d • %d%% — %s", wallTime.hour, wallTime.minute, capPct, zone),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Nemo.colors.ink,
                )
            }
        }

        // 3. Curve Canvas (Height 110dp)
        val inkColor = Nemo.colors.ink
        val surfaceColor = Nemo.colors.surface
        val segments = curve?.segments ?: emptyList()
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                now = LocalTime.now()
            }
        }
        val nowMin = now.hour * 60 + now.minute
        val wakeMin = wakeTime.hour * 60 + wakeTime.minute
        val diffMin = (nowMin - wakeMin + 1440) % 1440
        val nowHours = diffMin / 60f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (size.width > 0) {
                                touchedHoursFromWake = (offset.x / size.width).coerceIn(0f, 1f) * 24f
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (size.width > 0) {
                                touchedHoursFromWake = (offset.x / size.width).coerceIn(0f, 1f) * 24f
                            }
                        },
                        onDrag = { change, _ ->
                            if (size.width > 0) {
                                touchedHoursFromWake = (change.position.x / size.width).coerceIn(0f, 1f) * 24f
                            }
                        },
                        onDragEnd = { touchedHoursFromWake = null },
                        onDragCancel = { touchedHoursFromWake = null },
                    )
                },
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val w = size.width
            val h = size.height
            val padTop = 16.dp.toPx()
            val padBottom = 16.dp.toPx()
            val effH = h - padTop - padBottom

            fun mapX(hours: Float): Float = (hours / 24f) * w
            fun mapY(capacity: Float): Float = h - padBottom - capacity * effH

            if (segments.isNotEmpty()) {
                val curvePath = Path().apply {
                    moveTo(mapX(segments[0].startX), mapY(segments[0].startY))
                    for (seg in segments) {
                        cubicTo(
                            mapX(seg.cp1X), mapY(seg.cp1Y),
                            mapX(seg.cp2X), mapY(seg.cp2Y),
                            mapX(seg.endX), mapY(seg.endY),
                        )
                    }
                }

                // Shading peak area (capacity >= 0.70)
                val fillPath = Path().apply {
                    addPath(curvePath)
                    lineTo(w, h - padBottom)
                    lineTo(0f, h - padBottom)
                    close()
                }

                val peakThresholdY = mapY(0.70f)
                clipRect(top = 0f, bottom = peakThresholdY, left = 0f, right = w) {
                    drawPath(fillPath, color = inkColor.copy(alpha = 0.10f))
                }

                // Curve line (2.5dp)
                drawPath(
                    curvePath,
                    color = inkColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // NOW pin
                val nowX = mapX(nowHours)
                val nowCap = curve?.evaluate(nowHours) ?: 0.5f
                val nowY = mapY(nowCap)

                // Dashed vertical line
                drawLine(
                    color = inkColor.copy(alpha = 0.5f),
                    start = Offset(nowX, 0f),
                    end = Offset(nowX, h - padBottom),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                )

                // Circle pin
                drawCircle(color = inkColor, radius = 4.dp.toPx(), center = Offset(nowX, nowY))
                drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = Offset(nowX, nowY))
            }
        }

        // 4. X-Axis 5 wall-clock labels
        val offsetsHours = listOf(0, 4, 8, 12, 18)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (off in offsetsHours) {
                val t = wakeTime.plusHours(off.toLong())
                val label = String.format("%02d:%02d", t.hour, t.minute)
                Text(
                    text = label,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 9.sp,
                    color = Nemo.colors.mute,
                )
            }
        }
    }
}

@Composable
private fun PeakFocusWindowCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.wellbeing_peak_window_title),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            color = Nemo.colors.mute,
        )
        Text(
            text = "09:00 - 11:30",
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = stringResource(R.string.wellbeing_peak_window_desc),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
    }
}

@Composable
private fun NemoRecommendationCard(state: WellbeingUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header: Tag "NEMO INSIGHT", Avatar NemoMascot(MascotExpression.SLEEPY, size = 32.dp), Pill "ON-DEVICE"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NemoMascot(
                    expression = MascotExpression.SLEEPY,
                    size = 32.dp,
                )
                Text(
                    text = stringResource(R.string.wellbeing_insight_tag),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = Nemo.colors.mute,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Nemo.colors.fog)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.wellbeing_on_device_pill),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        // Quote rekomendasi cerdas kontekstual dari data/state
        val quote = when {
            state.availability != WellbeingAvailability.AVAILABLE || state.totalMs == 0L ->
                stringResource(R.string.wellbeing_nodata_body)
            state.totalMs > 3 * 3600_000L ->
                stringResource(R.string.wellbeing_rec_quote)
            else ->
                stringResource(R.string.wellbeing_rec_quote_morning)
        }

        Text(
            text = quote,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Nemo.colors.ink,
            lineHeight = 18.sp,
        )

        // Tombol/Cue aksi: "Jadwalkan tugas tersisa ke esok pagi ->"
        Text(
            text = stringResource(R.string.wellbeing_action_schedule_tomorrow),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Nemo.colors.inkSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StatCardsRow(state: WellbeingUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Stat Card 1: Screen Time
        val hasData = state.availability == WellbeingAvailability.AVAILABLE && state.totalMs > 0
        val screenTimeStr = if (hasData) formatDuration(state.totalMs) else "—"
        val screenTimeSub = if (hasData) {
            state.vsYesterday?.let { cmp ->
                if (cmp.deltaMs < 0) stringResource(R.string.wellbeing_stat_vs_baseline_minus, formatDuration(-cmp.deltaMs))
                else stringResource(R.string.wellbeing_stat_vs_baseline_plus, formatDuration(cmp.deltaMs))
            } ?: stringResource(R.string.wellbeing_no_comparison)
        } else {
            stringResource(R.string.wellbeing_no_comparison)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.wellbeing_stat_screentime_lbl),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = screenTimeStr,
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = screenTimeSub,
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
        }

        // Stat Card 2: Focus Efficiency
        val hasEfficiency = state.availability == WellbeingAvailability.AVAILABLE && state.distractionRatio != null
        val efficiencyPct = if (hasEfficiency) {
            "${((1f - (state.distractionRatio ?: 0f)) * 100).toInt()}%"
        } else {
            "—"
        }
        val deepWorkSub = if (hasEfficiency && state.totalMs > 0) {
            val deepWorkHours = (state.totalMs * (1f - (state.distractionRatio ?: 0f))) / 3600_000f
            stringResource(R.string.wellbeing_stat_deepwork_sub, deepWorkHours)
        } else {
            "—"
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.wellbeing_stat_efficiency_lbl),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = efficiencyPct,
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = deepWorkSub,
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    onOpenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wellbeing),
                contentDescription = null,
                tint = Nemo.colors.ink,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.wellbeing_perm_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
        }
        Text(
            text = stringResource(R.string.wellbeing_perm_body),
            fontFamily = GeistFamily,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
        Spacer(modifier = Modifier.height(4.dp))
        PrimaryButton(
            label = stringResource(R.string.wellbeing_perm_cta),
            onClick = onOpenSettings,
        )
        Text(
            text = stringResource(R.string.wellbeing_perm_app_settings),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
            modifier = Modifier
                .clip(PillShape)
                .pressable(shape = PillShape, onClick = onOpenAppSettings)
                .padding(vertical = 4.dp),
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
