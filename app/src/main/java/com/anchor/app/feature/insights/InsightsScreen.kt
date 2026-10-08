package com.anchor.app.feature.insights

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.Insight
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter

/**
 * Screen 08 — Insights / Daily Reflection (docs/superpowers/design/07-screens.md §2.12,
 * PRD §10.8-10.9).
 *
 * A calm, supportive summary: tasks, focus, screen time (hidden when there is no usage data),
 * longest distraction, then the INSIGHT and SUGGESTION blocks with an honest source attribution.
 * Tone is non-judgmental throughout (PRD §10.9).
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
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x5),
    ) {
        Text(
            text = stringResource(R.string.insights_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AnchorColors.textPrimary,
        )
        state.dateLabel?.let {
            Text(
                text = it.format(JAVA_DATE) + " — " + stringResource(R.string.insights_day_label),
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = AnchorColors.textMuted,
            )
        }

        if (!state.loading && !state.hasAnyContent) {
            EmptyState()
            return@Column
        }

        SectionHeader(title = stringResource(R.string.insights_your_day))

        // Stats: Tasks + Focus (always shown) ; Screen time only when data exists.
        StatRow(
            label = stringResource(R.string.insights_stat_tasks),
            value = stringResource(R.string.insights_stat_tasks_value, state.tasksCompleted, state.tasksPlanned),
        )
        StatRow(
            label = stringResource(R.string.insights_stat_focus),
            value = formatDuration(state.focusTimeMs),
        )
        state.screenTimeMs?.let {
            StatRow(
                label = stringResource(R.string.insights_stat_screen_time),
                value = formatDuration(it),
            )
        }
        state.longestDistractionMs?.let { ms ->
            StatRow(
                label = stringResource(R.string.insights_longest_distraction),
                value = (state.longestDistractionLabel ?: "") + " · " + formatDuration(ms),
            )
        }

        Divider()

        state.insight?.let { InsightBlock(it, viewModel.insightSourceIsAi(it)) }

        Text(
            text = stringResource(R.string.insights_refresh),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
            modifier = Modifier
                .clickable(onClick = viewModel::refresh)
                .padding(vertical = Spacing.x2),
        )
    }
}

@Composable
private fun InsightBlock(insight: Insight, fromAi: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
        // INSIGHT
        TaggedBlock(
            tag = stringResource(R.string.insights_tag_insight),
            body = insight.summary,
        )

        if (insight.patterns.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.insights_pattern_label))
            insight.patterns.forEach { PatternLine(it) }
        }

        // SUGGESTION (one realistic next step, FR-DR4)
        insight.suggestions.firstOrNull()?.let { suggestion ->
            TaggedBlock(
                tag = stringResource(R.string.insights_tag_suggestion),
                body = suggestion,
            )
        }

        // Honest attribution (FR-AI5)
        Text(
            text = stringResource(
                if (fromAi) R.string.insights_source_ai else R.string.insights_source_rule,
            ),
            fontFamily = InterFamily,
            fontSize = 11.sp,
            color = AnchorColors.textFaint,
        )
    }
}

@Composable
private fun TaggedBlock(tag: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
        Text(
            text = tag,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = AnchorColors.textFaint,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(AnchorColors.surfaceSunken)
                .padding(Spacing.x4),
        ) {
            Text(
                text = body,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AnchorColors.textStrongAlt,
            )
        }
    }
}

@Composable
private fun PatternLine(text: String) {
    Text(
        text = "· $text",
        fontFamily = InterFamily,
        fontSize = 13.sp,
        color = AnchorColors.textSecondary,
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
            fontFamily = InterFamily,
            fontSize = 14.sp,
            color = AnchorColors.textSecondary,
        )
        Text(
            text = value,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = AnchorColors.textPrimary,
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
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Text(
            text = stringResource(R.string.insights_empty_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = AnchorColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.insights_empty_body),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = AnchorColors.textMuted,
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
