package com.anchor.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.SegmentedChips
import com.anchor.app.core.design.components.TextPill
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.FocusMode

/**
 * Focus tab (screens 05 Setup + 06 Active, docs/superpowers/design/07-screens.md §2.9-2.10).
 *
 * Setup: task selector, mode cards (Gentle/Strict/Custom), duration chips, Begin.
 * Active: task name, current step, timer + progress, Pause/Finish, and an always-available
 * **Emergency exit** (PRD §10.5). Honest fallback notice when Strict/Custom degrades to Gentle.
 */
@Composable
fun FocusScreen(
    modifier: Modifier = Modifier,
    onManageBlockedApps: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-check the OS Accessibility status whenever we return to this screen (FR-F8): the user may
    // have just enabled the service in Settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshBlockingStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.isRunning) {
        FocusActiveContent(state = state, viewModel = viewModel, modifier = modifier)
    } else {
        FocusSetupContent(
            state = state,
            viewModel = viewModel,
            onManageBlockedApps = onManageBlockedApps,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            modifier = modifier,
        )
    }
}

@Composable
private fun FocusSetupContent(
    state: FocusUiState,
    viewModel: FocusViewModel,
    onManageBlockedApps: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x5),
    ) {
        Text(
            text = stringResource(R.string.focus_setup_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AnchorColors.textPrimary,
        )

        if (state.lastFallbackReason != null) {
            Notice(text = stringResource(R.string.focus_fallback_notice))
        }

        // Task selector
        SectionHeader(title = stringResource(R.string.focus_task_label))
        if (state.taskOptions.isEmpty()) {
            Text(
                text = stringResource(R.string.focus_none_scheduled),
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = AnchorColors.textFaint,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                state.taskOptions.forEach { option ->
                    SelectableRow(
                        label = option.title,
                        selected = option.id == state.selectedTaskId,
                        onClick = { viewModel.onSelectTask(option.id) },
                    )
                }
            }
        }

        // Mode cards
        SectionHeader(title = stringResource(R.string.focus_mode_label))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            ModeCard(
                title = stringResource(R.string.focus_mode_gentle),
                hint = stringResource(R.string.focus_mode_gentle_hint),
                selected = state.selectedMode == FocusMode.GENTLE,
                onClick = { viewModel.onSelectMode(FocusMode.GENTLE) },
            )
            ModeCard(
                title = stringResource(R.string.focus_mode_strict),
                hint = stringResource(R.string.focus_mode_strict_hint),
                selected = state.selectedMode == FocusMode.STRICT,
                onClick = { viewModel.onSelectMode(FocusMode.STRICT) },
            )
            ModeCard(
                title = stringResource(R.string.focus_mode_custom),
                hint = stringResource(R.string.focus_mode_custom_hint),
                selected = state.selectedMode == FocusMode.CUSTOM,
                onClick = { viewModel.onSelectMode(FocusMode.CUSTOM) },
            )
        }

        // Blocked-apps summary for Strict/Custom (FR-S4): pre-filled from Settings. When empty,
        // offer a calm CTA instead of blocking the user — the engine falls back to Gentle (FR-F8).
        if (state.selectedMode == FocusMode.STRICT || state.selectedMode == FocusMode.CUSTOM) {
            if (state.blockedApps.isNotEmpty()) {
                Notice(text = stringResource(R.string.focus_blocked_summary, state.blockedApps.size))
            } else {
                Notice(text = stringResource(R.string.focus_blocked_empty))
                Text(
                    text = stringResource(R.string.focus_blocked_manage),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = AnchorColors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageBlockedApps)
                        .padding(vertical = Spacing.x1),
                )
            }

            // Blocking readiness (FR-F4/F8, PRD v0.4 §10.5): the OS Accessibility service must be
            // enabled for Strict/Custom to actually pause apps. Explain, then offer to enable it —
            // if the user declines, the session simply runs as Gentle (handled by StartFocusUseCase).
            if (!state.blockingReady) {
                Notice(text = stringResource(R.string.focus_blocking_setup_body))
                Text(
                    text = stringResource(R.string.focus_blocking_setup_cta),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = AnchorColors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAccessibilitySettings)
                        .padding(vertical = Spacing.x1),
                )
            }
        }

        // Duration chips
        SectionHeader(title = stringResource(R.string.focus_duration_label))
        SegmentedChips(
            options = FOCUS_DURATIONS_MIN.map { stringResource(R.string.focus_duration_min, it) },
            selectedIndex = FOCUS_DURATIONS_MIN.indexOf(state.selectedDurationMin).coerceAtLeast(0),
            onSelect = { viewModel.onSelectDuration(FOCUS_DURATIONS_MIN[it]) },
        )

        Spacer(modifier = Modifier.height(Spacing.x2))
        PrimaryButton(
            label = stringResource(R.string.focus_begin),
            onClick = viewModel::onBegin,
        )
    }
}

