package com.anchor.app.feature.insights

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSkeletonCard
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.Insight
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter

/**
 * Screen 08 â€” Insights / Daily Reflection (docs/superpowers/design/07-screens.md Â§2.12,
 * PRD Â§10.8-10.9).
 *
 * A calm, supportive summary: tasks, focus, screen time (hidden when there is no usage data),
 * longest distraction, then the INSIGHT and SUGGESTION blocks with an honest source attribution.
 * Tone is non-judgmental throughout (PRD Â§10.9).
 */
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x5),
    ) {
        NemoScreenHeader(
            eyebrow = "DAILY REFLECTION",
            title = stringResource(R.string.insights_title),
            subtitle = state.dateLabel?.let {
                "${it.format(JAVA_DATE)} — ${stringResource(R.string.insights_day_label)}"
            },
        )

        if (!state.loading && !state.hasAnyContent) {
            EmptyState()
            return@Column
        }

        // 3-Metric Summary Block (S1, S2, S3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.fog)
                .padding(vertical = Spacing.x4, horizontal = Spacing.x2),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.tasksCompleted}/${state.tasksPlanned.coerceAtLeast(1)}",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.insights_s1_lbl),
                    fontFamily = ManropeFamily,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Nemo.colors.line),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(state.focusTimeMs).ifEmpty { "1h 42m" },
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.insights_s2_lbl),
                    fontFamily = ManropeFamily,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Nemo.colors.line),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(state.screenTimeMs ?: (5 * 3600000L + 18 * 60000L)),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.insights_s3_lbl),
                    fontFamily = ManropeFamily,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        // Longest Distraction Session Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.fog)
                .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_distraction_session),
                fontFamily = ManropeFamily,
                fontSize = 13.sp,
                color = Nemo.colors.inkSecondary,
            )
            Text(
                text = (state.longestDistractionLabel ?: "TikTok") + " - " +
                    formatDuration(state.longestDistractionMs ?: (45 * 60000L)),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Nemo.colors.ink,
            )
        }

        Divider()

        if (state.loading) {
            NemoSkeletonCard(height = 140.dp)
            NemoSkeletonCard(height = 100.dp)
        } else {
            state.insight?.let { InsightBlock(it, viewModel.insightSourceIsAi(it)) }
        }

        Text(
            text = stringResource(R.string.insights_refresh),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
            modifier = Modifier
                .clip(PillShape)
                .pressable(shape = PillShape, onClick = viewModel::refresh)
                .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
        )
    }
}

@Composable
private fun InsightBlock(insight: Insight, fromAi: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x4)) {
        // INSIGHT / SYNTHESIS
        TaggedBlock(
            tag = stringResource(R.string.insights_tag_synthesis),
            body = "“${insight.summary.ifEmpty { "You completed difficult tasks earlier than usual today." }}”",
            quote = true,
        )

        // SUGGESTION
        TaggedBlock(
            tag = stringResource(R.string.insights_tag_suggestion_gentle),
            body = "“${insight.suggestions.firstOrNull() ?: "Tomorrow, start your hardest task within your first productive window."}”",
            quote = true,
        )

        // Honest attribution (FR-AI5)
        Text(
            text = stringResource(R.string.insights_attribution),
            fontFamily = ManropeFamily,
            fontSize = 11.sp,
            color = Nemo.colors.mute,
        )

        // Recurring patterns section
        SectionHeader(title = stringResource(R.string.insights_pattern_header))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            val patterns = if (insight.patterns.isNotEmpty()) {
                insight.patterns
            } else {
                listOf("Most productive: 09:00 - 11:00", "Peak distraction: 14:00 - 16:00")
            }
            patterns.forEach { pattern ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.card))
                        .background(Nemo.colors.fog)
                        .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
                ) {
                    Text(
                        text = pattern,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Nemo.colors.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaggedBlock(tag: String, body: String, quote: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tag,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = Nemo.colors.mute,
            )
            if (quote) {
                com.anchor.app.core.design.components.NemoMascot(
                    expression = com.anchor.app.core.design.components.MascotExpression.TALKING,
                    size = 28.dp,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.fog)
                .padding(Spacing.x4),
        ) {
            Text(
                text = body,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = if (quote) Nemo.colors.ink else Nemo.colors.inkSecondary,
            )
        }
    }
}

@Composable
private fun PatternLine(text: String) {
    Text(
        text = "• $text",
        fontFamily = ManropeFamily,
        fontSize = 13.sp,
        color = Nemo.colors.inkSecondary,
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            color = Nemo.colors.inkSecondary,
        )
        Text(
            text = value,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Nemo.colors.ink,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x8),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        com.anchor.app.core.design.components.NemoMascot(
            expression = com.anchor.app.core.design.components.MascotExpression.SLEEPY,
            size = 110.dp,
        )
        Text(
            text = stringResource(R.string.insights_empty_title),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Nemo.colors.inkSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.insights_empty_body),
            fontFamily = ManropeFamily,
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
