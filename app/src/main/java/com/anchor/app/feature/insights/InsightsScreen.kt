package com.anchor.app.feature.insights

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.components.NemoMascot
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSkeletonCard
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter
import kotlin.math.absoluteValue

/**
 * Screen 08 — Insights / Daily Reflection (Jurnal Refleksi Harian).
 *
 * Horizontal Pager Carousel Deck with micro-animations and monochrome Nemo Day/Night aesthetic:
 * 1. Card 1 (The Daily Pulse / Ritme Hari): 3 key metrics, animated counters, visual focus vs screen ratio bar,
 *    and longest distraction session.
 * 2. Card 2 (Nemo's Observation / Catatan Sintesis): Chibi Nemo mascot with breathing aura and editorial serif quote.
 * 3. Card 3 (Tomorrow's Anchor / Saran & Pola): Gentle suggestion with serene Nemo, plus recurring pattern chips.
 */
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x5),
    ) {
        // 1. Header
        Box(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
            NemoScreenHeader(
                eyebrow = stringResource(R.string.insights_header_eyebrow),
                title = stringResource(R.string.insights_title),
                subtitle = state.dateLabel?.let {
                    "${it.format(JAVA_DATE)} — ${stringResource(R.string.insights_day_label)}"
                },
            )
        }

        // Empty state check
        if (!state.loading && !state.hasAnyContent) {
            Box(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
                EmptyState()
            }
            return@Column
        }

        // Loading state
        if (state.loading) {
            Box(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    NemoSkeletonCard(height = 380.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 60.dp, height = 6.dp)
                            .clip(PillShape)
                            .background(Nemo.colors.line),
                    )
                }
            }
            return@Column
        }

        // 2. Carousel Pager
        val pagerState = rememberPagerState(pageCount = { 3 })

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .graphicsLayer {
                        // Dynamic page transition (scale 1.0f -> 0.93f, alpha 1.0f -> 0.70f)
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceIn(0f, 1f)
                        val scale = 1.0f - (1.0f - 0.93f) * pageOffset
                        scaleX = scale
                        scaleY = scale
                        alpha = 1.0f - (1.0f - 0.70f) * pageOffset
                    }
                    .clip(RoundedCornerShape(Radius.card))
                    .background(Nemo.colors.fog)
                    .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                    .padding(Spacing.x4),
            ) {
                when (page) {
                    0 -> DailyPulseCard(state = state)
                    1 -> NemosObservationCard(state = state)
                    2 -> TomorrowsAnchorCard(state = state)
                }
            }
        }

        // 4. Page Dot Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.x1),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                val isSelected = pagerState.currentPage == index
                val targetWidth = if (isSelected) 22.dp else 6.dp
                val animatedWidth by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = tween(durationMillis = Motion.standard, easing = Motion.standardEase),
                    label = "pager_dot_width_$index",
                )
                val dotColor = if (isSelected) Nemo.colors.ink else Nemo.colors.line

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(animatedWidth)
                        .clip(PillShape)
                        .background(dotColor)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x2))

        // 5. Footer (Honest attribution & Refresh button)
        val attributionText = state.insight?.let {
            if (viewModel.insightSourceIsAi(it)) {
                stringResource(R.string.insights_source_ai)
            } else {
                stringResource(R.string.insights_source_rule)
            }
        } ?: stringResource(R.string.insights_attribution)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Text(
                text = attributionText,
                fontFamily = GeistFamily,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.insights_refresh),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Nemo.colors.inkSecondary,
                modifier = Modifier
                    .clip(PillShape)
                    .pressable(shape = PillShape, onClick = viewModel::refresh)
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
            )
        }
    }
}

/**
 * Kartu 1: The Daily Pulse / Ritme Hari.
 * 3 metrik utama dengan transisi angka halus, rasio visual bar fokus vs layar, dan sesi distraksi terlama.
 */
