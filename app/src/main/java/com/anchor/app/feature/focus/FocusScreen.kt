package com.anchor.app.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.platform.audio.LofiGenre
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.components.NemoMascot
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSwitch
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.Subtask

/**
 * 05 Focus Setup & 06 Focus Active (NEMO specs: QhNZe, BBjNr, vFsir).
 *
 * SETUP MODE:
 * - Header Block: "INTENTIONAL CALIBRATION", "Focus Session", "Konfigurasi lingkungan fokus sebelum mulai"
 * - Task Section: "FOKUS PADA TUGAS", selected task card with "Ganti" button & picker dialog
 * - Mode Section: "TINGKAT PROTEKSI", Gentle Awareness vs Strict Blocking cards with Shield Notice
 * - Duration Section: "DURASI TARGET", 25m/45m/60m/Custom chips + Circadian hint
 * - Soundscape Card: "Binaural Brown Noise (40Hz)" with switch
 * - Begin Button: "Mulai Sesi Fokus"
 *
 * ACTIVE MODE:
 * - Absolute #0A0A0A dark background
 * - Header pills: STRICT FOCUS & 432Hz Brown Noise
 * - Active intention micro & task title
 * - Endel Ring Box: large countdown ring MM:SS + waveform bars
 * - Subtasks Checklist Card with interactive toggle
 * - Action Controls: Pause/Resume + Finish (contrast white)
 * - Emergency unblock (Grace 2m)
 */
