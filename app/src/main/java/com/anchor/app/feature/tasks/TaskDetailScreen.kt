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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.NemoButton
import com.anchor.app.core.design.components.NemoButtonStyle
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.NemoSkeletonCard
import com.anchor.app.core.design.components.NemoSkeletonRow
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.nemoShimmer
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.TaskPriority

/**
 * 10 Task Detail (docs/superpowers/design/07-screens.md §2.14).
 *
 * Title, badges, meta, progress track, subtasks (check/order), a Future
 * View callout, and actions: Start Focus, Reschedule, Mark Done.
 */
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onStartFocusToComplete: (String) -> Unit = {},
    onOpenFutureView: (String) -> Unit = {},
    onEditTask: ((String) -> Unit)? = null,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }
    var newSubtask by remember { mutableStateOf("") }
    var showReschedule by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val task = state.task

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        if (state.isLoading && task == null) {
            // Header baris back/edit skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(PillShape)
                        .pressable(
                            shape = PillShape,
                            pressedScale = 0.96f,
                            onClick = onBack,
                        )
                        .nemoShimmer(),
                )
                if (onEditTask != null) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(24.dp)
                            .clip(PillShape)
                            .nemoShimmer(),
                    )
                }
            }

            // Baris judul skeleton
            NemoSkeletonCard(height = 36.dp)

            // Dua pill badge skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                NemoSkeletonCard(modifier = Modifier.weight(1f), height = 24.dp)
                NemoSkeletonCard(modifier = Modifier.weight(1f), height = 24.dp)
            }

            // Meta info skeleton
            NemoSkeletonCard(height = 40.dp)

            // 2 baris NemoSkeletonRow() untuk placeholder subtasks/steps
            NemoSkeletonRow()
            NemoSkeletonRow()

            return@Column
        }

        // Header: back + Edit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                )
            }
            if (onEditTask != null && task != null) {
                Text(
                    text = stringResource(R.string.detail_edit),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                    modifier = Modifier
                        .clip(PillShape)
                        .pressable(shape = PillShape, onClick = { onEditTask(task.id) })
                        .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
                )
            }
        }

        if (task == null) {
            Text(
                text = stringResource(R.string.home_empty_title),
                fontFamily = GeistFamily,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
            )
            return@Column
        }

        // Title
        Text(
            text = task.title,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.3).sp,
            color = Nemo.colors.ink,
        )

        // Status & Priority Badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NemoPill(
                text = priorityLabel(task.priority),
                filled = task.priority == TaskPriority.HIGH || task.priority == TaskPriority.URGENT,
            )
            NemoPill(
                text = stringResource(
                    if (task.isDone) R.string.detail_status_completed else R.string.detail_status_in_progress
                ),
                filled = task.isDone,
            )
        }

        // Meta info (Deadline, Duration, Progress)
        val cleanEstimated = remember(state.estimatedLabel) {
            state.estimatedLabel?.replace(" est", "")?.trim()
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.deadlineLabel?.let {
                    Text(
                        text = it,
                        fontFamily = GeistMonoFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.mute,
                    )
                }
                if (state.deadlineLabel != null && cleanEstimated != null) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Nemo.colors.mute),
                    )
                }
                cleanEstimated?.let {
                    Text(
                        text = stringResource(R.string.detail_meta_estimated, it),
                        fontFamily = GeistMonoFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.mute,
                    )
                }
            }

            if (state.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.x1))
                Text(
                    text = stringResource(
                        R.string.detail_meta_progress,
                        state.progressPercent,
                        state.doneSubtaskCount,
                        state.subtasks.size,
                    ),
                    fontFamily = GeistMonoFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.mute,
                )
                ProgressTrack(progress = state.progressPercent / 100f)
            }
        }

        Divider()

        // Subtasks / Steps
        SectionHeader(title = stringResource(R.string.detail_steps_header))
        if (state.subtasks.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_subtasks),
                fontFamily = GeistFamily,
                fontSize = 13.sp,
                color = Nemo.colors.placeholder,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                val orderedSubtasks = remember(state.subtasks) {
                    state.subtasks.sortedBy { it.orderIndex }
                }
                val firstIncompleteIndex = orderedSubtasks.indexOfFirst { !it.isDone }
                orderedSubtasks.forEachIndexed { idx, sub ->
                    val isActive = idx == firstIncompleteIndex && !sub.isDone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.card))
                            .background(if (isActive) Nemo.colors.fog else Nemo.colors.surface)
                            .border(
                                width = 1.dp,
                                color = if (isActive) Nemo.colors.line else Color.Transparent,
                                shape = RoundedCornerShape(Radius.card),
                            )
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x2 + 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundCheckbox(checked = sub.isDone, onToggle = { viewModel.onToggleSubtask(sub) })
                        Spacer(modifier = Modifier.width(Spacing.x3))
                        Text(
                            text = if (isActive) "${sub.title} ${stringResource(R.string.detail_active_step_tag)}" else sub.title,
                            fontFamily = GeistFamily,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (sub.isDone) Nemo.colors.mute else Nemo.colors.ink,
                            textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Quick add subtask (integrated modern input pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(Radius.field))
                .background(Nemo.colors.fog)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.field))
                .padding(start = Spacing.x3, end = Spacing.x1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Spacing.x1),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (newSubtask.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_task_subtask_hint),
                        fontFamily = GeistFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.placeholder,
                    )
                }
                BasicTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = GeistFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.ink,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (newSubtask.isNotBlank()) Nemo.colors.ink else Color.Transparent)
                    .clickable(enabled = newSubtask.isNotBlank()) {
                        viewModel.onAddSubtask(newSubtask)
                        newSubtask = ""
                    }
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.detail_add_subtask),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (newSubtask.isNotBlank()) Nemo.colors.onInk else Nemo.colors.mute,
                )
            }
        }

        // Future View callout (07-screens.md §2.14; opens screen 07)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                .pressable(
                    shape = RoundedCornerShape(Radius.card),
                    pressedScale = 0.98f,
                    onClick = { onOpenFutureView(task.id) },
                )
                .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            com.anchor.app.core.design.components.NemoMascot(
                expression = com.anchor.app.core.design.components.MascotExpression.THINKING,
                size = 34.dp,
            )
            Text(
                text = stringResource(R.string.detail_future_callout),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Nemo.colors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                tint = Nemo.colors.mute,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = 180f },
            )
        }

        // Actions: Primary Focus CTA, Secondary Mark Done, plus centered bottom actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.x2),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            val isDone = task.isDone
            NemoButton(
                text = stringResource(R.string.detail_action_complete_via_focus),
                onClick = { onStartFocusToComplete(task.id) },
                modifier = Modifier.fillMaxWidth(),
                style = if (isDone) NemoButtonStyle.Secondary else NemoButtonStyle.Primary,
            )

            val doneLabel = if (isDone) {
                stringResource(R.string.detail_action_todo)
            } else {
                stringResource(R.string.detail_action_complete)
            }
            NemoButton(
                text = doneLabel,
                onClick = { viewModel.onToggleDone() },
                modifier = Modifier.fillMaxWidth(),
                style = if (isDone) NemoButtonStyle.Primary else NemoButtonStyle.Secondary,
            )

            // Quiet secondary actions centered
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.x1),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryAction(stringResource(R.string.detail_action_reschedule)) {
                    showReschedule = true
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.x3)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Nemo.colors.mute),
                )
                SecondaryAction(stringResource(R.string.detail_action_delete), danger = true) {
                    showDeleteConfirmDialog = true
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Nemo.colors.surface,
            title = {
                Text(
                    text = stringResource(R.string.task_delete_confirm_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Nemo.colors.ink,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.task_delete_confirm_body),
                    fontFamily = GeistFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.mute,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.onDelete()
                        onBack()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_confirm_action),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        color = Nemo.colors.danger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Medium,
                        color = Nemo.colors.mute,
                    )
                }
            },
        )
    }

    if (showReschedule) {
        RescheduleSheet(
            today = state.today,
            zone = state.zone,
            currentDeadline = task?.deadline,
            onDismiss = { showReschedule = false },
            onConfirm = { newDeadline ->
                viewModel.onReschedule(newDeadline)
                showReschedule = false
            },
        )
    }
}

/**
 * A quiet, text-only secondary action (design-spec.md §7 #6): "Reschedule" is neutral text
 * and "Delete Task" is danger-coloured **text** (no bordered pill), so destructive actions
 * read as calm links rather than loud buttons.
 */
@Composable
private fun SecondaryAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .pressable(shape = PillShape, onClick = onClick)
            .padding(horizontal = Spacing.x3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (danger) Nemo.colors.danger else Nemo.colors.inkSecondary,
        )
    }
}

@Composable
private fun priorityLabel(priority: TaskPriority): String = stringResource(
    when (priority) {
        TaskPriority.LOW -> R.string.task_priority_low
        TaskPriority.MEDIUM -> R.string.task_priority_medium
        TaskPriority.HIGH -> R.string.task_priority_high
        TaskPriority.URGENT -> R.string.task_priority_urgent
    },
)
