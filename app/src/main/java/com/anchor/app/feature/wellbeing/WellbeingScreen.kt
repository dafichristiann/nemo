package com.anchor.app.feature.wellbeing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anchor.app.R
import com.anchor.app.core.design.components.SprintPlaceholder

/**
 * Wellbeing tab — placeholder for Sprint 2–3 (Digital Wellbeing, screen 03).
 */
@Composable
fun WellbeingScreen(modifier: Modifier = Modifier) {
    SprintPlaceholder(
        title = stringResource(R.string.wellbeing_placeholder_title),
        sprintLabel = stringResource(R.string.placeholder_sprint_label, "2–3"),
        body = stringResource(R.string.wellbeing_placeholder_body),
        modifier = modifier,
    )
}
