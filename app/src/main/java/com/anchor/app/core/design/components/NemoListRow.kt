package com.anchor.app.core.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Spacing

/**
 * NEMO list row (design-spec.md §6): `titleMedium` title, optional `bodySmall` subtitle, an
 * optional trailing slot, and a `line` divider underneath. Used in Settings, permissions and
 * blocked-apps lists. Minimum height 56 so rows stay comfortable.
 */
@Composable
fun NemoListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Nemo.colors
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 56.dp)
                .padding(vertical = Spacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.mute)
                }
            }
            trailing?.invoke()
        }
        HorizontalDivider(color = c.line)
    }
}

@Preview(showBackground = true)
@Composable
private fun NemoListRowPreview() {
    NemoTheme {
        Column(Modifier.padding(horizontal = Spacing.screenHorizontal)) {
            NemoListRow(title = "Profile", subtitle = "Rina")
            NemoListRow(title = "Privacy & AI", trailing = { NemoPill(text = "On", filled = true) })
        }
    }
}
