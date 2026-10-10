package com.anchor.app.feature.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.common.DateTimeFormatter
import com.anchor.app.core.design.components.AddTaskFab
import com.anchor.app.core.design.components.Banner
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.minTouchTarget
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.usecase.task.TaskFilter
import java.time.LocalDate
import java.time.LocalTime

/**
 * 01 Home â€” Smart Todo (docs/superpowers/design/07-screens.md Â§2.5).
 *
 * Layout refreshed to a calmer, editorial "planner" look (monochrome, high-contrast):
 *  - an oversized greeting header with a compact week strip (today highlighted);
 *  - a soft segmented filter control;
 *  - a refined progress summary;
 *  - the task list rendered as a **timeline** (status node + connector, time on the right),
 *    so the day reads like a schedule rather than a stack of cards.
 *
 * All data still comes from [TasksViewModel]; only presentation changed.
 */
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    onOpenTask: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var showOverdue by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var rescheduling by remember { mutableStateOf<TaskRowUi?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Nemo.colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = Spacing.x5,
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.listGap),
            ) {
                item { HomeHeader(state = state, onOpenSettings = onOpenSettings) }

                if (state.overdueCount > 0 && !bannerDismissed) {
                    item {
                        OverdueBanner(
                            count = state.overdueCount,
                            onReview = { showOverdue = !showOverdue },
                            onDismiss = { bannerDismissed = true },
                        )
                    }
                }

                if (showOverdue && state.overdueCount > 0) {
                    items(
                        items = state.overdueRows,
                        key = { "overdue-${it.task.id}" },
                    ) { row ->
                        OverdueTaskRow(
                            row = row,
                            onOpen = { onOpenTask(row.task.id) },
                            onKeep = { viewModel.onKeepForToday(row.task.id) },
                            onReschedule = { rescheduling = row },
                        )
                    }
                }

                item {
                    // Derive the summary once per relevant change instead of scanning the list
                    // inside the `item {}` lambda on every recomposition.
                    val completedCount = remember(state.rows) { state.rows.count { it.task.isDone } }
                    SummaryCard(
                        completed = completedCount,
                        total = state.rows.size + state.overdueRows.size,
                    )
                }

                item { FilterSegmented(selected = state.filter, onSelect = viewModel::setFilter) }

                if (state.rows.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(items = state.rows, key = { it.task.id }) { row ->
                        TimelineTaskRow(
                            row = row,
                            onClick = { onOpenTask(row.task.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        AddTaskFab(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.screenHorizontal, bottom = Spacing.x6),
        )
    }

    if (showAddSheet) {
        AddTaskSheet(
            onDismiss = { showAddSheet = false },
            onSave = { draft, onDone ->
                viewModel.onCreateTask(
                    title = draft.title,
                    description = draft.description,
                    deadline = draft.deadline,
                    priority = draft.priority,
                    estimatedDurationMin = draft.estimatedDurationMinutes,
                    category = draft.category,
                    subtaskTitles = draft.subtasks,
                    recurrenceRule = draft.recurrence,
                ) { result ->
                    if (result is com.anchor.app.core.domain.usecase.task.CreateTaskResult.Success) {
                        showAddSheet = false
                    }
                    onDone(result)
                }
            },
        )
    }

    rescheduling?.let { row ->
        RescheduleSheet(
            today = state.today,
            zone = state.zone,
            currentDeadline = row.task.deadline,
            onDismiss = { rescheduling = null },
            onConfirm = { newDeadline ->
                viewModel.onReschedule(row.task.id, newDeadline)
                rescheduling = null
            },
        )
    }
}

/**
 * Oversized greeting + a compact week strip. Today is highlighted with an ink pill, the way a
 * planner marks the current day â€” quiet but confident.
 */
@Composable
private fun HomeHeader(state: TasksUiState, onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val nickname = state.nickname
    val greeting = if (nickname.isBlank()) {
        when {
            hour < 12 -> stringResource(R.string.home_greeting_morning)
            hour < 18 -> stringResource(R.string.home_greeting_afternoon)
            else -> stringResource(R.string.home_greeting_evening)
        }
    } else {
        when {
            hour < 12 -> stringResource(R.string.home_greeting_morning_named, nickname)
            hour < 18 -> stringResource(R.string.home_greeting_afternoon_named, nickname)
            else -> stringResource(R.string.home_greeting_evening_named, nickname)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                Text(
                    text = greeting,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 30.sp,
                    letterSpacing = (-0.5).sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.home_subtitle_plan),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Nemo.colors.mute,
                )
            }
            Box(
                modifier = Modifier
                    .minTouchTarget()
                    .clip(CircleShape)
                    .border(1.dp, Nemo.colors.line, CircleShape)
                    .pressable(shape = CircleShape, onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = stringResource(R.string.settings_open),
                    tint = Nemo.colors.inkSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        WeekStrip(today = state.today)
    }
}

/**
 * Compact 7-day strip (Mon..Sun of today's week). Today gets an ink pill; every other day is a
 * quiet numeral. Purely a date reference for now â€” the list itself is filtered by the tabs below.
 */
@Composable
private fun WeekStrip(today: LocalDate) {
    // Remembered on `today`: the 7-day window only changes when the date does, not every recompose.
    val days = remember(today) {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { date ->
            val isToday = date == today
            val bg by animateColorAsState(
                targetValue = if (isToday) Nemo.colors.ink else Color.Transparent,
                animationSpec = Motion.standardSpec(),
                label = "day-bg",
            )
            val fg = if (isToday) Nemo.colors.onInk else Nemo.colors.mute
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(bg)
                    .padding(vertical = Spacing.x2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                Text(
                    text = date.dayOfWeekLabel(),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = fg,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    fontFamily = ManropeFamily,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = fg,
                )
            }
        }
    }
}

/** "M","T",â€¦ single-letter weekday index for the compact strip. */
private fun LocalDate.dayOfWeekLabel(): String = dayOfWeek.name.take(1)

/**
 * Refined progress summary: a large percentage, the count, and the hairline track. Reads as a
 * quiet stat block rather than a boxed card.
 */
@Composable
private fun SummaryCard(completed: Int, total: Int) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    val percent = Math.round(fraction * 100f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = Motion.standardSpec(),
        label = "summary-progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Nemo.colors.surface)
            .padding(horizontal = Spacing.cardHorizontal, vertical = Spacing.x5),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                Text(
                    text = stringResource(R.string.home_progress_title),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Nemo.colors.mute,
                )
                Text(
                    text = stringResource(R.string.home_progress_count, completed, total),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                )
            }
            Text(
                text = stringResource(R.string.home_progress_percent, percent),
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Light,
                fontSize = 34.sp,
                letterSpacing = (-1).sp,
                color = Nemo.colors.ink,
            )
        }
        // Hairline track with a smooth sweep.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(Radius.track))
                .background(Nemo.colors.fog),
        ) {
            if (animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(Radius.track))
                        .background(Nemo.colors.ink),
                )
            }
        }
    }
}

