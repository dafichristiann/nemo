package com.anchor.app.feature.futureview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.components.NemoMascot
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.NemoSkeletonCard
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.DominoEffect
import com.anchor.app.core.domain.model.FutureScenario
import com.anchor.app.core.domain.model.Task
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen 07 — Future View + Domino Effect (docs/superpowers/design/07-screens.md §2.11,
 * PRD §10.6-10.7).
 *
 * Clean projection of two scenarios (Start Now vs Keep Delaying) with honest disclaimers
 * and visible heuristic indicators. The domino section visually connects dependent tasks
 * in estimative language ("may affect").
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
        // Top Bar: Back & Projection Pill Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(PillShape)
                    .pressable(
                        shape = PillShape,
                        pressedScale = 0.96f,
                        onClick = onBack,
                    )
                    .padding(vertical = Spacing.x2, horizontal = Spacing.x1),
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
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Nemo.colors.fog)
                    .padding(horizontal = Spacing.x3, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.future_pill_tag),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }
        }

        // Header Title and Honest Disclaimer
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(
                text = stringResource(R.string.future_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.3).sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = stringResource(
                    R.string.future_disclaimer_formatted,
                    state.task?.title ?: stringResource(R.string.future_default_task_title),
                ),
                fontFamily = GeistFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Nemo.colors.inkSecondary,
            )
        }

        if (state.loading) {
            NemoSkeletonCard(height = 140.dp)
            NemoSkeletonCard(height = 140.dp)
            NemoSkeletonCard(height = 96.dp)
            NemoSkeletonCard(height = 54.dp)
        } else if (state.task == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(Nemo.colors.fog)
                    .padding(Spacing.x6),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_empty_title),
                    fontFamily = GeistFamily,
                    fontSize = 14.sp,
                    color = Nemo.colors.mute,
                )
            }
        } else {
            val view = state.futureView
            if (view != null) {
                ScenarioCard(
                    label = stringResource(R.string.future_scenario_now),
                    scenario = view.startNow,
                    zone = state.zone,
                    emphasis = true,
                )
                ScenarioCard(
                    label = stringResource(R.string.future_scenario_delay),
                    scenario = view.delay,
                    zone = state.zone,
                    emphasis = false,
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
            val duration = view?.estimatedDurationMin?.takeIf { it > 0 } ?: 45
            PrimaryButton(
                label = stringResource(R.string.future_start_focus_duration, duration),
                onClick = { taskId?.let(onStartFocus) },
                enabled = taskId != null,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))
        }
    }
}

/**
 * A scenario card showing projected timeline outcomes with Fraunces/Geist numerals.
 * Start Now uses an inverted dark container (`ink`) with Happy Nemo.
 * Keep Delaying uses a quiet framed neutral container (`fog`) with Angry Nemo.
 */
@Composable
private fun ScenarioCard(
    label: String,
    scenario: FutureScenario,
    zone: ZoneId,
    emphasis: Boolean = false,
) {
    val container = if (emphasis) Nemo.colors.ink else Nemo.colors.fog
    val titleColor = if (emphasis) Nemo.colors.onInk else Nemo.colors.mute
    val valueColor = if (emphasis) Nemo.colors.onInk else Nemo.colors.ink
    val metaColor = if (emphasis) Nemo.colors.onInk.copy(alpha = 0.75f) else Nemo.colors.inkSecondary
    val chipText = if (emphasis) {
        stringResource(R.string.future_scenario_chip_free)
    } else {
        stringResource(R.string.future_scenario_chip_stress)
    }
    val chipBg = if (emphasis) Nemo.colors.surface.copy(alpha = 0.2f) else Nemo.colors.surface
    val chipTextColor = if (emphasis) Nemo.colors.onInk else Nemo.colors.inkSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(container)
            .then(
                if (!emphasis) Modifier.border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                else Modifier
            )
            .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (emphasis) Nemo.colors.onInk else Nemo.colors.inkSecondary),
                )
                Text(
                    text = label,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = titleColor,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                NemoMascot(
                    expression = if (emphasis) MascotExpression.HAPPY else MascotExpression.ANGRY,
                    size = 28.dp,
                )
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(chipBg)
                        .padding(horizontal = Spacing.x2 + 2.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = chipText,
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = chipTextColor,
                    )
                }
            }
        }

        Text(
            text = stringResource(
                R.string.future_scenario_range,
                formatTime(scenario.startAt, zone),
                formatTime(scenario.endAt, zone),
            ),
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
            color = valueColor,
        )

        scenario.deadlineAt?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                Text(
                    text = stringResource(R.string.future_scenario_deadline, formatTime(it, zone)),
                    fontFamily = GeistFamily,
                    fontSize = 12.sp,
                    color = metaColor,
                )
            }
        }
    }
}

