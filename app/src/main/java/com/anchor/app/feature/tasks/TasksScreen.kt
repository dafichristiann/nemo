package com.anchor.app.feature.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.components.NemoFloatingToast
import com.anchor.app.core.design.components.NemoMascot
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSkeletonRow
import com.anchor.app.core.design.components.NemoTaskRow
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
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
 * 01 Home Ã¢â‚¬â€ Smart Todo (docs/superpowers/design/07-screens.md Ã‚Â§2.5).
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
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val taskDoneMessage = stringResource(R.string.task_toast_done)
    val taskUndoLabel = stringResource(R.string.task_toast_undo)
    val taskCreatedMessage = stringResource(R.string.task_toast_created)

    var showAddSheet by remember { mutableStateOf(false) }
    var showOverdue by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var rescheduling by remember { mutableStateOf<TaskRowUi?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Nemo.colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                top = Spacing.x5,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.listGap),
        ) {
            item { HomeHeader(nickname = state.nickname, onOpenSettings = onOpenSettings) }

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
                    contentType = { "overdue_task" },
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
                val totalTasks = if (state.filter == TaskFilter.TODAY) {
                    state.todayCompletedCount + state.todayCount
                } else {
                    state.rows.size + state.overdueRows.size
                }
                SummaryCard(
                    completed = state.todayCompletedCount,
                    total = totalTasks,
                )
            }

            item {
                FilterSegmented(
                    selected = state.filter,
                    onSelect = viewModel::setFilter,
                )
            }

            if (state.isLoading && state.rows.isEmpty()) {
                items(3) {
                    NemoSkeletonRow()
                }
            } else if (state.rows.isEmpty()) {
                item {
                    EmptyState(
                        filter = state.filter,
                        todayCompletedCount = state.todayCompletedCount,
                        onViewDone = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    )
                }
            } else {
                items(
                    items = state.rows,
                    key = { it.task.id },
                    contentType = { "task_row" },
                ) { row ->
                    TaskCardRow(
                        row = row,
                        onClick = { onOpenTask(row.task.id) },
                        onToggleDone = {
                            viewModel.onToggleTaskDone(
                                taskId = row.task.id,
                                currentStatus = row.task.status,
                                doneMessage = taskDoneMessage,
                                undoLabel = taskUndoLabel,
                            )
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // Quick Add Bar and floating toast
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var activeToast by remember { mutableStateOf<TaskToast?>(null) }
            LaunchedEffect(toast) {
                if (toast != null) {
                    activeToast = toast
                }
            }

            AnimatedVisibility(
                visible = toast != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = Motion.standard, easing = Motion.standardEase),
                ) + fadeIn(
                    animationSpec = tween(durationMillis = Motion.standard, easing = Motion.standardEase),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = Motion.standard, easing = Motion.standardEase),
                ) + fadeOut(
                    animationSpec = tween(durationMillis = Motion.standard, easing = Motion.standardEase),
                ),
            ) {
                activeToast?.let { currentToast ->
                    NemoFloatingToast(
                        message = currentToast.message,
                        actionLabel = currentToast.actionLabel,
                        onActionClick = {
                            currentToast.taskId?.let { viewModel.undoToggleDone(it) }
                                ?: viewModel.dismissToast()
                        },
                        onDismiss = { viewModel.dismissToast() },
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.x2))

            // Quick Add Bar from gamabaran.pen ghQ1F floating above the bottom nav
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Nemo.colors.background.copy(alpha = 0.9f),
                                Nemo.colors.background,
                            ),
                        ),
                    )
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.x3),
            ) {
                QuickAddBar(onClick = { showAddSheet = true })
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            today = state.today,
            zone = state.zone,
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
                    createdMessage = taskCreatedMessage,
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

@Composable
private fun rememberGreeting(nickname: String, hour: Int): String {
    val name = nickname.trim()
    return if (name.isNotBlank()) {
        when (hour) {
            in 4..10 -> stringResource(R.string.home_greeting_morning_named, name)
            in 11..14 -> stringResource(R.string.home_greeting_afternoon_named, name)
            in 15..17 -> stringResource(R.string.home_greeting_sore_named, name)
            else -> stringResource(R.string.home_greeting_evening_named, name)
        }
    } else {
        when (hour) {
            in 4..10 -> stringResource(R.string.home_greeting_morning)
            in 11..14 -> stringResource(R.string.home_greeting_afternoon)
            in 15..17 -> stringResource(R.string.home_greeting_sore)
            else -> stringResource(R.string.home_greeting_evening)
        }
    }
}

/**
 * Clean screen header matching canvas ghQ1F (NHIyO, dHjZg):
 * Personalized greeting title + tune circle button. No week strip / date micro per user spec.
 */
@Composable
private fun HomeHeader(nickname: String, onOpenSettings: () -> Unit) {
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = rememberGreeting(nickname, currentHour)

    NemoScreenHeader(
        title = greeting,
        subtitle = stringResource(R.string.home_subtitle_plan),
        action = {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(42.dp)
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
        },
    )
}

/**
 * Refined progress summary matching canvas ghQ1F (VvWsG, F74OQ3, N9NRlq):
 * Editorial progress block with label, count, big percent, and 3dp hairline track.
 */
@Composable
private fun SummaryCard(completed: Int, total: Int) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    val percent = Math.round(fraction * 100f)
    val isAllComplete = completed == total && total > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.x1),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.home_progress_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = Nemo.colors.mute,
                )
                Text(
                    text = stringResource(R.string.home_progress_count, completed, total),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isAllComplete) {
                    NemoMascot(
                        expression = MascotExpression.LAUGHING,
                        size = 20.dp,
                    )
                }
                Text(
                    text = stringResource(R.string.home_progress_percent, percent),
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Nemo.colors.ink,
                )
            }
        }
        ProgressTrack(
            progress = fraction,
            trackHeight = 3.dp,
        )
    }
}

