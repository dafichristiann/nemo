package com.anchor.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.anchor.app.core.design.components.SecondaryButton
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.TextPill
import com.anchor.app.core.design.components.TimerRing
import com.anchor.app.core.design.components.WheelPickerDialog
import com.anchor.app.core.design.components.formatDurationSeconds
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.FocusMode

/**
 * Focus tab (screens 05 Setup + 06 Active, docs/superpowers/design/07-screens.md §2.9-2.10).
 *
 * Setup: task selector, mode cards (Gentle/Strict), duration wheel, Begin.
 * Active: task name, current step, timer + progress, Pause/Finish, and an always-available
 * **Emergency exit** (PRD §10.5). Honest fallback notice when Strict degrades to Gentle.
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
            // Custom mode is intentionally hidden for now: its runtime behaviour is identical to
            // Strict (both pause the same blocked set), so offering both added no real choice.
        }

        // Blocked-apps summary for Strict (FR-S4): pre-filled from Settings. When empty,
        // offer a calm CTA instead of blocking the user — the engine falls back to Gentle (FR-F8).
        if (state.selectedMode == FocusMode.STRICT) {
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
                        .pressable(shape = RoundedCornerShape(Radius.chip), onClick = onManageBlockedApps)
                        .padding(vertical = Spacing.x1),
                )
            }

            // Blocking readiness (FR-F4/F8, PRD v0.4 §10.5): the OS Accessibility service must be
            // enabled for Strict to actually pause apps. Explain, then offer to enable it —
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
                        .pressable(shape = RoundedCornerShape(Radius.chip), onClick = onOpenAccessibilitySettings)
                        .padding(vertical = Spacing.x1),
                )
            }
        }

        // Duration field (tap → wheel dialog; seconds precision, no keyboard).
        SectionHeader(title = stringResource(R.string.focus_duration_label))
        var durationDialogOpen by remember { mutableStateOf(false) }
        val durationIndex = FOCUS_DURATION_STEPS_SEC
            .indexOf(state.selectedDurationSec)
            .let { if (it >= 0) it else FOCUS_DURATION_STEPS_SEC.indexOf(FOCUS_DEFAULT_DURATION_SEC) }
            .coerceAtLeast(0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(Radius.field))
                .background(AnchorColors.surfaceSunken)
                .pressable(
                    shape = RoundedCornerShape(Radius.field),
                    onClick = { durationDialogOpen = true },
                )
                .padding(horizontal = Spacing.x4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDurationSeconds(state.selectedDurationSec),
                fontFamily = InterFamily,
                fontSize = 14.sp,
                color = AnchorColors.textPrimary,
            )
            Text(
                text = stringResource(R.string.add_task_duration_choose),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = AnchorColors.textMuted,
            )
        }
        if (durationDialogOpen) {
            WheelPickerDialog(
                values = FOCUS_DURATION_STEPS_SEC,
                initialIndex = durationIndex,
                title = stringResource(R.string.focus_duration_label),
                onDismiss = { durationDialogOpen = false },
                onConfirm = { index ->
                    viewModel.onSelectDuration(FOCUS_DURATION_STEPS_SEC[index])
                    durationDialogOpen = false
                },
                formatValue = { formatDurationSeconds(it) },
            )
        }

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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        // Header row: status pill on the left so the ring below can be centered.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextPill(text = stringResource(R.string.focus_active_tag), highlighted = true)
        }

        Text(
            text = active.taskTitle ?: stringResource(R.string.focus_task_none),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AnchorColors.textPrimary,
            textAlign = TextAlign.Center,
        )

        // Ring Timer (06-design-system.md §7.13): thick monochrome progress ring with the
        // large digits centered inside. Replaces the old text + flat bar pairing.
        TimerRing(
            progress = active.progressFraction,
            time = formatClock(active.remainingMs ?: active.elapsedMs),
            subLabel = stringResource(
                if (active.paused) R.string.focus_active_paused_label else R.string.focus_active_remaining_label,
            ),
            modifier = Modifier.padding(top = Spacing.x2),
        )

        if (state.lastFallbackReason != null) {
            Notice(text = stringResource(R.string.focus_fallback_notice))
        }

        // Current step
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SectionHeader(title = stringResource(R.string.focus_active_current_step))
            Text(
                text = active.currentStep?.title
                    ?: if (active.totalSteps == 0) stringResource(R.string.detail_no_subtasks)
                    else stringResource(
                        R.string.detail_meta_progress,
                        active.doneSteps * 100 / active.totalSteps,
                        active.doneSteps,
                        active.totalSteps,
                    ),
                fontFamily = InterFamily,
                fontSize = 14.sp,
                color = AnchorColors.textSecondary,
            )
        }

        Divider()

        // Pause / Finish — clear hierarchy: Finish is primary, Pause is a quieter secondary.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            SecondaryButton(
                label = stringResource(if (active.paused) R.string.focus_active_resume else R.string.focus_active_pause),
                onClick = viewModel::onTogglePause,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f)) {
                PrimaryButton(
                    label = stringResource(R.string.focus_active_finish),
                    onClick = viewModel::onFinish,
                )
            }
        }

        // Emergency exit — always available (PRD §10.5)
        Text(
            text = stringResource(R.string.focus_emergency_exit),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .pressable(
                    shape = RoundedCornerShape(Radius.chip),
                    onClick = viewModel::onEmergencyExit,
                )
                .padding(vertical = Spacing.x3),
        )

        // Post-session feedback (PRD v0.5 §10.1): a full session completed the task; a partial
        // one did not. Tone stays calm and non-judgmental in both cases.
        state.lastCompletion?.let { completion ->
            Notice(
                text = if (completion.verified) {
                    stringResource(R.string.focus_finished_verified, completion.taskTitle, completion.focusMinutes)
                } else {
                    stringResource(R.string.focus_finished_not_verified)
                },
            )
            Text(
                text = stringResource(R.string.overdue_action_dismiss),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = AnchorColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(shape = RoundedCornerShape(Radius.chip), onClick = viewModel::onDismissCompletion)
                    .padding(vertical = Spacing.x2),
            )
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) AnchorColors.ink else AnchorColors.surfaceSunken,
        animationSpec = com.anchor.app.core.design.theme.Motion.standardSpec(),
        label = "selectable-bg",
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) AnchorColors.surface else AnchorColors.textPrimary,
        animationSpec = com.anchor.app.core.design.theme.Motion.standardSpec(),
        label = "selectable-fg",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.field))
            .background(background)
            .pressable(shape = RoundedCornerShape(Radius.field), onClick = onClick)
            .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = contentColor,
        )
    }
}

@Composable
private fun ModeCard(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val background by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) AnchorColors.surfaceMuted else AnchorColors.surfaceSunken,
        animationSpec = com.anchor.app.core.design.theme.Motion.standardSpec(),
        label = "mode-bg",
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) AnchorColors.ink else AnchorColors.border,
        animationSpec = com.anchor.app.core.design.theme.Motion.standardSpec(),
        label = "mode-border",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Radius.card),
            )
            .pressable(shape = RoundedCornerShape(Radius.card), onClick = onClick)
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