@Composable
private fun DominoSection(domino: DominoEffect, onOpenTask: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(R.string.future_domino_header))
            if (domino.affectedCount > 0) {
                Text(
                    text = stringResource(R.string.future_domino_count_label, domino.affectedCount),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        if (domino.affectedCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(Nemo.colors.fog)
                    .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x4),
            ) {
                Text(
                    text = stringResource(R.string.future_domino_empty),
                    fontFamily = GeistFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.placeholder,
                )
            }
            return
        }

        Text(
            text = stringResource(R.string.future_domino_count, domino.affectedCount),
            fontFamily = GeistFamily,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.fog)
                .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x4),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Root task node (CURRENT)
            DominoTimelineItem(
                title = domino.root.title,
                tag = stringResource(R.string.future_domino_current),
                isRoot = true,
                isLast = domino.affected.isEmpty(),
                onClick = { onOpenTask(domino.root.taskId) },
            )

            // Affected downstream nodes
            domino.affected.forEachIndexed { index, node ->
                DominoTimelineItem(
                    title = node.title,
                    tag = stringResource(R.string.future_buffer_tag, "-40m"),
                    isRoot = false,
                    isLast = index == domino.affected.lastIndex,
                    onClick = { onOpenTask(node.taskId) },
                )
            }
        }
    }
}

@Composable
private fun DominoTimelineItem(
    title: String,
    tag: String,
    isRoot: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(Radius.field))
            .pressable(
                shape = RoundedCornerShape(Radius.field),
                pressedScale = 0.98f,
                onClick = onClick,
            )
            .padding(vertical = Spacing.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .then(
                        if (isRoot) {
                            Modifier.background(Nemo.colors.ink)
                        } else {
                            Modifier
                                .background(Nemo.colors.background)
                                .border(2.dp, Nemo.colors.mute, CircleShape)
                        }
                    ),
            )
        }

        Text(
            text = title,
            fontFamily = GeistFamily,
            fontWeight = if (isRoot) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
            color = Nemo.colors.ink,
            modifier = Modifier.weight(1f),
        )

        NemoPill(
            text = tag,
            filled = isRoot,
        )
    }

    if (!isLast) {
        Box(
            modifier = Modifier
                .padding(start = 7.dp)
                .width(2.dp)
                .height(14.dp)
                .background(Nemo.colors.line),
        )
    }
}

/**
 * Lets the user link this task to another (PRD §10.7 FR-DE1). Each candidate is a tappable
 * row with 48dp+ hit target; a rejected cycle shows Confused Nemo in a gentle, non-alarming notice.
 */
@Composable
private fun DominoLinkSection(
    candidates: List<Task>,
    cycleWarning: Boolean,
    onLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        SectionHeader(title = stringResource(R.string.future_domino_add_label))

        if (cycleWarning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(Nemo.colors.fog)
                    .padding(Spacing.x4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                NemoMascot(MascotExpression.CONFUSED, size = 32.dp)
                Text(
                    text = stringResource(R.string.future_domino_cycle_warning),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Nemo.colors.ink,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (candidates.isEmpty()) {
            Text(
                text = stringResource(R.string.future_domino_none_available),
                fontFamily = GeistFamily,
                fontSize = 13.sp,
                color = Nemo.colors.placeholder,
            )
            return
        }

        Text(
            text = stringResource(R.string.future_domino_pick_hint),
            fontFamily = GeistFamily,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            candidates.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(RoundedCornerShape(Radius.field))
                        .background(Nemo.colors.fog)
                        .pressable(
                            shape = RoundedCornerShape(Radius.field),
                            pressedScale = 0.98f,
                            onClick = { onLink(task.id) },
                        )
                        .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    Text(
                        text = task.title,
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Nemo.colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "+",
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Nemo.colors.inkSecondary,
                        )
                        Text(
                            text = stringResource(R.string.future_domino_add_button),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Nemo.colors.inkSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.fog)
            .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        NemoMascot(
            expression = MascotExpression.THINKING,
            size = 26.dp,
        )
        Text(
            text = text,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = Nemo.colors.inkSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(TIME_FORMAT)
