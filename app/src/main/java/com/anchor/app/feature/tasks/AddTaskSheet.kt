package com.anchor.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.R
import com.anchor.app.core.design.components.NemoBottomSheet
import com.anchor.app.core.design.components.NemoSwitch
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.SegmentedChips
import com.anchor.app.core.design.components.WheelPickerDialog
import com.anchor.app.core.design.components.minTouchTarget
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.RecurrenceRule
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.usecase.task.CreateTaskResult

/** Draft model collected by the Add Task sheet before it is persisted. */
data class TaskDraft(
    val title: String = "",
    val description: String? = null,
    val deadline: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val estimatedDurationMinutes: Int? = null,
    val category: String? = null,
    val subtasks: List<String> = emptyList(),
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
)

/** Estimated-duration options (minutes) offered on the Add Task wheel: 5–240 in 5-min steps. */
private val DURATION_OPTIONS_MIN: List<Int> = (1..48).map { it * 5 }

/** Deadline hour options (whole hours, 0–23) offered on the Add Task deadline wheel. */
private val DEADLINE_HOUR_OPTIONS: List<Int> = (0..23).toList()

/**
 * 02 Add Task bottom sheet (docs/superpowers/design/07-screens.md §2.6).
 *
 * Title is the only required field; everything else is optional (PRD FR-T1..T5, Quick-add
 * UX). Empty titles cannot be saved (FR §10.1 Edge Cases). Calls [onSave] with the draft
 * and a completion callback that receives the create result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    onSave: (TaskDraft, (CreateTaskResult) -> Unit) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var durationMinutes by remember { mutableStateOf<Int?>(null) }
    var category by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
    var deadlineHours by remember { mutableStateOf<Int?>(null) }
    val subtasks = remember { mutableStateListOf<String>() }
    var subtaskInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<CreateTaskResult?>(null) }

    NemoBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.x8),
            verticalArrangement = Arrangement.spacedBy(Spacing.x4),
        ) {
            // Header: title + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.add_task_title),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Nemo.colors.ink,
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close_sheet),
                    tint = Nemo.colors.mute,
                    modifier = Modifier
                        .minTouchTarget()
                        .clip(CircleShape)
                        .pressable(shape = CircleShape, onClick = onDismiss)
                        .padding(Spacing.x2),
                )
            }

            Field(value = title, onValueChange = { title = it }, hint = stringResource(R.string.add_task_field_title_hint))
            Field(value = description, onValueChange = { description = it }, hint = stringResource(R.string.add_task_field_description_hint))

            LabeledSection(stringResource(R.string.add_task_priority_label)) {
                val labels = listOf(
                    stringResource(R.string.task_priority_low),
                    stringResource(R.string.task_priority_medium),
                    stringResource(R.string.task_priority_high),
                    stringResource(R.string.task_priority_urgent),
                )
                SegmentedChips(
                    options = labels,
                    selectedIndex = priority.ordinal,
                    onSelect = { priority = TaskPriority.entries[it] },
                )
            }

            LabeledSection(stringResource(R.string.add_task_recurrence_label)) {
                val labels = listOf(
                    stringResource(R.string.recurrence_none),
                    stringResource(R.string.recurrence_daily),
                    stringResource(R.string.recurrence_weekly),
                )
                SegmentedChips(
                    options = labels,
                    selectedIndex = recurrence.ordinal,
                    onSelect = { recurrence = RecurrenceRule.entries[it] },
                )
            }

            LabeledSection(stringResource(R.string.add_task_deadline_label)) {
                // Tap the field → a centered AlertDialog opens with an hour wheel; "Set" commits,
                // "Cancel" discards. Index 0 is "No deadline"; the rest are whole hours.
                var dialogOpen by remember { mutableStateOf(false) }
                val options: List<Int?> = remember { listOf(null) + DEADLINE_HOUR_OPTIONS }
                val noDeadlineLabel = stringResource(R.string.add_task_deadline_hint)
                val displayText = deadlineHours?.let { "%02d:00".format(it) } ?: noDeadlineLabel

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(Radius.field))
                        .background(Nemo.colors.fog)
                        .pressable(
                            shape = RoundedCornerShape(Radius.field),
                            onClick = { dialogOpen = true },
                        )
                        .padding(horizontal = Spacing.x4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = displayText,
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp,
                        color = if (deadlineHours == null) Nemo.colors.placeholder else Nemo.colors.ink,
                    )
                    Text(
                        text = stringResource(R.string.add_task_duration_choose),
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Nemo.colors.mute,
                    )
                }

                if (dialogOpen) {
                    WheelPickerDialog(
                        values = options,
                        initialIndex = options.indexOf(deadlineHours).coerceAtLeast(0),
                        title = stringResource(R.string.add_task_deadline_label),
                        onDismiss = { dialogOpen = false },
                        onConfirm = { index ->
                            deadlineHours = options[index]
                            dialogOpen = false
                        },
                        formatValue = { hour ->
                            if (hour == null) noDeadlineLabel else "%02d:00".format(hour)
                        },
                    )
                }
            }

            LabeledSection(stringResource(R.string.add_task_duration_label)) {
                // Tap the field → a centered AlertDialog opens with a scroll wheel to pick the
                // minutes, then "Set" commits (Cancel discards). Metadata is edited in the small
                // dialog instead of bloating the form, and the wheel reuses the shared,
                // dependency-free WheelPicker.
                var dialogOpen by remember { mutableStateOf(false) }
                val options: List<Int?> = remember { listOf(null) + DURATION_OPTIONS_MIN }
                val notSetLabel = stringResource(R.string.add_task_duration_not_set)
                // Precompute labels in composable context; the WheelPicker `formatValue` lambda
                // is not @Composable, so it must not call stringResource itself.
                val labelsByIndex: List<String> = options.map { minutes ->
                    if (minutes == null) notSetLabel
                    else stringResource(R.string.add_task_duration_minutes, minutes)
                }
                val displayText = durationMinutes
                    ?.let { stringResource(R.string.add_task_duration_minutes, it) }
                    ?: notSetLabel

                // The tappable field (collapsed state).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(Radius.field))
                        .background(Nemo.colors.fog)
                        .pressable(
                            shape = RoundedCornerShape(Radius.field),
                            onClick = { dialogOpen = true },
                        )
                        .padding(horizontal = Spacing.x4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = displayText,
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp,
                        color = if (durationMinutes == null) Nemo.colors.placeholder else Nemo.colors.ink,
                    )
                    Text(
                        text = stringResource(R.string.add_task_duration_choose),
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Nemo.colors.mute,
                    )
                }

                if (dialogOpen) {
                    WheelPickerDialog(
                        values = options,
                        initialIndex = options.indexOf(durationMinutes).coerceAtLeast(0),
                        title = stringResource(R.string.add_task_duration_label),
                        onDismiss = { dialogOpen = false },
                        onConfirm = { index ->
                            durationMinutes = options[index]
                            dialogOpen = false
                        },
                        formatValue = { minutes ->
                            val idx = options.indexOf(minutes)
                            if (idx in labelsByIndex.indices) labelsByIndex[idx] else minutes.toString()
                        },
                    )
                }
            }

            LabeledSection(stringResource(R.string.add_task_category_label)) {
                Field(
                    value = category,
                    onValueChange = { category = it },
                    hint = stringResource(R.string.add_task_category_hint),
                )
            }

            LabeledSection(stringResource(R.string.add_task_subtasks_label, subtasks.size)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    subtasks.forEachIndexed { index, sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "• $sub",
                                fontFamily = ManropeFamily,
                                fontSize = 13.sp,
                                color = Nemo.colors.inkSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = Nemo.colors.placeholder,
                                modifier = Modifier
                                    .width(16.dp)
                                    .clickable { subtasks.removeAt(index) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            Field(
                                value = subtaskInput,
                                onValueChange = { subtaskInput = it },
                                hint = stringResource(R.string.add_task_subtask_hint),
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        TextButton(onClick = {
                            val clean = subtaskInput.trim()
                            if (clean.isNotEmpty()) {
                                subtasks.add(clean)
                                subtaskInput = ""
                            }
                        }) {
                            Text(
                                text = stringResource(R.string.add_task_subtask_add),
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Nemo.colors.ink,
                            )
                        }
                    }
                }
            }

            if (error is CreateTaskResult.EmptyTitle) {
                Text(
                    text = stringResource(R.string.home_empty_title),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    color = Nemo.colors.danger,
                )
            }

            PrimaryButton(
                label = stringResource(R.string.add_task_save),
                enabled = title.isNotBlank(),
                onClick = {
                    val draft = TaskDraft(
                        title = title.trim(),
                        description = description.trim().ifEmpty { null },
                        deadline = deadlineHours?.let {
                            val today = java.time.LocalDate.now()
                            java.time.LocalDateTime.of(today, java.time.LocalTime.of(it, 0))
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        },
                        priority = priority,
                        estimatedDurationMinutes = durationMinutes,
                        category = category.trim().ifEmpty { null },
                        subtasks = subtasks.toList(),
                        recurrence = recurrence,
                    )
                    onSave(draft) { result ->
                        error = result
                        if (result is CreateTaskResult.Success) onDismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun LabeledSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
        content()
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(Radius.field))
            .background(Nemo.colors.fog)
            .padding(horizontal = Spacing.x4),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = hint,
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                color = Nemo.colors.placeholder,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Compact toggle row used by the sheet (kept for parity with Settings later). */
@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontFamily = ManropeFamily, fontSize = 14.sp, color = Nemo.colors.ink)
        NemoSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
