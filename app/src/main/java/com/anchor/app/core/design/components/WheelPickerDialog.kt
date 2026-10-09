package com.anchor.app.core.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.anchor.app.R
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily

/**
 * Centered [AlertDialog] with a scroll [WheelPicker] to pick a single value, committed on "Set".
 *
 * The wheel edits a local pending index; "Set" reports it via [onConfirm], "Cancel"/dismiss
 * discards it. This is the shared "tap a field → pick on a wheel → confirm" pattern used for the
 * Add Task estimated duration and the Focus session duration. Material3 [AlertDialog] + the
 * dependency-free [WheelPicker] keep it light and consistent with the rest of Anchor.
 *
 * @param values ordered options (top → bottom).
 * @param initialIndex index selected when the dialog opens.
 * @param formatValue maps a value to its display string (must be non-composable; pass a lambda).
 */
@Composable
fun <T> WheelPickerDialog(
    values: List<T>,
    initialIndex: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier,
    formatValue: (T) -> String = { it.toString() },
) {
    if (values.isEmpty()) return
    var pendingIndex by remember { mutableStateOf(initialIndex.coerceIn(0, values.lastIndex)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = AnchorColors.textPrimary,
            )
        },
        text = {
            WheelPicker(
                values = values,
                selectedIndex = pendingIndex,
                onSelect = { pendingIndex = it },
                modifier = Modifier.fillMaxWidth(),
                formatValue = formatValue,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pendingIndex) }) {
                Text(
                    text = stringResource(R.string.dialog_set),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = AnchorColors.ink,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    color = AnchorColors.textMuted,
                )
            }
        },
        containerColor = AnchorColors.surface,
    )
}
