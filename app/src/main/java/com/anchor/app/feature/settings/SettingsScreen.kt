package com.anchor.app.feature.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.BuildConfig
import com.anchor.app.R
import com.anchor.app.platform.ai.AiPreferences
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSwitch
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.SegmentedChips
import com.anchor.app.core.design.components.minTouchTarget
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoNight
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.platform.usagestats.UsageAccessSettings

/**
 * 09 Settings & Privacy (docs/superpowers/design/07-screens.md Â§2.13).
 *
 * Deliberately short: toggles (FR-S6), live permission status with shortcuts (FR-S1), and
 * compact summaries that link to the dedicated sub-screens for the long lists â€” app
 * categories (FR-S3) and blocked apps (FR-S4) â€” plus data controls (FR-S5).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    onManageBlocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMascotShowcase by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Re-check OS permission state whenever we return to this screen (FR-S1, docs 08 Â§2 flow).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask for notifications first; if the OS refuses, guide the user to the settings screen.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshPermissions()
        if (!granted) openNotificationSettings(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        val isDark = Nemo.colors.background == NemoNight.background
        val isIndo = settings.locale.startsWith("id", ignoreCase = true) || settings.locale.startsWith("in", ignoreCase = true)

        NemoScreenHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            action = {
                // Single Theme Mode Toggle Button (Alternating Sun / Moon)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Nemo.colors.surface)
                        .border(1.dp, Nemo.colors.line, CircleShape)
                        .pressable(shape = CircleShape, pressedScale = 0.90f) {
                            viewModel.toggleTheme(isDark)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode,
                        ),
                        contentDescription = stringResource(
                            if (isDark) R.string.settings_theme_toggle_light else R.string.settings_theme_toggle_dark,
                        ),
                        tint = Nemo.colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )

        // --- Profile (nickname · main focus · default focus length) ---
        SectionHeader(title = stringResource(R.string.settings_section_profile))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(
                text = stringResource(R.string.settings_profile_nickname_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
            SettingsNicknameField(
                value = settings.nickname,
                onValueChange = viewModel::setNickname,
                hint = stringResource(R.string.settings_profile_nickname_hint),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Text(
                text = stringResource(R.string.settings_profile_focus_area_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
            SegmentedChips(
                options = listOf(
                    stringResource(R.string.onboarding_profile_focus_college),
                    stringResource(R.string.onboarding_profile_focus_career),
                    stringResource(R.string.onboarding_profile_focus_personal),
                ),
                selectedIndex = settings.focusArea.ordinal,
                onSelect = { viewModel.setFocusArea(FocusArea.entries[it]) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Text(
                text = stringResource(R.string.settings_profile_duration_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
            SegmentedChips(
                options = AppSettings.FOCUS_DURATION_OPTIONS_MIN
                    .map { stringResource(R.string.settings_profile_duration_value, it) },
                selectedIndex = AppSettings.FOCUS_DURATION_OPTIONS_MIN
                    .indexOf(settings.defaultFocusDurationMin)
                    .coerceAtLeast(0),
                onSelect = { viewModel.setDefaultFocusDurationMin(AppSettings.FOCUS_DURATION_OPTIONS_MIN[it]) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            Text(
                text = stringResource(R.string.settings_profile_language_title),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
            SegmentedChips(
                options = listOf(
                    stringResource(R.string.settings_language_en),
                    stringResource(R.string.settings_language_id),
                ),
                selectedIndex = if (isIndo) 1 else 0,
                onSelect = { index ->
                    viewModel.setLocale(if (index == 1) "id" else "en")
                },
            )
        }

        Divider()

        // --- Permissions (FR-S1) ---
        SectionHeader(title = stringResource(R.string.settings_section_permissions))
        PermissionRow(
            title = stringResource(R.string.settings_perm_usage_title),
            description = stringResource(R.string.settings_perm_usage_desc),
            granted = permissions.usageAccessGranted,
            onEnable = { UsageAccessSettings.open(context) },
        )
        PermissionRow(
            title = stringResource(R.string.settings_perm_notif_title),
            description = stringResource(R.string.settings_perm_notif_desc),
            granted = permissions.notificationsGranted,
            onEnable = { requestNotifications(notifLauncher, context) },
        )
        PermissionRow(
            title = stringResource(R.string.settings_perm_accessibility_title),
            description = stringResource(R.string.settings_perm_accessibility_desc),
            granted = permissions.focusBlockingGranted,
            onEnable = { viewModel.onOpenAccessibilitySettings() },
        )

        Divider()

        // --- Notifications (Pengingat & Notifikasi) ---
        SectionHeader(title = if (isIndo) "Pengingat & Notifikasi" else "Notifications")
        SettingRow(
            title = if (isIndo) "Pengingat & Notifikasi" else "Notifications",
            subtitle = if (isIndo) "Aktifkan notifikasi untuk pengingat tugas dan intervensi fokus." else "Enable notifications for task reminders and focus interventions.",
            checked = settings.notificationsEnabled,
            onCheckedChange = viewModel::setNotificationsEnabled,
        )

        Divider()

        // --- Privacy & AI (FR-S2, FR-S6) ---
        SectionHeader(title = stringResource(R.string.settings_section_privacy))
        SettingRow(
            title = stringResource(R.string.settings_ai_title),
            subtitle = stringResource(R.string.settings_ai_subtitle),
            checked = settings.aiEnabled,
            onCheckedChange = viewModel::setAiEnabled,
        )

        // BYOK: Runtime Gemini API Key (OWASP Mobile M1 / M9)
        val hasCustomKey = customApiKey.isNotBlank()
        val hasBuiltinKey = BuildConfig.GEMINI_API_KEY.isNotBlank()
        val apiKeySummary = when {
            hasCustomKey -> stringResource(R.string.settings_ai_api_key_custom_active, AiPreferences.maskKey(customApiKey))
            hasBuiltinKey -> stringResource(R.string.settings_ai_api_key_builtin_active)
            else -> stringResource(R.string.settings_ai_api_key_none)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                .background(Nemo.colors.surface)
                .padding(Spacing.cardHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_ai_api_key_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = apiKeySummary,
                    fontFamily = GeistFamily,
                    fontSize = 12.sp,
                    color = if (hasCustomKey || hasBuiltinKey) Nemo.colors.inkSecondary else Nemo.colors.mute,
                )
            }
            Spacer(modifier = Modifier.width(Spacing.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                if (hasCustomKey) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .border(1.dp, Nemo.colors.dangerLine, PillShape)
                            .pressable(shape = PillShape, onClick = { viewModel.setCustomApiKey("") })
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x1),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_ai_api_key_clear_action),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Nemo.colors.danger,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Nemo.colors.ink)
                        .pressable(shape = PillShape, onClick = { showApiKeyDialog = true })
                        .padding(horizontal = Spacing.x3, vertical = Spacing.x1),
                ) {
                    Text(
                        text = stringResource(
                            if (hasCustomKey) R.string.settings_ai_api_key_edit_action
                            else R.string.settings_ai_api_key_set_action
                        ),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Nemo.colors.surface,
                    )
                }
            }
        }

        SettingRow(
            title = stringResource(R.string.settings_tracking_title),
            subtitle = stringResource(R.string.settings_tracking_subtitle),
            checked = settings.trackingEnabled,
            onCheckedChange = viewModel::setTrackingEnabled,
        )

        Divider()

        // --- App categories summary (FR-S3) â†’ sub-screen ---
        SectionHeader(title = stringResource(R.string.settings_section_categories))
        ManageRow(
            summary = if (categories.isEmpty()) {
                stringResource(R.string.settings_categories_empty)
            } else {
                stringResource(R.string.settings_categories_summary, categories.size)
            },
            onManage = onManageCategories,
        )

        Divider()

        // --- Blocked apps summary (FR-S4) â†’ sub-screen ---
        SectionHeader(title = stringResource(R.string.settings_section_blocked))
        ManageRow(
            summary = if (settings.blockedApps.isEmpty()) {
                stringResource(R.string.settings_blocked_summary_none)
            } else {
                stringResource(R.string.settings_blocked_summary, settings.blockedApps.size)
            },
            onManage = onManageBlocked,
        )

        // --- Focus blocking opt-in (FR-F4/F8, PRD v0.4 Â§10.5) ---
        Divider()
        SectionHeader(title = stringResource(R.string.settings_section_focus_blocking))
        SettingRow(
            title = stringResource(R.string.settings_focus_blocking_title),
            subtitle = stringResource(R.string.settings_focus_blocking_subtitle),
            checked = settings.focusBlockingEnabled,
            onCheckedChange = viewModel::setFocusBlockingEnabled,
        )

        // --- Continuous doomscroll alert (specs 2026-11-01) ---
        Divider()
        SectionHeader(title = stringResource(R.string.settings_section_doomscroll))
        SettingRow(
            title = stringResource(R.string.settings_doomscroll_title),
            subtitle = stringResource(R.string.settings_doomscroll_subtitle),
            checked = settings.doomscrollAlertEnabled,
            onCheckedChange = { enabled ->
                viewModel.setDoomscrollAlertEnabled(enabled)
                if (enabled && !permissions.focusBlockingGranted) {
                    viewModel.onOpenAccessibilitySettings()
                }
            },
        )
        if (settings.doomscrollAlertEnabled) {
            if (!permissions.focusBlockingGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.card))
                        .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                        .background(Nemo.colors.fog)
                        .pressable(
                            shape = RoundedCornerShape(Radius.card),
                            onClick = viewModel::onOpenAccessibilitySettings,
                        )
                        .padding(Spacing.cardHorizontal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_doomscroll_perm_warning),
                            fontFamily = GeistFamily,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Nemo.colors.ink,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Nemo.colors.ink)
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x1),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_doomscroll_perm_action),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Nemo.colors.surface,
                        )
                    }
                }
            }

            ManageRow(
                summary = if (settings.blockedApps.isEmpty()) {
                    stringResource(R.string.settings_doomscroll_apps_summary_none)
                } else {
                    stringResource(R.string.settings_doomscroll_apps_summary, settings.blockedApps.size)
                },
                onManage = onManageBlocked,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                    Text(
                        text = stringResource(R.string.settings_doomscroll_duration_title),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Nemo.colors.ink,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_doomscroll_duration_summary,
                            settings.doomscrollThresholdMinutes,
                        ),
                        fontFamily = GeistFamily,
                        fontSize = 12.sp,
                        color = Nemo.colors.mute,
                    )
                }
                SegmentedChips(
                    options = AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN
                        .map { stringResource(R.string.settings_profile_duration_value, it) },
                    selectedIndex = AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN
                        .indexOf(settings.doomscrollThresholdMinutes)
                        .coerceAtLeast(0),
                    onSelect = { index ->
                        viewModel.setDoomscrollThresholdMinutes(
                            AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN[index]
                        )
                    },
                )
            }
        }

        Divider()

        // --- Data controls (FR-S5) ---
        SectionHeader(title = stringResource(R.string.settings_section_data))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                .background(Nemo.colors.surface)
                .padding(Spacing.cardHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.settings_retention_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.settings_retention_label, settings.usageRetentionDays),
                    fontFamily = GeistFamily,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                )
            }
            Text(
                text = "›",
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Nemo.colors.mute,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .border(1.dp, Nemo.colors.dangerLine, PillShape)
                    .pressable(shape = PillShape, onClick = { showDeleteConfirmDialog = true })
                    .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
            ) {
                Text(
                    text = stringResource(R.string.settings_delete_all),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Nemo.colors.danger,
                )
            }
            com.anchor.app.core.design.components.NemoMascot(
                expression = com.anchor.app.core.design.components.MascotExpression.SAD,
                size = 28.dp,
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = Nemo.colors.surface,
                title = {
                    Text(
                        text = stringResource(R.string.settings_delete_confirm_title),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Nemo.colors.ink,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.settings_delete_confirm_body),
                        fontFamily = GeistFamily,
                        fontSize = 13.sp,
                        color = Nemo.colors.mute,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.onDeleteAllData()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.settings_delete_confirm_action),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Bold,
                            color = Nemo.colors.danger,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(
                            text = stringResource(R.string.dialog_cancel),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Medium,
                            color = Nemo.colors.mute,
                        )
                    }
                },
            )
        }

        if (showApiKeyDialog) {
            var tempKey by rememberSaveable { mutableStateOf(customApiKey) }
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                containerColor = Nemo.colors.surface,
                title = {
                    Text(
                        text = stringResource(R.string.settings_ai_api_key_dialog_title),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Nemo.colors.ink,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                        Text(
                            text = stringResource(R.string.settings_ai_api_key_dialog_desc),
                            fontFamily = GeistFamily,
                            fontSize = 13.sp,
                            color = Nemo.colors.mute,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(Radius.field))
                                .background(Nemo.colors.fog)
                                .padding(horizontal = Spacing.x3),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (tempKey.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.settings_ai_api_key_hint),
                                    fontFamily = GeistFamily,
                                    fontSize = 13.sp,
                                    color = Nemo.colors.mute,
                                )
                            }
                            BasicTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it.trim() },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = GeistMonoFamily,
                                    fontSize = 13.sp,
                                    color = Nemo.colors.ink,
                                ),
                                cursorBrush = SolidColor(Nemo.colors.ink),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setCustomApiKey(tempKey)
                            showApiKeyDialog = false
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_set),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Bold,
                            color = Nemo.colors.ink,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text(
                            text = stringResource(R.string.dialog_cancel),
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Medium,
                            color = Nemo.colors.mute,
                        )
                    }
                },
            )
        }

        Divider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            com.anchor.app.core.design.components.NemoMascot(
                expression = com.anchor.app.core.design.components.MascotExpression.HAPPY,
                size = 28.dp,
            )
            Text(
                text = stringResource(R.string.settings_assure_full),
                fontFamily = GeistFamily,
                fontSize = 12.sp,
                color = Nemo.colors.mute,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x4))

        // About Nemo Profile Card (gambaran.pen dJPpZ)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                .pressable(shape = RoundedCornerShape(Radius.card), onClick = { showMascotShowcase = true })
                .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            com.anchor.app.core.design.components.NemoMascot(
                expression = com.anchor.app.core.design.components.MascotExpression.HAPPY,
                size = 40.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_version_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Nemo.colors.ink,
                )
                Text(
                    text = stringResource(R.string.settings_mission_sub),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Nemo.colors.mute,
                )
            }
            Text(
                text = "Showcase ↗",
                fontFamily = GeistMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Nemo.colors.inkSecondary,
            )
        }

        if (showMascotShowcase) {
            com.anchor.app.core.design.components.NemoMascotShowcaseDialog(
                onDismiss = { showMascotShowcase = false },
            )
        }
    }
}

/**
 * Soft single-line nickname input matching the app's other fields (AddTaskSheet §Field)
 * with a confirmation checkmark button on the trailing end.
 */
