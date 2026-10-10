package com.anchor.app.core.design.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Nemo

/**
 * NEMO's on/off switch (design system §7). Wraps Material3 [Switch] with the app's
 * monochrome palette for **both** states, so the OFF track is never the default Material
 * color (which read as an off-brand lilac against the black-and-white UI).
 *
 * - ON:  ink track, `onInk` thumb; ink border ring.
 * - OFF: fog track, `onInk` thumb; line border so the control still reads as a switch.
 * - Disabled: everything fades to faint tokens.
 */
@Composable
fun NemoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            // ON
            checkedThumbColor = Nemo.colors.onInk,
            checkedTrackColor = Nemo.colors.ink,
            checkedBorderColor = Nemo.colors.ink,
            // OFF — the previously-unstyled state: keep it on-palette and clearly "off".
            uncheckedThumbColor = Nemo.colors.onInk,
            uncheckedTrackColor = Nemo.colors.fog,
            uncheckedBorderColor = Nemo.colors.line,
            // Disabled
            disabledCheckedThumbColor = Nemo.colors.onInk,
            disabledCheckedTrackColor = Nemo.colors.mute,
            disabledUncheckedThumbColor = Nemo.colors.onInk,
            disabledUncheckedTrackColor = Nemo.colors.fog,
            disabledUncheckedBorderColor = Nemo.colors.line,
            disabledCheckedBorderColor = Nemo.colors.mute,
        ),
    )
}
