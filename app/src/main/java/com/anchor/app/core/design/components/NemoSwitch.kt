package com.anchor.app.core.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Nemo

/**
 * NEMO's on/off switch (design-spec.md §5/§6): target size 40×24.
 *
 * Wraps Material3 [Switch] (intrinsic ~52×32) and scales it to the NEMO 40×24 footprint,
 * keeping a ≥48dp touch target via the surrounding [Box]. Palette is monochrome for both
 * states:
 * - ON:  ink track, `onInk` thumb.
 * - OFF: `line` track, `background` thumb.
 * - Disabled: muted.
 */
@Composable
fun NemoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier.size(width = 48.dp, height = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.scale(0.75f), // ~52x32 -> ~40x24 (design-spec.md §5)
            colors = SwitchDefaults.colors(
                // ON
                checkedThumbColor = Nemo.colors.onInk,
                checkedTrackColor = Nemo.colors.ink,
                checkedBorderColor = Nemo.colors.ink,
                // OFF
                uncheckedThumbColor = Nemo.colors.background,
                uncheckedTrackColor = Nemo.colors.line,
                uncheckedBorderColor = Nemo.colors.line,
                // Disabled
                disabledCheckedThumbColor = Nemo.colors.onInk,
                disabledCheckedTrackColor = Nemo.colors.mute,
                disabledUncheckedThumbColor = Nemo.colors.background,
                disabledUncheckedTrackColor = Nemo.colors.fog,
                disabledUncheckedBorderColor = Nemo.colors.line,
                disabledCheckedBorderColor = Nemo.colors.mute,
            ),
        )
    }
}
