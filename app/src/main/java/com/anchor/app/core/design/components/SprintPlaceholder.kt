package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Spacing

/**
 * Sprint-0 placeholder screen body. Each top-level tab renders this until its phase is
 * built (Sprint 1 for Tasks, Sprint 2–3 for Wellbeing, Sprint 5 for Focus, Sprint 7 for
 * Insights). It announces the planned Sprint so the shell is self-documenting.
 *
 * All copy comes from string resources — nothing is hardcoded (PRD §12).
 */
@Composable
fun SprintPlaceholder(
    title: String,
    sprintLabel: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextPill(text = sprintLabel)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.x5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Text(
                text = title,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = AnchorColors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = AnchorColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