/**
 * Segmented filter control: a soft track holding equal-width segments. The active segment gets
 * an ink pill that slides/animates â€” a cleaner alternative to the previous underline tabs.
 */
@Composable
private fun FilterSegmented(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    val tabs = listOf(
        TaskFilter.TODAY to R.string.task_filter_today,
        TaskFilter.UPCOMING to R.string.task_filter_upcoming,
        TaskFilter.OVERDUE to R.string.task_filter_overdue,
        TaskFilter.COMPLETED to R.string.task_filter_completed,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(Nemo.colors.fog)
            .horizontalScroll(rememberScrollState())
            .padding(Spacing.x1),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        tabs.forEach { (filter, labelRes) ->
            val isSelected = filter == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) Nemo.colors.ink else Color.Transparent,
                animationSpec = Motion.standardSpec(),
                label = "seg-bg",
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) Nemo.colors.onInk else Nemo.colors.mute,
                animationSpec = Motion.standardSpec(),
                label = "seg-fg",
            )
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(bg)
                    .pressable(
                        shape = PillShape,
                        pressedScale = 0.97f,
                        onClick = { onSelect(filter) },
                    )
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    fontFamily = ManropeFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = fg,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * A task rendered as a timeline row: a status node + vertical connector on the left, the title
 * and meta in the middle, and the deadline time on the right. The node fills when the task is
 * done; a thin priority bar sits under the title to signal importance without shouting.
 */
@Composable
private fun TimelineTaskRow(
    row: TaskRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .pressable(shape = RoundedCornerShape(Radius.card), pressedScale = 0.99f, onClick = onClick)
            .padding(vertical = Spacing.x2),
        verticalAlignment = Alignment.Top,
    ) {
        TimelineNode(done = row.task.isDone)
        Spacer(modifier = Modifier.width(Spacing.x4))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = row.task.title,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (row.task.isDone) Nemo.colors.mute else Nemo.colors.ink,
                textDecoration = if (row.task.isDone) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val metaLine = listOfNotNull(row.metaLabel, row.deadlineLabel).joinToString("  Â·  ")
            if (metaLine.isNotBlank()) {
                Text(
                    text = metaLine,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.x1))
            PriorityBar(priority = row.task.priority)
        }
        Spacer(modifier = Modifier.width(Spacing.x3))
        PriorityPill(priority = row.task.priority)
    }
}

