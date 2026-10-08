package com.anchor.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.AddTaskFab
import com.anchor.app.core.design.components.Banner
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.TextPill
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorElevation
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.LocalAnchorElevation
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.usecase.task.TaskFilter
import java.time.LocalDate
import java.time.LocalTime

/**
 * 01 Home — Smart Todo (docs/superpowers/design/07-screens.md §2.5).
 *
 * Greeting + date, today's progress card, All/Today/Upcoming/Completed tabs, a calm
 * overdue banner (never red, FR-T11), the task list, and the add-task FAB.
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

    Box(modifier = modifier.fillMaxSize().background(AnchorColors.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = Spacing.x6,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.listGap),
            ) {
                item { HomeHeader(onOpenSettings = onOpenSettings) }

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
                        items = state.rows.filter { it.overdue },
                        key = { "overdue-${it.task.id}" },
                    ) { row ->
                        OverdueTaskRow(
                            row = row,
                            onComplete = { viewModel.onComplete(row.task.id) },
                            onKeep = { viewModel.onKeepForToday(row.task.id) },
                            onReschedule = { rescheduling = row },
                        )
                    }
                }

                item {
                    ProgressCard(
                        completed = state.rows.count { it.task.isDone },
                        total = state.rows.size,
                    )
                }

                item { FilterTabs(selected = state.filter, onSelect = viewModel::setFilter) }

                if (state.rows.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(items = state.rows, key = { it.task.id }) { row ->
                        TaskListItem(
                            row = row,
                            onClick = { onOpenTask(row.task.id) },
                            onComplete = { viewModel.onComplete(row.task.id) },
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

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> stringResource(R.string.home_greeting_morning)
        hour < 18 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }
    val today = LocalDate.now()
    val dateLabel = "${today.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }}, " +
        "${today.month.name.take(3).replaceFirstChar { it.uppercase() }} ${today.dayOfMonth}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(
                text = greeting,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = AnchorColors.textPrimary,
            )
            Text(
                text = dateLabel,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = AnchorColors.textMuted,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.settings_open),
            tint = AnchorColors.textMutedAlt,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onOpenSettings),
        )
    }
}

@Composable
private fun ProgressCard(completed: Int, total: Int) {
    val elevation: AnchorElevation = LocalAnchorElevation.current
    val percent = if (total == 0) 0 else (completed * 100f / total).toInt()
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
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_progress_title),
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AnchorColors.textSecondary,
            )
            Text(
                text = stringResource(R.string.home_progress_percent, percent),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AnchorColors.textPrimary,
            )
        }
        ProgressTrack(progress = if (total == 0) 0f else completed.toFloat() / total)
        Text(
            text = stringResource(R.string.home_progress_count, completed, total),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = AnchorColors.textMuted,
        )
    }
}

@Composable
private fun FilterTabs(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    val tabs = listOf(
        TaskFilter.ALL to R.string.task_filter_all,
        TaskFilter.TODAY to R.string.task_filter_today,
        TaskFilter.UPCOMING to R.string.task_filter_upcoming,
        TaskFilter.COMPLETED to R.string.task_filter_completed,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x6),
    ) {
        tabs.forEach { (filter, labelRes) ->
            val isSelected = filter == selected
            Column(
                modifier = Modifier.clickable { onSelect(filter) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                Text(
                    text = stringResource(labelRes),
                    fontFamily = InterFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isSelected) AnchorColors.ink else AnchorColors.textMutedAlt,
                )
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (isSelected) 24.dp else 0.dp)
                        .background(AnchorColors.ink),
                )
            }
        }
    }
}

@Composable
private fun TaskListItem(row: TaskRowUi, onClick: () -> Unit, onComplete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundCheckbox(checked = row.task.isDone, onToggle = onComplete)
        Spacer(modifier = Modifier.width(Spacing.x3))
        com.anchor.app.core.design.components.TaskCard(
            title = row.task.title,
            subtitle = row.deadlineLabel,
            meta = row.metaLabel,
            modifier = Modifier.weight(1f),
            priorityBarColor = row.task.priority.barColor(),
            trailing = { PriorityPill(row.task.priority) },
        )
    }
}

@Composable
internal fun RoundCheckbox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = if (checked) AnchorColors.ink else AnchorColors.border,
                shape = CircleShape,
            )
            .background(if (checked) AnchorColors.ink else Color.Transparent)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = AnchorColors.surface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PriorityPill(priority: TaskPriority) {
    val labelRes = when (priority) {
        TaskPriority.LOW -> R.string.task_priority_low
        TaskPriority.MEDIUM -> R.string.task_priority_medium
        TaskPriority.HIGH -> R.string.task_priority_high
        TaskPriority.URGENT -> R.string.task_priority_urgent
    }
    TextPill(text = stringResource(labelRes), highlighted = priority == TaskPriority.HIGH)
}

@Composable
private fun OverdueBanner(count: Int, onReview: () -> Unit, onDismiss: () -> Unit) {
    val title = androidx.compose.ui.res.pluralStringResource(R.plurals.overdue_banner_title, count, count)
    Banner {
        Text(
            text = title,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = AnchorColors.textStrongAlt,
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .border(1.dp, AnchorColors.border, RoundedCornerShape(Radius.chip))
                    .clickable(onClick = onReview)
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.overdue_banner_review),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = AnchorColors.textPrimary,
                )
            }
            SmallAction(stringResource(R.string.overdue_action_dismiss), onDismiss)
        }
    }
}

@Composable
private fun OverdueTaskRow(
    row: TaskRowUi,
    onComplete: () -> Unit,
    onKeep: () -> Unit,
    onReschedule: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        TaskListItem(row = row, onClick = {}, onComplete = onComplete)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SmallAction(stringResource(R.string.overdue_action_reschedule), onReschedule)
            SmallAction(stringResource(R.string.overdue_action_keep), onKeep)
            SmallAction(stringResource(R.string.overdue_action_complete), onComplete)
        }
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.chip))
            .background(AnchorColors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = AnchorColors.textSecondary,
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
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = AnchorColors.textSecondary,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = AnchorColors.textMuted,
        )
    }
}

/** Priority → soft left-bar color. Never a glowing red (06-design-system.md §2.3). */
private fun TaskPriority.barColor(): Color = when (this) {
    TaskPriority.URGENT -> AnchorColors.textStrongAlt
    TaskPriority.HIGH -> AnchorColors.textSecondary
    TaskPriority.MEDIUM -> AnchorColors.textMutedAlt
    TaskPriority.LOW -> AnchorColors.border
}

@Preview(showBackground = true)
@Composable
private fun ProgressCardPreview() {
    AnchorTheme { ProgressCard(completed = 1, total = 3) }
}