@Composable
private fun FocusActiveContent(
    state: FocusUiState,
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier,
) {
    val active = state.active ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        TextPill(text = stringResource(R.string.focus_active_tag), highlighted = true)

        Text(
            text = active.taskTitle ?: stringResource(R.string.focus_task_none),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = AnchorColors.textPrimary,
        )

        // Timer + progress (required by PRD §10.5)
        Text(
            text = formatClock(active.remainingMs ?: active.elapsedMs),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = AnchorColors.textPrimary,
        )
        ProgressTrack(progress = active.progressFraction)

        if (state.lastFallbackReason != null) {
            Notice(text = stringResource(R.string.focus_fallback_notice))
        }

        // Current step
        SectionHeader(title = stringResource(R.string.focus_active_current_step))
        Text(
            text = active.currentStep?.title
                ?: if (active.totalSteps == 0) stringResource(R.string.detail_no_subtasks)
                else stringResource(R.string.detail_meta_progress, active.doneSteps * 100 / active.totalSteps, active.doneSteps, active.totalSteps),
            fontFamily = InterFamily,
            fontSize = 14.sp,
            color = AnchorColors.textSecondary,
        )

        Divider()

        // Pause / Finish
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            PrimaryButton(
                label = stringResource(if (active.paused) R.string.focus_active_resume else R.string.focus_active_pause),
                onClick = viewModel::onTogglePause,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                label = stringResource(R.string.focus_active_finish),
                onClick = viewModel::onFinish,
                modifier = Modifier.weight(1f),
            )
        }

        // Emergency exit — always available (PRD §10.5)
        Text(
            text = stringResource(R.string.focus_emergency_exit),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = viewModel::onEmergencyExit)
                .padding(vertical = Spacing.x3),
        )

        // Post-session progress offer (FR-F6)
        if (state.lastFinishedTaskId != null) {
            Notice(text = stringResource(R.string.focus_finished_toast))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                PrimaryButton(
                    label = stringResource(R.string.focus_update_progress),
                    onClick = { viewModel.onUpdateProgress(active.doneSteps.coerceAtLeast(1) * 10) },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    label = stringResource(R.string.overdue_action_dismiss),
                    onClick = viewModel::onDismissProgressOffer,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.field))
            .background(if (selected) AnchorColors.ink else AnchorColors.surfaceSunken)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (selected) AnchorColors.surface else AnchorColors.textPrimary,
        )
    }
}

@Composable
private fun ModeCard(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(if (selected) AnchorColors.surfaceMuted else AnchorColors.surfaceSunken)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AnchorColors.ink else AnchorColors.border,
                shape = RoundedCornerShape(Radius.card),
            )
            .clickable(onClick = onClick)
            .padding(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        Text(
            text = title,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = AnchorColors.textPrimary,
        )
        Text(
            text = hint,
            fontFamily = InterFamily,
            fontSize = 12.sp,
            color = AnchorColors.textSecondary,
        )
    }
}

@Composable
private fun Notice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(AnchorColors.surfaceSunken)
            .padding(Spacing.x4),
    ) {
        Text(
            text = text,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
        )
    }
}

/** mm:ss (or h:mm:ss for long sessions) — used for both the countdown and the elapsed clock. */
private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