@Composable
fun FocusScreen(
    modifier: Modifier = Modifier,
    onManageBlockedApps: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onFinished: () -> Unit = {},
    onBack: () -> Unit = onFinished,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshBlockingStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.isRunning) {
        FocusActiveContent(
            state = state,
            viewModel = viewModel,
            onFinished = onFinished,
            onBack = onBack,
            modifier = modifier,
        )
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

// =========================================================================
// 1. FOCUS SETUP MODE (Frames QhNZe / BBjNr)
// =========================================================================

@Composable
private fun FocusSetupContent(
    state: FocusUiState,
    viewModel: FocusViewModel,
    onManageBlockedApps: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTaskPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        // --- Header Block ---
        val setupMascotExp = MascotExpression.NEUTRAL
        NemoScreenHeader(
            title = stringResource(R.string.focus_setup_title_header),
            subtitle = stringResource(R.string.focus_setup_subtitle_header),
            action = {
                NemoMascot(
                    expression = setupMascotExp,
                    size = 48.dp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
        )

        // Post-session feedback if present (Spec 2026-11-03)
        state.lastCompletion?.let { completion ->
            FocusCompletionCard(
                completion = completion,
                onRate = viewModel::onRateLastSession,
                onDismiss = viewModel::onDismissCompletion,
            )
        }

        // --- Task Section ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.focus_section_task),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.mute,
            )

            val selectedTask = state.selectedTask
            val taskTitle = selectedTask?.title ?: if (state.taskOptions.isNotEmpty()) state.taskOptions.first().title else stringResource(R.string.focus_task_default)
            val durationMin = selectedTask?.estimatedDurationMin?.takeIf { it > 0 } ?: (state.selectedDurationSec / 60)
            val baseMeta = selectedTask?.let {
                stringResource(R.string.focus_task_priority_prefix, it.priority.name.lowercase().replaceFirstChar { c -> c.uppercase() })
            } ?: stringResource(R.string.focus_task_primary_fallback)
            val taskMeta = "$baseMeta • ${stringResource(R.string.focus_duration_min, durationMin)}"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Nemo.colors.surface)
                    .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Nemo.colors.ink),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = taskTitle,
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Nemo.colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = taskMeta,
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Nemo.colors.mute,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Nemo.colors.fog)
                        .pressable(shape = RoundedCornerShape(6.dp)) { showTaskPicker = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.focus_action_change),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Nemo.colors.inkSecondary,
                    )
                }
            }
        }

        // --- Mode Section (2 cards grid) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.focus_section_protection),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.mute,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Gentle Mode Card
                val isGentle = state.selectedMode == FocusMode.GENTLE
                ModeCardItem(
                    title = stringResource(R.string.focus_mode_gentle_card_title),
                    desc = stringResource(R.string.focus_mode_gentle_card_desc),
                    iconRes = R.drawable.ic_wellbeing,
                    isSelected = isGentle,
                    onClick = { viewModel.onSelectMode(FocusMode.GENTLE) },
                    modifier = Modifier.weight(1f),
                )

                // Strict Mode Card
                val isStrict = state.selectedMode == FocusMode.STRICT
                ModeCardItem(
                    title = stringResource(R.string.focus_mode_strict_card_title),
                    desc = stringResource(R.string.focus_mode_strict_card_desc),
                    iconRes = R.drawable.ic_focus,
                    isSelected = isStrict,
                    onClick = { viewModel.onSelectMode(FocusMode.STRICT) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.selectedMode == FocusMode.STRICT) {
                val blockedCount = state.blockedApps.size
                val statusText = if (blockedCount == 0) {
                    stringResource(R.string.focus_blocked_apps_empty)
                } else {
                    stringResource(R.string.focus_blocked_apps_count, blockedCount)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Nemo.colors.surface)
                        .border(1.dp, Nemo.colors.line, RoundedCornerShape(12.dp))
                        .pressable(shape = RoundedCornerShape(12.dp)) { onManageBlockedApps() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Nemo.colors.fog),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock),
                                contentDescription = null,
                                tint = Nemo.colors.ink,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.focus_blocked_apps_title),
                                fontFamily = GeistFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Nemo.colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = statusText,
                                fontFamily = GeistFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = Nemo.colors.mute,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Nemo.colors.fog)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.focus_action_manage),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Nemo.colors.inkSecondary,
                        )
                    }
                }
            }

            if (state.selectedMode == FocusMode.STRICT && !state.blockingReady) {
                Text(
                    text = stringResource(R.string.focus_accessibility_enable_link),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Nemo.colors.ink,
                    modifier = Modifier
                        .clip(PillShape)
                        .pressable(shape = PillShape, onClick = onOpenAccessibilitySettings)
                        .padding(vertical = 4.dp),
                )
            }
        }

        // --- Soundscape Card (Minimalist) ---
        var showGenreDialog by remember { mutableStateOf(false) }

        if (showGenreDialog) {
            LofiGenreSelectionDialog(
                selectedGenre = state.selectedLofiGenre,
                onSelectGenre = { genre ->
                    viewModel.onSelectLofiGenre(genre)
                    showGenreDialog = false
                },
                onDismiss = { showGenreDialog = false },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(state.selectedLofiGenre) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount
                            },
                            onDragEnd = {
                                val genres = LofiGenre.ALL
                                val currentIndex = genres.indexOf(state.selectedLofiGenre).coerceAtLeast(0)
                                if (totalDragX > 30f) {
                                    val nextGenre = genres[(currentIndex + 1) % genres.size]
                                    viewModel.onSelectLofiGenre(nextGenre)
                                } else if (totalDragX < -30f) {
                                    val prevIndex = if (currentIndex - 1 < 0) genres.size - 1 else currentIndex - 1
                                    viewModel.onSelectLofiGenre(genres[prevIndex])
                                }
                            },
                        )
                    }
                    .clickable { showGenreDialog = true },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.focus_soundscape_lofi_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(state.selectedLofiGenre.titleRes),
                    fontFamily = GeistFamily,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                )
            }
            NemoSwitch(
                checked = state.soundscapeEnabled,
                onCheckedChange = viewModel::onToggleSoundscape,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Begin Button ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Nemo.colors.ink)
                .pressable(
                    shape = RoundedCornerShape(14.dp),
                    onClick = viewModel::onBegin,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "▶",
                    fontSize = 12.sp,
                    color = Nemo.colors.onInk,
                )
                Text(
                    text = stringResource(R.string.focus_action_start),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Nemo.colors.onInk,
                )
            }
        }
    }

    // Task selection dialog
    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            containerColor = Nemo.colors.surface,
            title = {
                Text(
                    text = stringResource(R.string.focus_picker_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Nemo.colors.ink,
                )
            },
            text = {
                if (state.taskOptions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.focus_picker_empty),
                        fontFamily = GeistFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.mute,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            items = state.taskOptions,
                            key = { it.id },
                            contentType = { "focus_task_option" },
                        ) { option ->
                            val isSelected = option.id == state.selectedTaskId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Nemo.colors.fog else Color.Transparent)
                                    .pressable(shape = RoundedCornerShape(8.dp)) {
                                        viewModel.onSelectTask(option.id)
                                        showTaskPicker = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Nemo.colors.ink else Nemo.colors.line),
                                )
                                val durationSuffix = option.estimatedDurationMin?.takeIf { it > 0 }?.let { " (${it}m)" } ?: ""
                                Text(
                                    text = "${option.title}$durationSuffix",
                                    fontFamily = GeistFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = Nemo.colors.ink,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) {
                    Text(
                        text = stringResource(R.string.focus_picker_close),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Nemo.colors.ink,
                    )
                }
            },
        )
    }
}

