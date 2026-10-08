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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.TextPill
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorElevation
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.LocalAnchorElevation
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.TaskPriority

/**
 * 10 Task Detail (docs/superpowers/design/07-screens.md §2.14).
 *
 * Priority bar, title, badges, meta, progress track, subtasks (check/order), a Future
 * View callout (wired in Sprint 6), and the actions: Start Focus, Reschedule, Mark Done.
 */
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newSubtask by remember { mutableStateOf("") }
    var showReschedule by remember { mutableStateOf(false) }
    val task = state.task

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        // Header: back + title
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = AnchorColors.textPrimary,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Text(
                text = stringResource(R.string.detail_back),
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AnchorColors.textPrimary,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }

        if (task == null) {
            Text(
                text = stringResource(R.string.home_empty_title),
                fontFamily = InterFamily,
                fontSize = 14.sp,
                color = AnchorColors.textMuted,
            )
            return@Column
        }

        // Priority bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(Radius.track))
                .background(AnchorColors.ink),
        )

        Text(
            text = task.title,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = AnchorColors.textPrimary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            TextPill(text = priorityLabel(task.priority), highlighted = task.priority == TaskPriority.HIGH)
            TextPill(text = stringResource(R.string.detail_status_in_progress))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            state.deadlineLabel?.let {
                Text(it, fontFamily = InterFamily, fontSize = 13.sp, color = AnchorColors.textMuted)
            }
            state.estimatedLabel?.let {
                Text(
                    text = stringResource(R.string.detail_meta_estimated, it),
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = AnchorColors.textMuted,
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
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = AnchorColors.textMuted,
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
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = AnchorColors.textFaint,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                state.subtasks.sortedBy { it.orderIndex }.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundCheckbox(checked = sub.isDone, onToggle = { viewModel.onToggleSubtask(sub) })
                        Spacer(modifier = Modifier.width(Spacing.x3))
                        Text(
                            text = sub.title,
                            fontFamily = InterFamily,
                            fontSize = 14.sp,
                            color = if (sub.isDone) AnchorColors.textMuted else AnchorColors.textPrimary,
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
                    .background(AnchorColors.surfaceSunken)
                    .padding(horizontal = Spacing.x3),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (newSubtask.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_task_subtask_hint),
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = AnchorColors.textFaint,
                    )
                }
                BasicTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = InterFamily, fontSize = 13.sp, color = AnchorColors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.x2))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(AnchorColors.ink)
                    .clickable {
                        viewModel.onAddSubtask(newSubtask)
                        newSubtask = ""
                    }
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.detail_add_subtask),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AnchorColors.surface,
                )
            }
        }

        // Future View callout (07-screens.md §2.14; screen built in Sprint 6)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(AnchorColors.surfaceSunken)
                .padding(Spacing.x4),
        ) {
            Text(
                text = stringResource(R.string.detail_future_callout),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = AnchorColors.textSecondary,
            )
        }

        // Actions
        PrimaryButton(
            label = stringResource(R.string.detail_action_complete),
            onClick = { viewModel.onComplete() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SecondaryAction(stringResource(R.string.detail_action_start_focus)) { /* Sprint 5 */ }
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

@Composable
private fun SecondaryAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.chip))
            .border(1.dp, if (danger) AnchorColors.accentDanger else AnchorColors.border, RoundedCornerShape(Radius.chip))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = if (danger) AnchorColors.accentDanger else AnchorColors.textSecondary,
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