@Composable
private fun DailyPulseCard(state: InsightsUiState) {
    val animatedTasks by animateFloatAsState(
        targetValue = state.tasksCompleted.toFloat(),
        animationSpec = tween(durationMillis = Motion.progress, easing = Motion.standardEase),
        label = "anim_tasks_pulse",
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CARD 01",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = "THE DAILY PULSE",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.inkSecondary,
            )
        }

        // 3 Key Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(16.dp))
                .padding(vertical = Spacing.x3, horizontal = Spacing.x2),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${animatedTasks.toInt()}/${state.tasksPlanned}",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.insights_s1_lbl),
                    fontFamily = GeistFamily,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Nemo.colors.line),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.focusTimeMs > 0) formatDuration(state.focusTimeMs) else "0m",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.insights_s2_lbl),
                    fontFamily = GeistFamily,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Nemo.colors.line),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.screenTimeMs?.let { formatDuration(it) } ?: "—",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.insights_s3_lbl),
                    fontFamily = GeistFamily,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        // Rasio Visual Bar Fokus vs Layar
        val focusMs = state.focusTimeMs.coerceAtLeast(0L)
        val screenMs = (state.screenTimeMs ?: 0L).coerceAtLeast(0L)
        val totalMs = focusMs + screenMs

        val targetRatio = when {
            totalMs > 0L -> (focusMs.toFloat() / totalMs.toFloat()).coerceIn(0.04f, 0.96f)
            focusMs > 0L -> 1f
            else -> 0.5f
        }

        val animatedRatio by animateFloatAsState(
            targetValue = targetRatio,
            animationSpec = tween(durationMillis = Motion.progress, easing = Motion.standardEase),
            label = "anim_focus_screen_ratio",
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Focus vs Screen Ratio",
                    fontFamily = GeistFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Nemo.colors.inkSecondary,
                )
                val focusPercent = (animatedRatio * 100).toInt()
                Text(
                    text = if (state.screenTimeMs != null) "$focusPercent% Focus" else "Focus active",
                    fontFamily = GeistMonoFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Nemo.colors.ink,
                )
            }

            // Visual dual-segmented bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(PillShape)
                    .background(Nemo.colors.line),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatio)
                        .clip(PillShape)
                        .background(Nemo.colors.ink),
                )
            }
        }

        // Sesi Distraksi Terlama
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
                .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_distraction_session),
                fontFamily = GeistFamily,
                fontSize = 12.sp,
                color = Nemo.colors.inkSecondary,
            )
            val longestMs = state.longestDistractionMs
            val distractionText = if (longestMs != null && longestMs > 0) {
                val label = state.longestDistractionLabel ?: stringResource(R.string.insights_distraction_session)
                "$label · ${formatDuration(longestMs)}"
            } else {
                "—"
            }
            Text(
                text = distractionText,
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Nemo.colors.ink,
            )
        }
    }
}

/**
 * Kartu 2: Nemo's Observation / Catatan Sintesis.
 * Maskot Nemo di tengah dengan animasi Breathing Aura diiringi kutipan sintesis editorial serif Fraunces.
 */
@Suppress("DEPRECATION")
@Composable
private fun NemosObservationCard(state: InsightsUiState) {
    val auraTransition = rememberInfiniteTransition(label = "nemo_breathing_aura")
    val auraScale by auraTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = Motion.standardEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_scale",
    )
    val auraAlpha by auraTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = Motion.standardEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_alpha",
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CARD 02",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = "NEMO'S OBSERVATION",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.inkSecondary,
            )
        }

        // Center Mascot with Breathing Aura
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = auraScale
                        scaleY = auraScale
                        alpha = auraAlpha
                    }
                    .clip(CircleShape)
                    .background(Nemo.colors.ink),
            )
            NemoMascot(
                expression = MascotExpression.TALKING,
                size = 70.dp,
            )
        }

        // Editorial Serif Fraunces Quote
        val summaryText = state.insight?.summary?.ifEmpty { null }
            ?: stringResource(R.string.insights_summary_fallback)

        Text(
            text = "“$summaryText”",
            fontFamily = FrauncesFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            color = Nemo.colors.ink,
            modifier = Modifier.padding(horizontal = Spacing.x2),
        )

        // Footer Tag
        Text(
            text = stringResource(R.string.insights_tag_synthesis),
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            letterSpacing = 1.sp,
            color = Nemo.colors.mute,
        )
    }
}

/**
 * Kartu 3: Tomorrow's Anchor / Saran & Pola.
 * Saran lembut hari esok ditemani Nemo tenang, dan daftar pola berulang sebagai chip elegan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TomorrowsAnchorCard(state: InsightsUiState) {
    val suggestionText = state.insight?.suggestions?.firstOrNull()?.ifEmpty { null }
        ?: stringResource(R.string.insights_suggestion_fallback)
    val patterns = state.insight?.patterns ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CARD 03",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Nemo.colors.mute,
            )
            Text(
                text = "TOMORROW'S ANCHOR",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.inkSecondary,
            )
        }

        // Gentle suggestion with serene Nemo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(16.dp))
                .padding(Spacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            NemoMascot(
                expression = MascotExpression.NEUTRAL,
                size = 38.dp,
            )
            Text(
                text = "“$suggestionText”",
                fontFamily = GeistFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Nemo.colors.ink,
                modifier = Modifier.weight(1f),
            )
        }

        // Recurring patterns section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            Text(
                text = stringResource(R.string.insights_pattern_header),
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.mute,
            )

            if (patterns.isEmpty()) {
                Text(
                    text = stringResource(R.string.insights_no_patterns),
                    fontFamily = GeistFamily,
                    fontSize = 12.sp,
                    color = Nemo.colors.placeholder,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    patterns.take(4).forEach { pattern ->
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(Nemo.colors.surface)
                                .border(1.dp, Nemo.colors.line, PillShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = pattern,
                                fontFamily = GeistFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Nemo.colors.inkSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        NemoMascot(
            expression = MascotExpression.SLEEPY,
            size = 110.dp,
        )
        Text(
            text = stringResource(R.string.insights_empty_title),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Nemo.colors.inkSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.insights_empty_body),
            fontFamily = GeistFamily,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            textAlign = TextAlign.Center,
        )
    }
}

private val JAVA_DATE = JavaDateTimeFormatter.ofPattern("MMMM d")

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
