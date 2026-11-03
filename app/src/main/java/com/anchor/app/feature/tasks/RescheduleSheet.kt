package com.anchor.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.R
import com.anchor.app.core.design.components.NemoBottomSheet
import com.anchor.app.core.design.components.NemoSheetHeader
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reschedule picker (07-screens.md Â§2.5/Â§2.14, FR-T12).
 *
 * A calm, non-blocking way to move a deadline: pick a day offset (Today / Tomorrow /
 * +3 Days / +7 Days) and a time-of-day. Days are computed from a [today] anchor passed
 * by the caller (from `TimeProvider`) so behaviour stays deterministic in tests. The
 * sheet returns the resolved epoch-millis deadline via [onConfirm].
 *
 * If [allowClear] is set, the user may also clear the deadline entirely (deadline -> null),
 * which the reschedule use case records as a RescheduleEvent just like any other move.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleSheet(
    today: LocalDate,
    zone: ZoneId,
    currentDeadline: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true,
) {
    val (initialDay, initialHour) = remember(currentDeadline, today, zone) {
        initialRescheduleSelection(currentDeadline, today, zone)
    }
    var selectedDay by remember { mutableStateOf(initialDay) }
    var selectedHour by remember { mutableIntStateOf(initialHour) }

    val dayOptions = listOf(
        RescheduleDay.TODAY to stringResource(R.string.reschedule_day_today),
        RescheduleDay.TOMORROW to stringResource(R.string.reschedule_day_tomorrow),
        RescheduleDay.PLUS_3 to stringResource(R.string.reschedule_day_plus3),
        RescheduleDay.PLUS_7 to stringResource(R.string.reschedule_day_plus7),
    )

    NemoBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.x8),
            verticalArrangement = Arrangement.spacedBy(Spacing.x5),
        ) {
            NemoSheetHeader(
                title = stringResource(R.string.reschedule_title),
                onClose = onDismiss,
            )

            LabeledRow(stringResource(R.string.reschedule_day_label)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    dayOptions.forEach { (day, label) ->
                        ChoiceChip(
                            label = label,
                            selected = selectedDay == day,
                            onClick = {
                                selectedDay = day
                                if (day == RescheduleDay.TODAY && today == LocalDate.now(zone)) {
                                    val currentHour = LocalTime.now(zone).hour
                                    if (selectedHour <= currentHour) {
                                        selectedHour = RESCHEDULE_HOURS.firstOrNull { it > currentHour } ?: 23
                                    }
                                }
                            },
                        )
                    }
                }
            }

            LabeledRow(stringResource(R.string.reschedule_time_label)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    RESCHEDULE_HOURS.forEach { hour ->
                        val isPastToday = selectedDay == RescheduleDay.TODAY &&
                            today == LocalDate.now(zone) &&
                            LocalTime.now(zone).hour >= hour
                        ChoiceChip(
                            label = "%02d:00".format(hour),
                            selected = selectedHour == hour,
                            enabled = !isPastToday,
                            onClick = { selectedHour = hour },
                        )
                    }
                }
            }

            PrimaryButton(
                label = stringResource(R.string.reschedule_confirm),
                onClick = {
                    onConfirm(resolveDeadlineMillis(selectedDay, selectedHour, today, zone))
                },
            )

            if (allowClear && currentDeadline != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .border(1.dp, Nemo.colors.line, PillShape)
                        .clickable { onConfirm(null) }
                        .padding(vertical = Spacing.x3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.reschedule_clear),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Nemo.colors.inkSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
        Text(
            text = label,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
        content()
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(
                if (selected) Nemo.colors.ink
                else if (!enabled) Nemo.colors.fog.copy(alpha = 0.5f)
                else Nemo.colors.fog,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
    ) {
        Text(
            text = label,
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) Nemo.colors.onInk
            else if (!enabled) Nemo.colors.mute.copy(alpha = 0.5f)
            else Nemo.colors.inkSecondary,
        )
    }
}