/**
 * Filter control matching canvas ghQ1F (aLfgJ):
 * Segmented pill container: Today | Upcoming | Chains | Done
 */
@Composable
private fun FilterSegmented(
    selected: TaskFilter,
    onSelect: (TaskFilter) -> Unit,
) {
    val tabs = listOf(
        TaskFilter.TODAY to stringResource(R.string.task_filter_today),
        TaskFilter.UPCOMING to stringResource(R.string.task_filter_upcoming),
        TaskFilter.OVERDUE to stringResource(R.string.tasks_tab_domino),
        TaskFilter.COMPLETED to stringResource(R.string.task_filter_done),
    )
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        val spacing = 3.dp
        val tabWidth = (maxWidth - spacing * (tabs.size - 1)) / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = (tabWidth + spacing) * selectedIndex,
            animationSpec = Motion.standardSpec(),
            label = "tab-indicator-offset",
        )

        // Single animated sliding indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Nemo.colors.fog),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { (filter, labelText) ->
                val isSelected = filter == selected
                val fg by animateColorAsState(
                    targetValue = if (isSelected) Nemo.colors.ink else Nemo.colors.mute,
                    animationSpec = Motion.standardSpec(),
                    label = "tab-fg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    onSelect(filter)
                                }
                            },
                        )
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelText,
                        fontFamily = GeistFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = fg,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Task card using NemoTaskRow:
 * Semantic Nemo.colors, NemoNode with spring and haptic feedback, priority pills, and time metadata.
 */
@Composable
private fun TaskCardRow(
    row: TaskRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDone: () -> Unit = {},
) {
    val isDone = row.task.isDone
    val isDomino = row.task.priority == TaskPriority.URGENT || row.overdue

    val subtitle = if (isDone) {
        row.metaLabel ?: stringResource(R.string.task_filter_completed)
    } else {
        null
    }

    val meta = if (!isDone) {
        row.metaLabel
    } else {
        null
    }

    NemoTaskRow(
        title = row.task.title,
        subtitle = subtitle,
        meta = meta,
        done = isDone,
        showNode = isDone,
        highlighted = isDomino && !isDone,
        onClick = onClick,
        onToggleDone = if (isDone) onToggleDone else null,
        modifier = modifier,
        trailing = {
            if (!isDone) {
                if (row.task.priority == TaskPriority.HIGH) {
                    NemoPill(text = stringResource(R.string.task_priority_high), filled = true)
                } else if (isDomino) {
                    NemoPill(text = stringResource(R.string.tasks_badge_domino), filled = true)
                }
            } else {
                NemoPill(text = stringResource(R.string.detail_status_completed), filled = false)
            }
        },
        content = {
            if (!isDone) {
                if (!row.deadlineLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Nemo.colors.fog)
                                .padding(horizontal = Spacing.x2, vertical = 2.dp),
                        ) {
                            Text(
                                text = row.deadlineLabel,
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = Nemo.colors.mute,
                            )
                        }
                    }
                }

                if (isDomino) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Nemo.colors.fog)
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tasks),
                            contentDescription = null,
                            tint = Nemo.colors.mute,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.tasks_unblocks_sequential),
                            fontFamily = GeistFamily,
                            fontSize = 11.sp,
                            color = Nemo.colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (row.task.progressPercent > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.tasks_progress_percent_format, row.task.progressPercent),
                            fontFamily = GeistMonoFamily,
                            fontSize = 11.sp,
                            color = Nemo.colors.mute,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val filledDots = when {
                                row.task.progressPercent >= 100 -> 3
                                row.task.progressPercent >= 66 -> 2
                                row.task.progressPercent >= 33 -> 1
                                else -> 0
                            }
                            for (i in 0..2) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 14.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (i < filledDots) Nemo.colors.ink else Nemo.colors.fog),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

