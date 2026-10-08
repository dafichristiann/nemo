package com.anchor.app.feature.insights

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anchor.app.R
import com.anchor.app.core.design.components.SprintPlaceholder

/**
 * Insights tab — placeholder for Sprint 7 (Daily Reflection, screen 08).
 */
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    SprintPlaceholder(
        title = stringResource(R.string.insights_placeholder_title),
        sprintLabel = stringResource(R.string.placeholder_sprint_label, "7"),
        body = stringResource(R.string.insights_placeholder_body),
        modifier = modifier,
    )
}