@Composable
private fun SettingsNicknameField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf(value) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(value) {
        if (value != text.trim()) text = value
    }

    val confirm: () -> Unit = {
        val trimmed = text.trim()
        onValueChange(trimmed)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val isModified = text.trim() != value.trim()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(Radius.field))
            .background(Nemo.colors.fog)
            .padding(horizontal = Spacing.x4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (text.isEmpty()) {
                Text(
                    text = hint,
                    fontFamily = GeistFamily,
                    fontSize = 14.sp,
                    color = Nemo.colors.mute,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { new ->
                    text = new.take(AppSettings.MAX_NICKNAME_LENGTH)
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = GeistFamily,
                    fontSize = 14.sp,
                    color = Nemo.colors.ink,
                ),
                cursorBrush = SolidColor(Nemo.colors.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Confirmation checkmark button at the trailing end
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isModified && text.isNotBlank()) Nemo.colors.ink else Nemo.colors.surface)
                .border(
                    width = 1.dp,
                    color = if (isModified && text.isNotBlank()) Nemo.colors.ink else Nemo.colors.line,
                    shape = CircleShape,
                )
                .pressable(shape = CircleShape, onClick = { confirm() }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tasks),
                contentDescription = stringResource(R.string.settings_confirm_nickname),
                tint = if (isModified && text.isNotBlank()) Nemo.colors.onInk else Nemo.colors.ink,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Requests POST_NOTIFICATIONS on 33+; on older versions it is granted by default. */
private fun requestNotifications(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context,
) {    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        openNotificationSettings(context)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            runCatching {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.fromParts("package", context.packageName, null))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onEnable: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
            .background(Nemo.colors.surface)
            .pressable(
                shape = RoundedCornerShape(Radius.card),
                onClick = onEnable,
            )
            .padding(Spacing.cardHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = title,
                fontFamily = GeistFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = if (granted) stringResource(R.string.settings_p1_granted) else stringResource(R.string.settings_p2_off),
                fontFamily = GeistFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Nemo.colors.mute,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.x3))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(PillShape)
                .background(if (granted) Nemo.colors.ink else Nemo.colors.line),
        )
    }
}

/** Compact row: a one-line summary plus a [Manage] affordance that opens a sub-screen. */
@Composable
private fun ManageRow(summary: String, onManage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.field))
            .pressable(shape = RoundedCornerShape(Radius.field), onClick = onManage)
            .padding(vertical = Spacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            fontFamily = GeistFamily,
            fontSize = 13.sp,
            color = Nemo.colors.inkSecondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(Spacing.x3))
        Text(
            text = stringResource(R.string.settings_manage),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Nemo.colors.ink,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(text = title, fontFamily = GeistFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Nemo.colors.ink)
            Text(text = subtitle, fontFamily = GeistFamily, fontSize = 12.sp, color = Nemo.colors.mute)
        }
        NemoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
