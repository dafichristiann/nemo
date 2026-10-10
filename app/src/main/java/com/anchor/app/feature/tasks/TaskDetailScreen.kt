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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.TaskPriority

/**
 * 10 Task Detail (docs/superpowers/design/07-screens.md Ã‚Â§2.14).
 *
 * Priority bar, title, badges, meta, progress track, subtasks (check/order), a Future
 * View callout (wired in Sprint 6), and the actions: Start Focus, Reschedule, Mark Done.
 */
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onStartFocusToComplete: (String) -> Unit = {},
    onOpenFutureView: (String) -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newSubtask by remember { mutableStateOf("") }
    var showReschedule by remember { mutableStateOf(false) }
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
        // Header: back + title
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

        if (task == null) {
            Text(
                text = stringResource(R.string.home_empty_title),
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
            )
            return@Column
        }

        // Priority bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(Radius.track))
                .background(Nemo.colors.ink),
        )

        Text(
            text = task.title,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            color = Nemo.colors.ink,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            NemoPill(text = priorityLabel(task.priority), filled = task.priority == TaskPriority.HIGH)
            NemoPill(text = stringResource(R.string.detail_status_in_progress))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            state.deadlineLabel?.let {
                Text(it, fontFamily = ManropeFamily, fontSize = 13.sp, color = Nemo.colors.mute)
            }
            state.estimatedLabel?.let {
                Text(
                    text = stringResource(R.string.detail_meta_estimated, it),
                    fontFamily = ManropeFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.mute,
                )
            }
            if (state.subtasks.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.detail_meta_progress,
                        state.progressPercent,
                        state.doneSubtaskCount,
                        state.subtasks.size,
                    ),
                    fontFamily = ManropeFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        ProgressTrack(progress = state.progressPercent / 100f)

        Divider()

        // Subtasks
        SectionHeader(title = stringResource(R.string.detail_subtasks_title))
        if (state.subtasks.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_subtasks),
                fontFamily = ManropeFamily,
                fontSize = 13.sp,
                color = Nemo.colors.placeholder,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                // Remembered: re-sorting on every recomposition (e.g. each keystroke in the
                // quick-add field below) is wasted work; only the subtask list matters.
                val orderedSubtasks = remember(state.subtasks) {
                    state.subtasks.sortedBy { it.orderIndex }
                }
                orderedSubtasks.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundCheckbox(checked = sub.isDone, onToggle = { viewModel.onToggleSubtask(sub) })
                        Spacer(modifier = Modifier.width(Spacing.x3))
                        Text(
                            text = sub.title,
                            fontFamily = ManropeFamily,
                            fontSize = 14.sp,
                            color = if (sub.isDone) Nemo.colors.mute else Nemo.colors.ink,
                            textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Quick add subtask
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(Radius.field))
                    .background(Nemo.colors.fog)
                    .padding(horizontal = Spacing.x3),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (newSubtask.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_task_subtask_hint),
                        fontFamily = ManropeFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.placeholder,
                    )
                }
                BasicTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = ManropeFamily, fontSize = 13.sp, color = Nemo.colors.ink),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.x2))
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Nemo.colors.ink)
                    .clickable {
                        viewModel.onAddSubtask(newSubtask)
                        newSubtask = ""
                    }
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.detail_add_subtask),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Nemo.colors.onInk,
                )
            }
        }

        // Future View callout (07-screens.md Ã‚Â§2.14; opens screen 07)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.fog)
                .clickable { onOpenFutureView(task.id) }
                .padding(Spacing.x4),
        ) {
            Text(
                text = stringResource(R.string.detail_future_callout),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Nemo.colors.inkSecondary,
            )
        }

        // Actions. Completion requires a focus session (PRD v0.5 Ã‚Â§10.1), so the primary action
        // opens Focus for this task instead of completing it directly.
        PrimaryButton(
            label = stringResource(R.string.detail_action_complete_via_focus),
            onClick = { onStartFocusToComplete(task.id) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SecondaryAction(stringResource(R.string.detail_action_reschedule)) {
                showReschedule = true
            }
            SecondaryAction(stringResource(R.string.detail_action_delete), danger = true) {
                viewModel.onDelete()
                onBack()
            }
        }
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
            fontFamily = ManropeFamily,
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