/** The circular status node and the connector stub below it. */
@Composable
private fun TimelineNode(done: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (done) Nemo.colors.ink else Nemo.colors.line,
                    shape = CircleShape,
                )
                .background(if (done) Nemo.colors.ink else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_tasks),
                    contentDescription = null,
                    tint = Nemo.colors.onInk,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        // Connector stub â€” implies the timeline even on the last row, subtly.
        Box(
            modifier = Modifier
                .padding(top = Spacing.x1)
                .width(1.5.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(Radius.track))
                .background(Nemo.colors.line),
        )
    }
}

/** Thin priority bar under a task title â€” soft tone, never a red block. */
@Composable
private fun PriorityBar(priority: TaskPriority) {
    val width by animateDpAsState(
        targetValue = when (priority) {
            TaskPriority.URGENT -> 40.dp
            TaskPriority.HIGH -> 28.dp
            TaskPriority.MEDIUM -> 18.dp
            TaskPriority.LOW -> 10.dp
        },
        animationSpec = Motion.standardSpec(),
        label = "priority-bar",
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(3.dp)
            .clip(RoundedCornerShape(Radius.track))
            .background(priority.barColor()),
    )
}

@Composable
private fun PriorityPill(priority: TaskPriority) {
    val labelRes = when (priority) {
        TaskPriority.LOW -> R.string.task_priority_low
        TaskPriority.MEDIUM -> R.string.task_priority_medium
        TaskPriority.HIGH -> R.string.task_priority_high
        TaskPriority.URGENT -> R.string.task_priority_urgent
    }
    NemoPill(text = stringResource(labelRes), highlighted = priority == TaskPriority.HIGH)
}

@Composable
private fun OverdueBanner(count: Int, onReview: () -> Unit, onDismiss: () -> Unit) {
    val title = pluralStringResource(R.plurals.overdue_banner_title, count, count)
    Banner {
        Text(
            text = title,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Nemo.colors.inkSecondary,
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .border(1.dp, Nemo.colors.line, PillShape)
                    .pressable(shape = PillShape, onClick = onReview)
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.overdue_banner_review),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Nemo.colors.ink,
                )
            }
            SmallAction(stringResource(R.string.overdue_action_dismiss), onDismiss)
        }
    }
}

@Composable
private fun OverdueTaskRow(
    row: TaskRowUi,
    onOpen: () -> Unit,
    onKeep: () -> Unit,
    onReschedule: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        TimelineTaskRow(row = row, onClick = onOpen)
        Row(
            modifier = Modifier.padding(start = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            SmallAction(stringResource(R.string.overdue_action_reschedule), onReschedule)
            SmallAction(stringResource(R.string.overdue_action_keep), onKeep)
            // Completion now requires a focus session (PRD v0.5 Â§10.1) â€” open the task to start one.
            SmallAction(stringResource(R.string.overdue_action_open), onOpen)
        }
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(Nemo.colors.fog)
            .pressable(shape = PillShape, onClick = onClick)
            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Nemo.colors.inkSecondary,
        )
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(1.dp, Nemo.colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tasks),
                contentDescription = null,
                tint = Nemo.colors.mute,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_empty_title),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Nemo.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            textAlign = TextAlign.Center,
        )
    }
}

/** Priority â†’ soft left-bar color. Never a glowing red (06-design-system.md Â§2.3). */
@Composable
private fun TaskPriority.barColor(): Color = when (this) {
    TaskPriority.URGENT -> Nemo.colors.inkSecondary
    TaskPriority.HIGH -> Nemo.colors.inkSecondary
    TaskPriority.MEDIUM -> Nemo.colors.mute
    TaskPriority.LOW -> Nemo.colors.line
}

/**
 * Interactive round checkbox â€” kept for the subtask list on the Task Detail screen
 * (docs 07-screens.md Â§2.6). The Home list itself uses the passive [TimelineNode], since a task
 * is completed only by finishing a full Focus session (PRD v0.5 Â§10.1).
 */
@Composable
internal fun RoundCheckbox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = if (checked) Nemo.colors.ink else Nemo.colors.line,
                shape = CircleShape,
            )
            .background(if (checked) Nemo.colors.ink else Color.Transparent)
            .pressable(shape = CircleShape, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_tasks),
                contentDescription = null,
                tint = Nemo.colors.onInk,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardPreview() {
    NemoTheme { SummaryCard(completed = 2, total = 5) }
}
