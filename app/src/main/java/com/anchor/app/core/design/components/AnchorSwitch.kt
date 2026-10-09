package com.anchor.app.core.design.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.AnchorColors

/**
 * Anchor's on/off switch (design system §7). Wraps Material3 [Switch] with the app's
 * monochrome palette for **both** states, so the OFF track is never the default Material
 * color (which read as an off-brand lilac against the black-and-white UI).
 *
 * - ON:  ink track, white thumb; ink border ring.
 * - OFF: muted track, white thumb; soft border so the control still reads as a switch.
 * - Disabled: everything fades to faint tokens.
 */
@Composable
fun AnchorSwitch(
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
            checkedThumbColor = AnchorColors.surface,
            checkedTrackColor = AnchorColors.ink,
            checkedBorderColor = AnchorColors.ink,
            // OFF — the previously-unstyled state: keep it on-palette and clearly "off".
            uncheckedThumbColor = AnchorColors.surface,
            uncheckedTrackColor = AnchorColors.surfaceMuted,
            uncheckedBorderColor = AnchorColors.border,
            // Disabled
            disabledCheckedThumbColor = AnchorColors.surface,
            disabledCheckedTrackColor = AnchorColors.textFaintSoft,
            disabledUncheckedThumbColor = AnchorColors.surface,
            disabledUncheckedTrackColor = AnchorColors.track,
            disabledUncheckedBorderColor = AnchorColors.borderSoft,
            disabledCheckedBorderColor = AnchorColors.textFaintSoft,
        ),
    )
}
