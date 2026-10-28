package com.anchor.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
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
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
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

/** Estimated-duration options (minutes) offered on the Add Task wheel: 5â€“240 in 5-min steps. */
private val DURATION_OPTIONS_MIN: List<Int> = (1..48).map { it * 5 }

/** Deadline hour options (whole hours, 0â€“23) offered on the Add Task deadline wheel. */
private val DEADLINE_HOUR_OPTIONS: List<Int> = (0..23).toList()

/**
 * 02 Add Task bottom sheet (docs/superpowers/design/07-screens.md Â§2.6).
 *
 * Title is the only required field; everything else is optional (PRD FR-T1..T5, Quick-add
 * UX). Empty titles cannot be saved (FR Â§10.1 Edge Cases). Calls [onSave] with the draft
 * and a completion callback that receives the create result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    today: java.time.LocalDate = java.time.LocalDate.now(),
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    onDismiss: () -> Unit,
    onSave: (TaskDraft, (CreateTaskResult) -> Unit) -> Unit,
) {
    var isSubmitting by remember { mutableStateOf(false) }
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
    // "More" disclosure (design-spec.md §7 #5): repeat, category and subtasks live behind it.
    var moreExpanded by remember { mutableStateOf(false) }

    NemoBottomSheet(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
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
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cd_close_sheet),
                    tint = Nemo.colors.mute,
                    modifier = Modifier
                        .minTouchTarget()
                        .clip(CircleShape)
                        .pressable(
                            shape = CircleShape,
                            enabled = !isSubmitting,
                            onClick = { if (!isSubmitting) onDismiss() },
                        )
                        .padding(Spacing.x2),
                )
            }

            Field(
                value = title,
                onValueChange = { title = it },
                hint = "Task title…",
                enabled = !isSubmitting,
            )
            Field(
                value = description,
                onValueChange = { description = it },
                hint = stringResource(R.string.add_task_field_description_hint),
                enabled = !isSubmitting,
            )

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

            // Two-column Deadline & Duration layout matching canvas vrE9D
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                // Deadline column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    Text(
                        text = stringResource(R.string.add_task_deadline_label),
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Nemo.colors.inkSecondary,
                    )
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
                                enabled = !isSubmitting,
                                onClick = { if (!isSubmitting) dialogOpen = true },
                            )
                            .padding(horizontal = Spacing.x3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = displayText,
                            fontFamily = ManropeFamily,
                            fontSize = 13.sp,
                            color = if (deadlineHours == null) Nemo.colors.placeholder else Nemo.colors.ink,
                            maxLines = 1,
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

                // Est. Duration column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    Text(
                        text = "Est. Duration",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Nemo.colors.inkSecondary,
                    )
                    var dialogOpen by remember { mutableStateOf(false) }
                    val options: List<Int?> = remember { listOf(null) + DURATION_OPTIONS_MIN }
                    val notSetLabel = stringResource(R.string.add_task_duration_not_set)
                    val labelsByIndex: List<String> = options.map { minutes ->
                        if (minutes == null) notSetLabel
                        else stringResource(R.string.add_task_duration_minutes, minutes)
                    }
                    val displayText = durationMinutes
                        ?.let { stringResource(R.string.add_task_duration_minutes, it) }
                        ?: notSetLabel

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(Radius.field))
                            .background(Nemo.colors.fog)
                            .pressable(
                                shape = RoundedCornerShape(Radius.field),
                                enabled = !isSubmitting,
                                onClick = { if (!isSubmitting) dialogOpen = true },
                            )
                            .padding(horizontal = Spacing.x3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = displayText,
                            fontFamily = ManropeFamily,
                            fontSize = 13.sp,
                            color = if (durationMinutes == null) Nemo.colors.placeholder else Nemo.colors.ink,
                            maxLines = 1,
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
            }

            // Direct Subtasks list matching canvas vrE9D
            LabeledSection(
                if (subtasks.isEmpty()) "Subtasks" else "Subtasks (${subtasks.size})"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    subtasks.forEachIndexed { index, sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.field))
                                .background(Nemo.colors.fog)
                                .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = sub,
                                fontFamily = ManropeFamily,
                                fontSize = 13.sp,
                                color = Nemo.colors.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.cd_close_sheet),
                                tint = Nemo.colors.placeholder,
                                modifier = Modifier
                                    .minTouchTarget()
                                    .size(16.dp)
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
                                enabled = !isSubmitting,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        TextButton(
                            enabled = !isSubmitting,
                            onClick = {
                                val clean = subtaskInput.trim()
                                if (clean.isNotEmpty()) {
                                    subtasks.add(clean)
                                    subtaskInput = ""
                                }
                            },
                        ) {
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

            // "More" disclosure for Recurrence and Category
            MoreToggle(expanded = moreExpanded, onToggle = { moreExpanded = !moreExpanded })

            if (moreExpanded) {
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

                LabeledSection(stringResource(R.string.add_task_category_label)) {
                    Field(
                        value = category,
                        onValueChange = { category = it },
                        hint = stringResource(R.string.add_task_category_hint),
                        enabled = !isSubmitting,
                    )
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
                enabled = title.isNotBlank() && !isSubmitting,
                loading = isSubmitting,
                loadingText = stringResource(R.string.add_task_saving),
                onClick = {
                    if (isSubmitting) return@PrimaryButton
                    isSubmitting = true
                    val draft = TaskDraft(
                        title = title.trim(),
                        description = description.trim().ifEmpty { null },
                        deadline = deadlineHours?.let {
                            java.time.LocalDateTime.of(today, java.time.LocalTime.of(it, 0))
                                .atZone(zone)
                                .toInstant().toEpochMilli()
                        },
                        priority = priority,
                        estimatedDurationMinutes = durationMinutes,
                        category = category.trim().ifEmpty { null },
                        subtasks = subtasks.toList(),
                        recurrence = recurrence,
                    )
                    onSave(draft) { result ->
                        if (result is CreateTaskResult.Success) {
                            onDismiss()
                        } else {
                            error = result
                            isSubmitting = false
                        }
                    }
                },
            )
        }
    }
}

/**
 * The "More" disclosure row (design-spec.md §7 #5): a quiet text row with a rotating chevron
 * that reveals repeat / category / subtasks.
 */
@Composable
private fun MoreToggle(expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .pressable(shape = PillShape, onClick = onToggle)
            .padding(vertical = Spacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.add_task_more),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Nemo.colors.inkSecondary,
        )
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = Motion.standardSpec(),
            label = "more-chevron",
        )
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = null,
            tint = Nemo.colors.mute,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation + 270f),
        )
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
    enabled: Boolean = true,
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
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                color = if (enabled) Nemo.colors.ink else Nemo.colors.mute,
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
