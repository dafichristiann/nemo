package com.anchor.app.feature.intervention

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.InterventionLevel

/**
 * Screen 04 — Contextual Intervention as a calm in-app card
 * (docs/superpowers/design/07-screens.md §2.8, 04-intervention-engine.md §6).
 *
 * Shown when the engine delivered through the in-app channel (e.g. notification permission
 * denied) or the user is inside Anchor. Offers [Start Focus] / [Later] and a plain dismiss —
 * awareness, never a command. Tapping the scrim dismisses without a cooldown.
 */
@Composable
fun InterventionOverlay(
    modifier: Modifier = Modifier,
    onNavigateToFocus: (taskId: String?) -> Unit = {},
    viewModel: InterventionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visible = state as? InterventionUiState.Visible ?: return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { viewModel.onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.x4)
                .background(AnchorColors.surface, shape = Radius.panelShape)
                .clickable(enabled = false) {}
                .padding(Spacing.x6),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Text(
                text = stringResource(titleFor(visible.level)),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AnchorColors.textPrimary,
            )
            Text(
                text = stringResource(R.string.intervention_overlay_body),
                fontFamily = InterFamily,
                fontSize = 15.sp,
                color = AnchorColors.textSecondary,
            )

            PrimaryButton(
                label = stringResource(R.string.intervention_action_start_focus),
                onClick = { viewModel.onStartFocus(onNavigateToFocus) },
                modifier = Modifier.padding(top = Spacing.x2),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
            ) {
                Text(
                    text = stringResource(R.string.intervention_action_later),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AnchorColors.textSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.onLater() }
                        .padding(vertical = Spacing.x3),
                )
                Text(
                    text = stringResource(R.string.intervention_action_dismiss),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AnchorColors.textFaint,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.onDismiss() }
                        .padding(vertical = Spacing.x3),
                )
            }

            Text(
                text = stringResource(R.string.intervention_overlay_later_hint),
                fontFamily = InterFamily,
                fontSize = 12.sp,
                color = AnchorColors.textFaint,
            )
        }
    }
}

private fun titleFor(level: Int): Int = when (level) {
    InterventionLevel.FOCUS -> R.string.intervention_overlay_title_focus
    InterventionLevel.FUTURE_VIEW -> R.string.intervention_overlay_title_future
    InterventionLevel.CONTEXTUAL -> R.string.intervention_overlay_title_contextual
    else -> R.string.intervention_overlay_title_gentle
}
