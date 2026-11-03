package com.anchor.app.feature.intervention

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
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
 * denied) or the user is inside Nemo. Offers [Start Focus] / [Later] and a plain dismiss —
 * awareness, never a command. Tapping the scrim dismisses without a cooldown.
 */
@Composable
fun InterventionOverlay(
    modifier: Modifier = Modifier,
    targetAppPackage: String? = null,
    activeTaskTitle: String? = null,
    remainingMinutes: Int? = null,
    onNavigateToFocus: (taskId: String?) -> Unit = {},
    viewModel: InterventionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visible = state as? InterventionUiState.Visible

    // Enter/exit motion (calm): the scrim fades, the card rises a short distance. Uses
    // AnimatedVisibility so dismissal animates out too.
    AnimatedVisibility(
        visible = visible != null,
        enter = fadeIn(Motion.standardSpec()),
        exit = fadeOut(Motion.instantSpec()),
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
                    animationSpec = Motion.largeSpec(),
                    initialOffsetY = { it / 4 },
                ) + fadeIn(Motion.standardSpec()),
                exit = slideOutVertically(
                    animationSpec = Motion.standardSpec(),
                    targetOffsetY = { it / 4 },
                ) + fadeOut(Motion.instantSpec()),
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
                        .padding(Spacing.x5),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.x3),
                ) {
                    // Pill Tag: MINDFUL INTERVENTION
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Nemo.colors.fog)
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFF34D399)),
                        )
                        Text(
                            text = stringResource(R.string.intervention_tag_mindful),
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = Nemo.colors.inkSecondary,
                        )
                    }

                    // Breathing Ring Container (canvas JuZ1E)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(top = Spacing.x1),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Outer Halo
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Nemo.colors.fog.copy(alpha = 0.5f)),
                        )
                        // Middle Guide Ring
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    Nemo.colors.line,
                                    CircleShape,
                                ),
                        )
                        // Inner Core
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Nemo.colors.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.intervention_breath_seconds),
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 18.sp,
                                    color = Nemo.colors.ink,
                                )
                                Text(
                                    text = stringResource(R.string.intervention_breath_cue),
                                    fontFamily = GeistFamily,
                                    fontSize = 8.sp,
                                    color = Nemo.colors.mute,
                                )
                            }
                        }
                    }

                    // Main Headline & Subheadline
                    Text(
                        text = stringResource(R.string.intervention_headline),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        color = Nemo.colors.ink,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    val context = LocalContext.current
                    val rawAppName = targetAppPackage
                        ?: v.targetAppPackage?.takeIf { it.isNotBlank() }
                    val appName = rawAppName?.let { pkg ->
                        runCatching {
                            val pm = context.packageManager
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        }.getOrDefault(pkg)
                    } ?: stringResource(R.string.intervention_sample_app)

                    val taskTitle = activeTaskTitle
                        ?: v.activeTaskTitle?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.intervention_sample_task)

                    val remaining = remainingMinutes
                        ?: v.remainingMinutes.takeIf { it > 0 }
                        ?: 25

                    Text(
                        text = stringResource(
                            R.string.intervention_subheadline,
                            appName,
                            taskTitle,
                            remaining,
                        ),
                        fontFamily = GeistFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.inkSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                    )

                    // Reflection Intent Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.card))
                            .background(Nemo.colors.fog)
                            .padding(Spacing.cardHorizontal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                        ) {
                            com.anchor.app.core.design.components.NemoMascot(
                                expression = com.anchor.app.core.design.components.MascotExpression.THINKING,
                                size = 20.dp,
                            )
                            Text(
                                text = stringResource(R.string.intervention_coach_tag),
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp,
                                color = Nemo.colors.mute,
                            )
                        }
                        Text(
                            text = stringResource(R.string.intervention_coach_quote),
                            fontFamily = GeistFamily,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 13.sp,
                            color = Nemo.colors.ink,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.x1))

                    // Primary Return Button
                    PrimaryButton(
                        label = stringResource(R.string.intervention_action_return),
                        onClick = { viewModel.onStartFocus(onNavigateToFocus) },
                    )

                    // Secondary Open Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                            .background(Nemo.colors.fog)
                            .pressable(shape = PillShape, onClick = { viewModel.onLater() })
                            .padding(vertical = Spacing.x3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.intervention_action_continue_5m),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Nemo.colors.ink,
                        )
                    }

                    // Emergency exit
                    Text(
                        text = stringResource(R.string.intervention_action_emergency_exit),
                        fontFamily = GeistFamily,
                        fontSize = 11.sp,
                        color = Nemo.colors.mute,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .pressable(shape = PillShape, onClick = { viewModel.onDismiss() })
                            .padding(vertical = Spacing.x1),
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
            fontFamily = GeistFamily,
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

private fun expressionFor(level: Int): com.anchor.app.core.design.components.MascotExpression = when (level) {
    InterventionLevel.FOCUS -> com.anchor.app.core.design.components.MascotExpression.HAPPY
    InterventionLevel.FUTURE_VIEW -> com.anchor.app.core.design.components.MascotExpression.TALKING
    InterventionLevel.CONTEXTUAL -> com.anchor.app.core.design.components.MascotExpression.SAD
    else -> com.anchor.app.core.design.components.MascotExpression.THINKING
}
