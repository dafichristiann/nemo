package com.anchor.app.core.design.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.R
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme

/**
 * Circular add FAB (docs/superpowers/design/06-design-system.md §7.11).
 *
 * 56 dp, `#0A0A0A`, white `+` icon. Positioned bottom-right above the nav bar by the
 * caller's Scaffold/Box.
 */
@Composable
fun AddTaskFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        containerColor = AnchorColors.ink,
        contentColor = AnchorColors.surface,
        shape = androidx.compose.foundation.shape.CircleShape,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.cd_add_task),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddTaskFabPreview() {
    AnchorTheme { AddTaskFab(onClick = {}) }
}
