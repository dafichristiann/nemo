package com.anchor.app.feature.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.anchor.app.core.design.components.MascotExpression
import com.anchor.app.core.design.components.NemoMascot
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSwitch
import com.anchor.app.core.design.components.WheelPickerDialog
import com.anchor.app.core.design.components.formatDurationSeconds
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.FrauncesFamily
import com.anchor.app.core.design.theme.ManropeFamily
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
    var soundscapeOn by remember { mutableStateOf(true) }
    var durationDialogOpen by remember { mutableStateOf(false) }

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
        NemoScreenHeader(
            eyebrow = "INTENTIONAL CALIBRATION",
            title = "Focus Session",
            subtitle = "Konfigurasi lingkungan fokus sebelum mulai",
            action = {
                NemoMascot(
                    expression = MascotExpression.NEUTRAL,
                    size = 32.dp,
                )
            },
        )

        // --- Task Section ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FOKUS PADA TUGAS",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.mute,
            )

            val selectedTask = state.selectedTask
            val taskTitle = selectedTask?.title ?: if (state.taskOptions.isNotEmpty()) state.taskOptions.first().title else "Draft Chapter 3 methodology"
            val taskMeta = selectedTask?.let {
                "Prioritas: ${it.priority.name.lowercase().replaceFirstChar { c -> c.uppercase() }}"
            } ?: "Tugas pilihan utama"

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
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Nemo.colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = taskMeta,
                            fontFamily = ManropeFamily,
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
                        text = "Ganti",
                        fontFamily = ManropeFamily,
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
                text = "TINGKAT PROTEKSI",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.mute,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Gentle Mode Card
                val isGentle = state.selectedMode == FocusMode.GENTLE
                ModeCardItem(
                    title = "Gentle Awareness",
                    desc = "Notifikasi lembut saat terdistraksi",
                    iconRes = R.drawable.ic_wellbeing,
                    isSelected = isGentle,
                    onClick = { viewModel.onSelectMode(FocusMode.GENTLE) },
                    modifier = Modifier.weight(1f),
                )

                // Strict Mode Card
                val isStrict = state.selectedMode == FocusMode.STRICT
                ModeCardItem(
                    title = "Strict Blocking",
                    desc = "Aplikasi hiburan diblokir total",
                    iconRes = R.drawable.ic_focus,
                    isSelected = isStrict,
                    onClick = { viewModel.onSelectMode(FocusMode.STRICT) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Shield Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Nemo.colors.surface)
                    .border(1.dp, Nemo.colors.line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = Nemo.colors.inkSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Pemblokiran dikelola lokal oleh Android Accessibility Service.",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }

            if (state.selectedMode == FocusMode.STRICT && !state.blockingReady) {
                Text(
                    text = "Aktifkan Accessibility Service untuk pemblokiran ->",
                    fontFamily = ManropeFamily,
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

        // --- Duration Section ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "DURASI TARGET",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = Nemo.colors.mute,
            )

            val presetDurations = listOf(25 * 60, 45 * 60, 60 * 60)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presetDurations.forEach { durSec ->
                    val isSelected = state.selectedDurationSec == durSec
                    val bg = if (isSelected) Nemo.colors.ink else Nemo.colors.surface
                    val fg = if (isSelected) Nemo.colors.onInk else Nemo.colors.mute
                    val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, Nemo.colors.line, RoundedCornerShape(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .then(borderModifier)
                            .pressable(
                                shape = RoundedCornerShape(10.dp),
                                onClick = { viewModel.onSelectDuration(durSec) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${durSec / 60}m",
                            fontFamily = ManropeFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = fg,
                        )
                    }
                }

                // Custom Chip
                val isCustom = state.selectedDurationSec !in presetDurations
                val bg = if (isCustom) Nemo.colors.ink else Nemo.colors.surface
                val fg = if (isCustom) Nemo.colors.onInk else Nemo.colors.mute
                val borderModifier = if (isCustom) Modifier else Modifier.border(1.dp, Nemo.colors.line, RoundedCornerShape(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .then(borderModifier)
                        .pressable(
                            shape = RoundedCornerShape(10.dp),
                            onClick = { durationDialogOpen = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isCustom) "${state.selectedDurationSec / 60}m" else "Custom",
                        fontFamily = ManropeFamily,
                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = fg,
                    )
                }
            }

            // Circadian Hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wellbeing),
                    contentDescription = null,
                    tint = Nemo.colors.mute,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Sesi 45m selaras dengan jendela fokus optimal saat ini.",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Nemo.colors.placeholder,
                )
            }
        }

        // --- Soundscape Card ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_focus),
                    contentDescription = null,
                    tint = Nemo.colors.inkSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Binaural Brown Noise (40Hz)",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }
            NemoSwitch(
                checked = soundscapeOn,
                onCheckedChange = { soundscapeOn = it },
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
                    text = "Mulai Sesi Fokus",
                    fontFamily = ManropeFamily,
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
                    text = "Pilih Tugas Fokus",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Nemo.colors.ink,
                )
            },
            text = {
                if (state.taskOptions.isEmpty()) {
                    Text(
                        text = "Tidak ada tugas aktif yang tersedia.",
                        fontFamily = ManropeFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.mute,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.taskOptions) { option ->
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
                                Text(
                                    text = option.title,
                                    fontFamily = ManropeFamily,
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
                        text = "Tutup",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Nemo.colors.ink,
                    )
                }
            },
        )
    }

    // Custom duration wheel dialog
    if (durationDialogOpen) {
        val durationIndex = FOCUS_DURATION_STEPS_SEC
            .indexOf(state.selectedDurationSec)
            .let { if (it >= 0) it else FOCUS_DURATION_STEPS_SEC.indexOf(FOCUS_DEFAULT_DURATION_SEC) }
            .coerceAtLeast(0)
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
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(strokeWidth, strokeColor, RoundedCornerShape(14.dp))
            .pressable(shape = RoundedCornerShape(14.dp), onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
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
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Nemo.colors.ink),
                )
            }
        }
        Text(
            text = title,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isSelected) Nemo.colors.ink else Nemo.colors.mute,
        )
        Text(
            text = desc,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = if (isSelected) Nemo.colors.inkSecondary else Nemo.colors.placeholder,
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
    modifier: Modifier = Modifier,
) {
    val active = state.active ?: return
    var showFinishConfirm by remember { mutableStateOf(false) }

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
        // --- Session Header Row: Strict Mode Pill + Soundscape Pill ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
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
                    text = if (active.mode == FocusMode.STRICT) "STRICT FOCUS" else "GENTLE FLOW",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = textPrimary,
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_focus),
                    contentDescription = null,
                    tint = textMute,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "432Hz Brown Noise",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = textMute,
                )
            }
        }

        // --- Intention Tag & Active Task Name ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "INTENTION",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = textFaint,
            )
            Text(
                text = active.taskTitle ?: "Draft Chapter 3 methodology",
                fontFamily = FrauncesFamily,
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
                caption = "remaining",
                diameter = 220.dp,
            )

            // Sound Waveform Dots (6 vertical wave bars)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val waveHeights = listOf(8.dp, 16.dp, 22.dp, 14.dp, 18.dp, 10.dp)
                val waveColors = listOf(
                    Color(0xFF4A4A54),
                    Color(0xFF8A8A96),
                    Color(0xFFFFFFFF),
                    Color(0xFF8A8A96),
                    Color(0xFFCFCFCF),
                    Color(0xFF4A4A54),
                )
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
                text = "INTENTIONAL SUB-STEPS",
                fontFamily = ManropeFamily,
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
                        text = active.taskTitle ?: "Fokus penuh pada tugas utama",
                        fontFamily = ManropeFamily,
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
                            fontFamily = ManropeFamily,
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
                        text = if (active.paused) "Resume" else "Pause",
                        fontFamily = ManropeFamily,
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
                        text = "Finish",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0A0A0A),
                    )
                }
            }
        }

        // --- Emergency unblock (Grace 2m) ---
        Text(
            text = "Emergency unblock (Grace 2m)",
            fontFamily = ManropeFamily,
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

        // Post-session feedback if present
        state.lastCompletion?.let { completion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NemoMascot(
                    expression = if (completion.verified) MascotExpression.LAUGHING else MascotExpression.SAD,
                    size = 28.dp,
                )
                Text(
                    text = if (completion.verified) {
                        stringResource(R.string.focus_finished_verified, completion.taskTitle, completion.focusMinutes)
                    } else {
                        stringResource(R.string.focus_finished_not_verified, completion.focusMinutes)
                    },
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    color = textPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Finish confirmation dialog
    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            containerColor = Color(0xFF141414),
            title = {
                Text(
                    text = "Selesaikan Sesi?",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = "Menyelesaikan sesi akan menandai tugas ini selesai.",
                    fontFamily = ManropeFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF8A8A8A),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirm = false
                    viewModel.onFinish()
                    onFinished()
                }) {
                    Text(
                        text = "Selesai",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) {
                    Text(
                        text = "Lanjut Fokus",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF6B6B6B),
                    )
                }
            },
        )
    }
}

/** Custom monochrome glowing countdown timer ring for Active mode. */
@Composable
private fun ActiveTimerRing(
    progress: Float,
    time: String,
    caption: String,
    diameter: Dp = 220.dp,
    strokeWidth: Dp = 6.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = Motion.progressSpec(),
        label = "active-timer-ring-progress",
    )

    Box(
        modifier = Modifier.size(diameter),
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
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = time,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Light,
                fontSize = 44.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = caption,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = Color(0xFF8A8A8A),
                textAlign = TextAlign.Center,
            )
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
