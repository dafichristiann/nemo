package com.anchor.app.feature.futureview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.DominoEffect
import com.anchor.app.core.domain.model.FutureScenario
import com.anchor.app.core.domain.model.FutureView
import com.anchor.app.core.domain.model.Task
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen 07 — Future View + Domino Effect (docs/superpowers/design/07-screens.md §2.11,
 * PRD §10.6-10.7).
 *
 * Two side-by-side scenarios (start now vs keep delaying) with an honest disclaimer and any
 * heuristic assumptions clearly marked, plus the dependency chain in estimative language
 * ("may affect"). The primary action is [Start Focus] (FR-FV4).
 */
@Composable
fun FutureViewScreen(
    onBack: () -> Unit,
    onStartFocus: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FutureViewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cycleWarning by viewModel.cycleMessage.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        Row(
            modifier = Modifier
                .clip(PillShape)
                .pressable(
                    shape = PillShape,
                    pressedScale = 0.96f,
                    onClick = onBack,
                )
                .padding(vertical = Spacing.x1, horizontal = Spacing.x1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.detail_back),
                tint = Nemo.colors.ink,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.detail_back),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
        }

        Text(
            text = stringResource(R.string.future_title),
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = stringResource(R.string.future_disclaimer),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
        )

        val view = state.futureView
        if (view != null) {
            ScenarioCard(
                label = stringResource(R.string.future_scenario_now),
                scenario = view.startNow,
                zone = state.zone,
            )
            ScenarioCard(
                label = stringResource(R.string.future_scenario_delay),
                scenario = view.delay,
                zone = state.zone,
            )

            if (view.usingAssumedDuration || view.usingAssumedDeadline) {
                Notice(text = stringResource(R.string.future_assumptions))
            }
        }

        state.domino?.let { DominoSection(it, onOpenTask) }

        DominoLinkSection(
            candidates = state.linkCandidates,
            cycleWarning = cycleWarning,
            onLink = { viewModel.onAddDependency(it) },
        )

        val taskId = state.task?.id
        PrimaryButton(
            label = stringResource(R.string.future_start_focus),
            onClick = { taskId?.let(onStartFocus) },
        )
    }
}

@Composable
private fun ScenarioCard(label: String, scenario: FutureScenario, zone: ZoneId) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.fog)
            .padding(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = stringResource(
                R.string.future_scenario_range,
                formatTime(scenario.startAt, zone),
                formatTime(scenario.endAt, zone),
            ),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
        )
        scenario.deadlineAt?.let {
            Text(
                text = stringResource(R.string.future_scenario_deadline, formatTime(it, zone)),
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                color = Nemo.colors.placeholder,
            )
        }
    }
}

@Composable
private fun DominoSection(domino: DominoEffect, onOpenTask: (String) -> Unit) {
    SectionHeader(title = stringResource(R.string.future_domino_title))

    if (domino.affectedCount == 0) {
        Text(
            text = stringResource(R.string.future_domino_empty),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.placeholder,
        )
        return
    }

    // Estimative count — never deterministic (FR-DE4).
    Text(
        text = stringResource(R.string.future_domino_count, domino.affectedCount),
        fontFamily = ManropeFamily,
        fontSize = 13.sp,
        color = Nemo.colors.inkSecondary,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.fog)
            .padding(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        DominoRow(
            title = domino.root.title,
            tag = stringResource(R.string.future_domino_current),
            onClick = { onOpenTask(domino.root.taskId) },
        )
        domino.affected.forEach { node ->
            Text(
                text = "↓",
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                color = Nemo.colors.placeholder,
                modifier = Modifier.padding(start = Spacing.x2),
            )
            DominoRow(
                title = node.title,
                tag = null,
                onClick = { onOpenTask(node.taskId) },
            )
        }
    }
}

@Composable
private fun DominoRow(title: String, tag: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.field))
            .pressable(shape = RoundedCornerShape(Radius.field), onClick = onClick)
            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Text(
            text = title,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Nemo.colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (tag != null) {
            Text(
                text = tag,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = Nemo.colors.placeholder,
            )
        }
    }
}

/**
 * Lets the user link this task to another (PRD §10.7 FR-DE1). Each candidate is a tappable
 * row; a rejected cycle shows a calm, non-alarming notice (never a hard error).
 */
@Composable
private fun DominoLinkSection(
    candidates: List<Task>,
    cycleWarning: Boolean,
    onLink: (String) -> Unit,
) {
    SectionHeader(title = stringResource(R.string.future_domino_add_label))

    if (cycleWarning) {
        Notice(text = stringResource(R.string.future_domino_cycle_warning))
    }

    if (candidates.isEmpty()) {
        Text(
            text = stringResource(R.string.future_domino_none_available),
            fontFamily = ManropeFamily,
            fontSize = 13.sp,
            color = Nemo.colors.placeholder,
        )
        return
    }

    Text(
        text = stringResource(R.string.future_domino_pick_hint),
        fontFamily = ManropeFamily,
        fontSize = 12.sp,
        color = Nemo.colors.mute,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
        candidates.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.field))
                    .background(Nemo.colors.fog)
                    .pressable(shape = RoundedCornerShape(Radius.field), onClick = { onLink(task.id) })
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.future_domino_add_button),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.fog)
            .padding(Spacing.x4),
    ) {
        Text(
            text = text,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
        )
    }
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(TIME_FORMAT)
