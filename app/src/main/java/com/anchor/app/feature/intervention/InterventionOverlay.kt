package com.anchor.app.feature.intervention

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
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
    val visible = state as? InterventionUiState.Visible

    // Enter/exit motion (calm): the scrim fades, the card rises a short distance. Uses
    // AnimatedVisibility so dismissal animates out too.
    AnimatedVisibility(
        visible = visible != null,
        enter = fadeIn(tween(Motion.standard, easing = Motion.standardEase)),
        exit = fadeOut(tween(Motion.fast)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Nemo.colors.scrim.copy(alpha = Nemo.colors.scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { viewModel.onDismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible != null,
                enter = slideInVertically(
                    animationSpec = tween(Motion.large, easing = Motion.enterEase),
                    initialOffsetY = { it / 4 },
                ) + fadeIn(tween(Motion.standard)),
                exit = slideOutVertically(
                    animationSpec = tween(Motion.fast),
                    targetOffsetY = { it / 4 },
                ) + fadeOut(tween(Motion.fast)),
            ) {
                val v = visible ?: return@AnimatedVisibility
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.x4)
                        .clip(Radius.panelShape)
                        .background(Nemo.colors.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {}
                        .padding(Spacing.x6),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x3),
                ) {
                    Text(
                        text = stringResource(titleFor(v.level)),
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Nemo.colors.ink,
                    )
                    Text(
                        text = stringResource(R.string.intervention_overlay_body),
                        fontFamily = ManropeFamily,
                        fontSize = 15.sp,
                        color = Nemo.colors.inkSecondary,
                    )

                    Box(modifier = Modifier.padding(top = Spacing.x2)) {
                        PrimaryButton(
                            label = stringResource(R.string.intervention_action_start_focus),
                            onClick = { viewModel.onStartFocus(onNavigateToFocus) },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
                    ) {
                        OverlayTextAction(
                            label = stringResource(R.string.intervention_action_later),
                            color = Nemo.colors.inkSecondary,
                            onClick = { viewModel.onLater() },
                            modifier = Modifier.weight(1f),
                        )
                        OverlayTextAction(
                            label = stringResource(R.string.intervention_action_dismiss),
                            color = Nemo.colors.placeholder,
                            onClick = { viewModel.onDismiss() },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = stringResource(R.string.intervention_overlay_later_hint),
                        fontFamily = ManropeFamily,
                        fontSize = 12.sp,
                        color = Nemo.colors.placeholder,
                    )
                }
            }
        }
    }
}

/** A quiet text action with a 48 dp target and press feedback. */
@Composable
private fun OverlayTextAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(PillShape)
            .pressable(
                shape = PillShape,
                pressedScale = 0.97f,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = color,
        )
    }
}

private fun titleFor(level: Int): Int = when (level) {
    InterventionLevel.FOCUS -> R.string.intervention_overlay_title_focus
    InterventionLevel.FUTURE_VIEW -> R.string.intervention_overlay_title_future
    InterventionLevel.CONTEXTUAL -> R.string.intervention_overlay_title_contextual
    else -> R.string.intervention_overlay_title_gentle
}
