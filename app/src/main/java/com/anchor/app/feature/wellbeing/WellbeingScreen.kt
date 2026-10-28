package com.anchor.app.feature.wellbeing

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
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
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
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
            eyebrow = "ATTENTION RHYTHM",
            title = "Circadian Wellbeing",
            subtitle = "Siklus energi mental harian",
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
            CircadianArcCard()

            // --- 3. Peak Focus Window Card ---
            PeakFocusWindowCard()

            // --- 4. Nemo Recommendation Card ---
            NemoRecommendationCard(state = state)

            // --- 5. Dua Kartu Metrik (Stat Cards) ---
            StatCardsRow(state = state)
        }

        // --- 6. Sync Note ---
        Text(
            text = "Data diolah lokal via Android UsageStatsManager tanpa upload server.",
            fontFamily = ManropeFamily,
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

/** Data point for the 7 circadian timeline bars. */
private data class CircadianHour(
    val time: String,
    val tag: String,
    val targetPct: Float,
    val isPeak: Boolean = false,
    val minuteOfDay: Int,
)

@Composable
private fun CircadianArcCard() {
    val circadianHours = remember {
        listOf(
            CircadianHour(time = "07:00", tag = "WAKE", targetPct = 0.35f, isPeak = false, minuteOfDay = 7 * 60),
            CircadianHour(time = "09:30", tag = "PEAK", targetPct = 0.95f, isPeak = true, minuteOfDay = 9 * 60 + 30),
            CircadianHour(time = "11:30", tag = "HIGH", targetPct = 0.85f, isPeak = false, minuteOfDay = 11 * 60 + 30),
            CircadianHour(time = "13:30", tag = "DIP", targetPct = 0.40f, isPeak = false, minuteOfDay = 13 * 60 + 30),
            CircadianHour(time = "15:30", tag = "FOCUS", targetPct = 0.70f, isPeak = false, minuteOfDay = 15 * 60 + 30),
            CircadianHour(time = "18:00", tag = "EASE", targetPct = 0.50f, isPeak = false, minuteOfDay = 18 * 60),
            CircadianHour(time = "21:00", tag = "WIND", targetPct = 0.25f, isPeak = false, minuteOfDay = 21 * 60),
        )
    }

    val now = remember { LocalTime.now() }
    val nowMinutes = now.hour * 60 + now.minute
    val closestIndex = remember(nowMinutes) {
        circadianHours.indices.minByOrNull { kotlin.math.abs(circadianHours[it].minuteOfDay - nowMinutes) } ?: 1
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header baris: "Daily Focus Capacity" + Pill status "OPTIMAL"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Daily Focus Capacity",
                fontFamily = ManropeFamily,
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
                    text = "OPTIMAL",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Nemo.colors.onInk,
                )
            }
        }

        // 7 Circadian Bars + NOW Pin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                circadianHours.forEachIndexed { index, hour ->
                    val isCurrent = index == closestIndex
                    val isPeak = hour.isPeak

                    val targetHeight = (hour.targetPct * 84).dp
                    val animatedHeight by animateDpAsState(
                        targetValue = if (appeared) targetHeight else 8.dp,
                        animationSpec = Motion.enterSpec(),
                        label = "circadian-bar-$index",
                    )

                    Column(
                        modifier = Modifier.width(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        // NOW Pin if current
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Nemo.colors.ink)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "NOW",
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Nemo.colors.onInk,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Bar Track with Fill
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(84.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Nemo.colors.fog),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            val barColor = when {
                                isPeak -> Nemo.colors.ink
                                hour.targetPct >= 0.8f -> Nemo.colors.ink.copy(alpha = 0.85f)
                                hour.targetPct >= 0.6f -> Nemo.colors.ink.copy(alpha = 0.55f)
                                else -> Nemo.colors.ink.copy(alpha = 0.25f)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(animatedHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = hour.time,
                            fontFamily = ManropeFamily,
                            fontWeight = if (isPeak || isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            color = if (isPeak || isCurrent) Nemo.colors.ink else Nemo.colors.mute,
                        )

                        Text(
                            text = hour.tag,
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (isPeak || isCurrent) Nemo.colors.ink else Nemo.colors.placeholder,
                        )
                    }
                }
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
            text = "PEAK FOCUS WINDOW",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            color = Nemo.colors.mute,
        )
        Text(
            text = "09:00 - 11:30",
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = "Waktu terbaik untuk tugas berat (High/Urgent). Kognisi 40% lebih tajam.",
            fontFamily = ManropeFamily,
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
                    text = "NEMO INSIGHT",
                    fontFamily = ManropeFamily,
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
                    text = "ON-DEVICE",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        // Quote rekomendasi cerdas kontekstual dari data/state
        val quote = if (state.totalMs > 3 * 3600_000L) {
            "“Pickups spike post-lunch (+34%). Set a 10m buffer after lunch to prevent dopamine fatigue.”"
        } else {
            "“Ritme kognisi optimal tercapai pagi ini. Alokasikan tugas high-focus sebelum energi menurun.”"
        }

        Text(
            text = quote,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Nemo.colors.ink,
            lineHeight = 18.sp,
        )

        // Tombol/Cue aksi: "Jadwalkan tugas tersisa ke esok pagi ->"
        Text(
            text = "Jadwalkan tugas tersisa ke esok pagi ->",
            fontFamily = ManropeFamily,
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
        val screenTimeStr = if (state.totalMs > 0) formatDuration(state.totalMs) else "3h 42m"
        val screenTimeSub = state.vsYesterday?.let { cmp ->
            if (cmp.deltaMs < 0) "-${formatDuration(-cmp.deltaMs)} vs baseline"
            else "+${formatDuration(cmp.deltaMs)} vs baseline"
        } ?: "-35m vs baseline"

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
                text = "SCREEN TIME",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = screenTimeStr,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = screenTimeSub,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
        }

        // Stat Card 2: Focus Efficiency
        val efficiencyPct = if (state.distractionRatio != null) {
            "${((1f - state.distractionRatio) * 100).toInt()}%"
        } else {
            "82%"
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
                text = "FOCUS EFFICIENCY",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = efficiencyPct,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = "2.4 jam deep work",
                fontFamily = ManropeFamily,
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
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
        }
        Text(
            text = stringResource(R.string.wellbeing_perm_body),
            fontFamily = ManropeFamily,
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
            fontFamily = ManropeFamily,
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