@Composable
private fun ModeCardItem(
    title: String,
    desc: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) Nemo.colors.fog else Nemo.colors.surface
    val strokeColor = if (isSelected) Nemo.colors.ink else Nemo.colors.line
    val strokeWidth = if (isSelected) 1.5.dp else 1.dp

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(strokeWidth, strokeColor, RoundedCornerShape(14.dp))
            .pressable(shape = RoundedCornerShape(14.dp), onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (isSelected) Nemo.colors.ink else Nemo.colors.mute,
                    modifier = Modifier.size(16.dp),
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Nemo.colors.ink else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Nemo.colors.ink else Nemo.colors.line,
                            shape = CircleShape,
                        ),
                )
            }
            Text(
                text = title,
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) Nemo.colors.ink else Nemo.colors.mute,
            )
        }
        Text(
            text = desc,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = if (isSelected) Nemo.colors.inkSecondary else Nemo.colors.placeholder,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// =========================================================================
// 2. FOCUS ACTIVE MODE (Frame vFsir) - Absolute #0A0A0A Black
// =========================================================================

@Composable
private fun FocusActiveContent(
    state: FocusUiState,
    viewModel: FocusViewModel,
    onFinished: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = state.active ?: return
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var finishCountdown by remember { mutableIntStateOf(5) }
    var showActiveGenreDialog by remember { mutableStateOf(false) }

    if (showActiveGenreDialog) {
        LofiGenreSelectionDialog(
            selectedGenre = state.selectedLofiGenre,
            onSelectGenre = { genre ->
                viewModel.onSelectLofiGenre(genre)
                showActiveGenreDialog = false
            },
            onDismiss = { showActiveGenreDialog = false },
        )
    }

    BackHandler(enabled = true) {
        showExitConfirm = true
    }

    LaunchedEffect(showFinishConfirm) {
        if (showFinishConfirm) {
            finishCountdown = 5
            while (finishCountdown > 0) {
                delay(1000L)
                finishCountdown -= 1
            }
        }
    }

    val activeBg = Color(0xFF0A0A0A)
    val cardBg = Color(0xFF141414)
    val borderCol = Color(0xFF26262E)
    val textPrimary = Color(0xFFFFFFFF)
    val textMute = Color(0xFF8A8A8A)
    val textFaint = Color(0xFF6B6B6B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(activeBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        // --- Session Header Row: Back Button (Left) + Mode Pill (Right) ---
        val isSoundActive = state.soundscapeEnabled && !active.paused
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, borderCol, CircleShape)
                    .clickable { showExitConfirm = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.detail_back),
                    tint = textPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Mode pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
                Text(
                    text = if (active.mode == FocusMode.STRICT) stringResource(R.string.focus_active_strict_pill) else stringResource(R.string.focus_active_mode_gentle),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = textPrimary,
                )
            }
        }

        // --- Intention Tag & Active Task Name ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.focus_active_intention_micro),
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = textFaint,
            )
            Text(
                text = active.taskTitle ?: stringResource(R.string.focus_task_default),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = textPrimary,
                textAlign = TextAlign.Center,
            )
        }

        // --- Endel Ring Box: Glowing Timer & Waveform ---
        val sessionMin = ((active.plannedDurationSec ?: 2700) / 60).coerceAtLeast(1)
        val timeString = formatClock(active.remainingMs ?: active.elapsedMs)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            ActiveTimerRing(
                progress = active.progressFraction,
                time = timeString,
                caption = if (active.paused) {
                    stringResource(R.string.focus_active_paused_label)
                } else {
                    stringResource(R.string.focus_active_remaining_label)
                },
                isPaused = active.paused,
                diameter = 220.dp,
            )

            // Sound Waveform Dots & Quick Mute / Switch Controller
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSoundActive) Color(0xFF161622) else Color(0xFF101014))
                    .border(
                        1.dp,
                        if (isSoundActive) Color(0xFF2C2C3E) else borderCol,
                        RoundedCornerShape(20.dp),
                    )
                    .pointerInput(state.selectedLofiGenre) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount
                            },
                            onDragEnd = {
                                val genres = LofiGenre.ALL
                                val currentIndex = genres.indexOf(state.selectedLofiGenre).coerceAtLeast(0)
                                if (totalDragX > 30f) {
                                    val nextGenre = genres[(currentIndex + 1) % genres.size]
                                    viewModel.onSelectLofiGenre(nextGenre)
                                } else if (totalDragX < -30f) {
                                    val prevIndex = if (currentIndex - 1 < 0) genres.size - 1 else currentIndex - 1
                                    viewModel.onSelectLofiGenre(genres[prevIndex])
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { viewModel.onToggleActiveSoundscape() },
                            onLongPress = { showActiveGenreDialog = true },
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val waveHeights = if (isSoundActive) WAVE_HEIGHTS_ACTIVE else WAVE_HEIGHTS_INACTIVE
                    val waveColors = if (isSoundActive) WAVE_COLORS_ACTIVE else WAVE_COLORS_INACTIVE
                    waveHeights.forEachIndexed { idx, h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h)
                                .clip(RoundedCornerShape(2.dp))
                                .background(waveColors[idx]),
                        )
                    }
                }

                Text(
                    text = if (isSoundActive) {
                        stringResource(state.selectedLofiGenre.titleRes)
                    } else {
                        stringResource(R.string.focus_soundscape_muted)
                    },
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (isSoundActive) Color(0xFFE2E2EC) else textFaint,
                )
            }
        }

        // --- Subtasks Checklist Card ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.focus_active_steps_header),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = textFaint,
            )

            if (active.subtasks.isEmpty()) {
                // If task has no subtasks, show calm intentional placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                    Text(
                        text = active.taskTitle ?: stringResource(R.string.focus_active_single_task_placeholder),
                        fontFamily = GeistFamily,
                        fontSize = 12.sp,
                        color = textPrimary,
                    )
                }
            } else {
                val firstIncompleteIndex = active.subtasks.indexOfFirst { !it.isDone }
                active.subtasks.forEachIndexed { idx, sub ->
                    val isFirstIncomplete = idx == firstIncompleteIndex
                    val itemBg = if (isFirstIncomplete && !sub.isDone) Color(0xFF19191E) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemBg)
                            .pressable(shape = RoundedCornerShape(8.dp)) { viewModel.toggleSubtask(sub) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Checkbox representation
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (sub.isDone) Color(0xFF26262E) else if (isFirstIncomplete) Color.White else textFaint,
                                    shape = CircleShape,
                                )
                                .background(if (sub.isDone) Color(0xFF26262E) else if (isFirstIncomplete) Color.White else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (sub.isDone) {
                                Text(
                                    text = "✓",
                                    fontSize = 10.sp,
                                    color = textMute,
                                )
                            } else if (isFirstIncomplete) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0A0A0A)),
                                )
                            }
                        }

                        Text(
                            text = sub.title,
                            fontFamily = GeistFamily,
                            fontWeight = if (isFirstIncomplete && !sub.isDone) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = if (isFirstIncomplete && !sub.isDone) 13.sp else 12.sp,
                            color = if (sub.isDone) textFaint else if (isFirstIncomplete) textPrimary else textMute,
                            textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // --- Action Controls Row: Pause / Complete ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Pause / Resume Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    .pressable(
                        shape = RoundedCornerShape(12.dp),
                        onClick = viewModel::onTogglePause,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (active.paused) "▶" else "❚❚",
                        fontSize = 12.sp,
                        color = Color(0xFFCFCFCF),
                    )
                    Text(
                        text = if (active.paused) stringResource(R.string.focus_active_resume) else stringResource(R.string.focus_active_pause),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFFCFCFCF),
                    )
                }
            }

            // Finish Button (Solid white)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .pressable(
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showFinishConfirm = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "✓",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0A0A),
                    )
                    Text(
                        text = stringResource(R.string.focus_active_finish),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0A0A0A),
                    )
                }
            }
        }

        // --- Emergency unblock (Grace 2m) ---
        Text(
            text = stringResource(R.string.focus_active_emergency_unblock),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(PillShape)
                .pressable(
                    shape = PillShape,
                    onClick = viewModel::onEmergencyExit,
                )
                .padding(vertical = 4.dp),
        )

        // Post-session feedback if present (Spec 2026-11-03)
        state.lastCompletion?.let { completion ->
            FocusCompletionCard(
                completion = completion,
                onRate = viewModel::onRateLastSession,
                onDismiss = viewModel::onDismissCompletion,
                darkTheme = true,
            )
        }
    }

    // Exit confirmation dialog (Back button pressed)
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            containerColor = Color(0xFF141414),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NemoMascot(
                        expression = MascotExpression.SAD,
                        size = 32.dp,
                    )
                    Text(
                        text = stringResource(R.string.focus_exit_dialog_title),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.focus_exit_dialog_body),
                    fontFamily = GeistFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF8A8A8A),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        viewModel.autoPauseFocus()
                        onBack()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.focus_exit_dialog_confirm),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.focus_exit_dialog_dismiss),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF6B6B6B),
                    )
                }
            },
        )
    }

    // Finish confirmation dialog (with 5-second countdown safety lock)
    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            containerColor = Color(0xFF141414),
            title = {
                Text(
                    text = stringResource(R.string.focus_finish_dialog_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.focus_finish_dialog_body),
                    fontFamily = GeistFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF8A8A8A),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (finishCountdown == 0) {
                            showFinishConfirm = false
                            viewModel.onFinish()
                            onFinished()
                        }
                    },
                    enabled = finishCountdown == 0,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = if (finishCountdown > 0) {
                            "${stringResource(R.string.focus_finish_dialog_confirm)} (${finishCountdown}s)"
                        } else {
                            stringResource(R.string.focus_finish_dialog_confirm)
                        },
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (finishCountdown == 0) Color.White else Color(0xFF6B6B6B),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinishConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.focus_finish_dialog_dismiss),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF6B6B6B),
                    )
                }
            },
        )
    }
}