/**
 * Quick Add Bar matching canvas ghQ1F (NcI1r):
 * 48dp height, 12dp radius, Plus icon, "Add a new intention...".
 */
@Composable
private fun QuickAddBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Nemo.colors.surface)
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(12.dp))
            .pressable(shape = RoundedCornerShape(12.dp), onClick = onClick)
            .padding(horizontal = Spacing.x4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = Nemo.colors.ink,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.tasks_quick_add_hint),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
        )
    }
}

/** Thin priority bar under a task title Ã¢â‚¬â€ soft tone, never a red block. */
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
    NemoPill(text = stringResource(labelRes), filled = priority == TaskPriority.HIGH)
}

@Composable
private fun OverdueBanner(count: Int, onReview: () -> Unit, onDismiss: () -> Unit) {
    val title = pluralStringResource(R.plurals.overdue_banner_title, count, count)
    Banner {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            NemoMascot(
                expression = MascotExpression.SAD,
                size = 36.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Nemo.colors.inkSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.x2))
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
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Nemo.colors.ink,
                        )
                    }
                    SmallAction(stringResource(R.string.overdue_action_dismiss), onDismiss)
                }
            }
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
        TaskCardRow(
            row = row,
            onClick = onOpen,
        )
        Row(
            modifier = Modifier.padding(start = Spacing.x2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            SmallAction(stringResource(R.string.overdue_action_reschedule), onReschedule)
            SmallAction(stringResource(R.string.overdue_action_keep), onKeep)
            // Completion now requires a focus session (PRD v0.5 Ã‚Â§10.1) Ã¢â‚¬â€ open the task to start one.
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
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Nemo.colors.inkSecondary,
        )
    }
}

@Composable
private fun EmptyState(
    filter: TaskFilter = TaskFilter.TODAY,
    todayCompletedCount: Int = 0,
    onViewDone: () -> Unit = {},
) {
    val isCompletedTab = filter == TaskFilter.COMPLETED
    val isTodayAllCaughtUp = filter == TaskFilter.TODAY && todayCompletedCount > 0

    val mascot = when {
        isCompletedTab -> MascotExpression.SLEEPY
        isTodayAllCaughtUp -> MascotExpression.HAPPY
        else -> MascotExpression.CONFUSED
    }
    val title = when {
        isCompletedTab -> stringResource(R.string.tasks_empty_completed_title)
        isTodayAllCaughtUp -> stringResource(R.string.tasks_all_done_title)
        else -> stringResource(R.string.home_empty_title)
    }
    val body = when {
        isCompletedTab -> stringResource(R.string.tasks_empty_completed_body)
        isTodayAllCaughtUp -> stringResource(R.string.tasks_all_done_body, todayCompletedCount)
        else -> stringResource(R.string.home_empty_body)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        NemoMascot(
            expression = mascot,
            size = 96.dp,
            contentDescription = title,
        )
        Text(
            text = title,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Nemo.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Nemo.colors.mute,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.x4),
        )

        if (isTodayAllCaughtUp) {
            Spacer(modifier = Modifier.height(Spacing.x2))
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Nemo.colors.surface)
                    .border(1.dp, Nemo.colors.line, PillShape)
                    .pressable(shape = PillShape, onClick = onViewDone)
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tasks),
                        contentDescription = null,
                        tint = Nemo.colors.ink,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.tasks_all_done_action),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Nemo.colors.ink,
                    )
                }
            }
        }
    }
}

/** Priority Ã¢â€ â€™ soft left-bar color. Never a glowing red (06-design-system.md Ã‚Â§2.3). */
@Composable
private fun TaskPriority.barColor(): Color = when (this) {
    TaskPriority.URGENT -> Nemo.colors.inkSecondary
    TaskPriority.HIGH -> Nemo.colors.inkSecondary
    TaskPriority.MEDIUM -> Nemo.colors.mute
    TaskPriority.LOW -> Nemo.colors.line
}

/**
 * Interactive round checkbox Ã¢â‚¬â€ kept for the subtask list on the Task Detail screen
 * (docs 07-screens.md Ã‚Â§2.6). The Home list itself uses the passive [TimelineNode], since a task
 * is completed only by finishing a full Focus session (PRD v0.5 Ã‚Â§10.1).
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
