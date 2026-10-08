package com.anchor.app.feature.focus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anchor.app.R
import com.anchor.app.core.design.components.SprintPlaceholder

/**
 * Focus tab — placeholder for Sprint 5 (Focus Setup + Active, screens 05/06).
 */
@Composable
fun FocusScreen(modifier: Modifier = Modifier) {
    SprintPlaceholder(
        title = stringResource(R.string.focus_placeholder_title),
        sprintLabel = stringResource(R.string.placeholder_sprint_label, "5"),
        body = stringResource(R.string.focus_placeholder_body),
        modifier = modifier,
    )
}