/** Custom monochrome glowing countdown timer ring for Active mode with periodic mascot encouragement. */
@Composable
private fun ActiveTimerRing(
    progress: Float,
    time: String,
    caption: String,
    isPaused: Boolean = false,
    diameter: Dp = 220.dp,
    strokeWidth: Dp = 6.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = Motion.progressSpec(),
        label = "active-timer-ring-progress",
    )

    val expressions = remember {
        listOf(
            MascotExpression.HAPPY,
            MascotExpression.WINK,
            MascotExpression.THINKING,
            MascotExpression.TALKING,
            MascotExpression.LAUGHING,
            MascotExpression.NEUTRAL,
        )
    }

    var expressionIndex by remember { mutableIntStateOf(0) }
    var mascotShownUntilMs by remember { mutableLongStateOf(0L) }
    var isMascotVisible by remember { mutableStateOf(false) }

    // Periodic encouragement: triggers every 45s for 6s (calm and not too frequent)
    LaunchedEffect(isPaused) {
        if (isPaused) {
            mascotShownUntilMs = 0L
            return@LaunchedEffect
        }
        while (isActive) {
            delay(45_000L)
            expressionIndex = (expressionIndex + 1) % expressions.size
            mascotShownUntilMs = System.currentTimeMillis() + 6_000L
            delay(6_500L)
        }
    }

    LaunchedEffect(mascotShownUntilMs) {
        val remaining = mascotShownUntilMs - System.currentTimeMillis()
        if (remaining > 0) {
            isMascotVisible = true
            delay(remaining)
            isMascotVisible = false
        } else {
            isMascotVisible = false
        }
    }

    // Smooth, organic layer animations (soft bouncy enter, gentle ease-out collapse on exit)
    val mascotAlpha by animateFloatAsState(
        targetValue = if (isMascotVisible) 1f else 0f,
        animationSpec = if (isMascotVisible) {
            tween(durationMillis = 380, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 400, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        },
        label = "active-mascot-alpha",
    )

    val mascotScale by animateFloatAsState(
        targetValue = if (isMascotVisible) 1f else 0.55f,
        animationSpec = if (isMascotVisible) {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)
        } else {
            tween(durationMillis = 400, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        },
        label = "active-mascot-scale",
    )

    val clockAlpha by animateFloatAsState(
        targetValue = if (isMascotVisible) 0f else 1f,
        animationSpec = if (isMascotVisible) {
            tween(durationMillis = 200, easing = FastOutLinearInEasing)
        } else {
            tween(durationMillis = 380, delayMillis = 120, easing = FastOutSlowInEasing)
        },
        label = "active-clock-alpha",
    )

    val clockScale by animateFloatAsState(
        targetValue = if (isMascotVisible) 0.88f else 1f,
        animationSpec = if (isMascotVisible) {
            tween(durationMillis = 250, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 380, delayMillis = 100, easing = FastOutSlowInEasing)
        },
        label = "active-clock-scale",
    )

    Box(
        modifier = Modifier
            .size(diameter)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (isMascotVisible) {
                    mascotShownUntilMs = 0L
                } else {
                    expressionIndex = (expressionIndex + 1) % expressions.size
                    mascotShownUntilMs = System.currentTimeMillis() + 6_000L
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            // Background track
            drawArc(
                color = Color(0xFF19191E),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Progress active sweep
            if (animated > 0f) {
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = (360f * animated).coerceIn(0f, 360f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        // Clock Numbers
        if (clockAlpha > 0.001f) {
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = clockAlpha
                    scaleX = clockScale
                    scaleY = clockScale
                    transformOrigin = TransformOrigin.Center
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = time,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 44.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = caption,
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF8A8A8A),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Mascot inside active timer countdown ring with soft radial backlight
        if (mascotAlpha > 0.001f) {
            val circleSize = diameter - (strokeWidth * 2) - 4.dp
            val currentExpression = if (isPaused) {
                MascotExpression.SLEEPY
            } else {
                expressions[expressionIndex % expressions.size]
            }

            Box(
                modifier = Modifier
                    .size(circleSize)
                    .graphicsLayer {
                        alpha = mascotAlpha
                        scaleX = mascotScale
                        scaleY = mascotScale
                        transformOrigin = TransformOrigin.Center
                    },
                contentAlignment = Alignment.Center,
            ) {
                NemoMascot(
                    expression = currentExpression,
                    size = 172.dp,
                    paused = isPaused,
                )
            }
        }
    }
}

/** Format milliseconds into MM:SS. */
private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun FocusCompletionCard(
    completion: CompletionResult,
    onRate: (Int) -> Unit,
    onDismiss: () -> Unit,
    darkTheme: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cardBg = if (darkTheme) Color(0xFF141414) else Nemo.colors.surface
    val borderCol = if (darkTheme) Color(0xFF26262E) else Nemo.colors.line
    val textPrimary = if (darkTheme) Color.White else Nemo.colors.ink
    val textMute = if (darkTheme) Color.White.copy(alpha = 0.65f) else Nemo.colors.mute
    val rateItemBg = if (darkTheme) Color(0xFF262626) else Nemo.colors.fog

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NemoMascot(
                expression = if (completion.verified) MascotExpression.LAUGHING else MascotExpression.SAD,
                size = 32.dp,
            )
            Text(
                text = if (completion.verified) {
                    stringResource(R.string.focus_finished_verified, completion.focusMinutes, completion.taskTitle)
                } else {
                    stringResource(R.string.focus_finished_not_verified, completion.focusMinutes)
                },
                fontFamily = GeistFamily,
                fontSize = 13.sp,
                color = textPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .pressable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    fontSize = 12.sp,
                    color = textMute,
                )
            }
        }

        // Rating row for verified sessions or sessions >= 15 minutes (Spec 2026-11-03 §4.2)
        if (completion.verified || completion.focusMinutes >= 15) {
            if (completion.rating == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.focus_rating_prompt),
                        fontFamily = GeistFamily,
                        fontSize = 11.sp,
                        color = textMute,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { r ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(rateItemBg)
                                    .pressable(onClick = { onRate(r) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = r.toString(),
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textPrimary,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.focus_rating_thank_you),
                    fontFamily = GeistMonoFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50),
                )
            }
        }
    }
}

@Composable
private fun LofiGenreSelectionDialog(
    selectedGenre: LofiGenre,
    onSelectGenre: (LofiGenre) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.focus_lofi_dialog_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = Nemo.colors.ink,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(LofiGenre.ALL, key = { it.id }) { genre ->
                    val isSelected = genre == selectedGenre
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Nemo.colors.fog else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) Nemo.colors.ink else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelectGenre(genre) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(genre.titleRes),
                            fontFamily = GeistFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Nemo.colors.ink,
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Nemo.colors.ink else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isSelected) Nemo.colors.ink else Nemo.colors.line,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Nemo.colors.onInk),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.focus_picker_close),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Medium,
                    color = Nemo.colors.ink,
                )
            }
        },
        containerColor = Nemo.colors.surface,
        shape = RoundedCornerShape(16.dp),
    )
}

private val WAVE_HEIGHTS_ACTIVE = listOf(8.dp, 16.dp, 22.dp, 14.dp, 18.dp, 10.dp)
private val WAVE_HEIGHTS_INACTIVE = listOf(4.dp, 4.dp, 4.dp, 4.dp, 4.dp, 4.dp)
private val WAVE_COLORS_ACTIVE = listOf(
    Color(0xFF4A4A54),
    Color(0xFF8A8A96),
    Color(0xFFFFFFFF),
    Color(0xFF8A8A96),
    Color(0xFFCFCFCF),
    Color(0xFF4A4A54),
)
private val WAVE_COLORS_INACTIVE = listOf(
    Color(0xFF33333A),
    Color(0xFF33333A),
    Color(0xFF444450),
    Color(0xFF444450),
    Color(0xFF33333A),
    Color(0xFF33333A),
)
