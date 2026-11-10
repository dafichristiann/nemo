package com.anchor.app.feature.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.Banner
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.NemoSwitch
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.SegmentedChips
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.platform.accessibility.AccessibilityStatus
import com.anchor.app.platform.usagestats.UsageAccessSettings

/**
 * 00 - Splash & Onboarding (docs/superpowers/design/gamabaran.pen WCFeF).
 */
@Composable
fun WelcomeOnboardingScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_skip),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x8))

        // Avatar Card (Maskot Nemo)
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Nemo.colors.surface)
                .border(1.dp, Nemo.colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            com.anchor.app.core.design.components.NemoMascot(
                expression = com.anchor.app.core.design.components.MascotExpression.HAPPY,
                size = 110.dp,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x8))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_title_1),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_title_2),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                color = Nemo.colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))
            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Nemo.colors.inkSecondary,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            label = stringResource(R.string.onboarding_welcome_cta),
            onClick = onGetStarted,
        )
    }
}

/**
 * 00b Permission Onboarding (docs 07-screens.md §2.4, gamabaran.pen r4bPS8).
 *
 * Step indicator, progress track, two optional permission cards, a local-only privacy
 * card with trust mascot avatar, and Continue / Skip for now.
 */
@Composable
fun PermissionOnboardingScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accessibilityStatus = remember { AccessibilityStatus() }
    var notificationsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
        )
    }
    var usageGranted by remember { mutableStateOf(usageAccessGranted(context)) }
    var accessibilityGranted by remember { mutableStateOf(accessibilityStatus.isEnabled(context)) }
    var showBlockedDialog by remember { mutableStateOf(false) }

    // Usage access and Accessibility service granted in OS Settings, so re-check when we return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = usageAccessGranted(context)
                accessibilityGranted = accessibilityStatus.isEnabled(context)
                notificationsGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
    }

    val tryProceed = {
        if (usageGranted && accessibilityGranted) {
            onContinue()
        } else {
            showBlockedDialog = true
        }
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.onboarding_perm_required_dialog_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.onboarding_perm_required_dialog_message),
                    fontFamily = GeistFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Nemo.colors.inkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showBlockedDialog = false }) {
                    Text(
                        text = stringResource(R.string.onboarding_perm_required_dialog_dismiss),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Nemo.colors.ink,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBlockedDialog = false
                        onContinue()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip_for_now),
                        fontFamily = GeistFamily,
                        color = Nemo.colors.mute,
                    )
                }
            },
            containerColor = Nemo.colors.surface,
            shape = RoundedCornerShape(Radius.card),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(vertical = Spacing.x6),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        Text(
            text = stringResource(R.string.onboarding_perm_step_indicator),
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = Nemo.colors.mute,
        )
        ProgressTrack(progress = 1f)

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.onboarding_perm_headline_1),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = stringResource(R.string.onboarding_perm_headline_2),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                color = Nemo.colors.ink,
            )
        }
        Text(
            text = stringResource(R.string.onboarding_perm_sub),
            fontFamily = GeistFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Nemo.colors.inkSecondary,
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_perm_usage_title),
            subtitle = stringResource(R.string.onboarding_perm_usage_sub),
            granted = usageGranted,
            iconRes = R.drawable.ic_wellbeing,
            onEnable = { UsageAccessSettings.open(context) },
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_notif_title),
            subtitle = stringResource(R.string.onboarding_perm_notif_sub),
            granted = notificationsGranted,
            iconRes = R.drawable.ic_insights,
            onEnable = { requestNotifications(notifLauncher, context) },
            helperText = if (!notificationsGranted) {
                val isIndo = java.util.Locale.getDefault().language.let { it.startsWith("id") || it.startsWith("in") }
                if (isIndo) {
                    "Notifikasi dinonaktifkan. Pengingat dalam aplikasi (in-app reminder) akan digunakan sebagai alternatif."
                } else {
                    "Notifications disabled. In-app reminder will be used as a fallback."
                }
            } else null,
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_blocking_title),
            subtitle = stringResource(R.string.onboarding_perm_blocking_sub),
            granted = accessibilityGranted,
            iconRes = R.drawable.ic_focus,
            onEnable = { accessibilityStatus.open(context) },
        )

        val allPermissionsGranted = usageGranted && notificationsGranted && accessibilityGranted
        val permMascot = if (allPermissionsGranted) {
            com.anchor.app.core.design.components.MascotExpression.HAPPY
        } else {
            com.anchor.app.core.design.components.MascotExpression.SHY
        }

        Banner {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                com.anchor.app.core.design.components.NemoMascot(
                    expression = permMascot,
                    size = 32.dp,
                )
                Spacer(modifier = Modifier.width(Spacing.x3))
                Text(
                    text = stringResource(R.string.onboarding_privacy_card),
                    fontFamily = GeistFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x4))

        PrimaryButton(label = stringResource(R.string.onboarding_continue), onClick = tryProceed)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.onboarding_skip_for_now),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }
    }
}

/**
 * 00a Profile Setup (docs 07-screens.md Ã‚Â§2.3b).
 *
 * Collects the three fields we ask every new user for Ã¢â‚¬â€ a nickname (personalizes the mascot
 * greeting), a main focus area (College / Career / Personal Growth), and a default focus-session
 * length (25 / 45 / 60 min). Every field is optional and immediately persisted, so the user can
 * tap Continue/Skip at any point without losing what they typed.
 */
@Composable
fun ProfileSetupScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showWarningDialog by rememberSaveable { mutableStateOf(false) }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.onboarding_profile_dialog_title),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Nemo.colors.ink,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.onboarding_profile_dialog_message),
                    fontFamily = GeistFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Nemo.colors.inkSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEnableAllKeyFeatures()
                        showWarningDialog = false
                        onContinue()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_profile_dialog_enable_all),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Nemo.colors.ink,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text(
                        text = stringResource(R.string.onboarding_profile_dialog_adjust),
                        fontFamily = GeistFamily,
                        color = Nemo.colors.mute,
                    )
                }
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        onContinue()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_profile_dialog_continue_anyway),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.Medium,
                        color = Nemo.colors.inkSecondary,
                    )
                }
            },
            containerColor = Nemo.colors.surface,
            shape = RoundedCornerShape(Radius.card),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(vertical = Spacing.x6),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        Text(
            text = stringResource(R.string.onboarding_profile_step, 2, 3),
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
        ProgressTrack(progress = 2f / 3f)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val mascotExp = if (settings.nickname.isNotBlank()) {
                com.anchor.app.core.design.components.MascotExpression.HAPPY
            } else {
                com.anchor.app.core.design.components.MascotExpression.TALKING
            }
            com.anchor.app.core.design.components.NemoMascot(
                expression = mascotExp,
                size = 100.dp,
            )
        }

        Text(
            text = stringResource(R.string.onboarding_profile_headline),
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = stringResource(R.string.onboarding_profile_sub),
            fontFamily = GeistFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Nemo.colors.inkSecondary,
        )

        // Field 1 Ã¢â‚¬â€ nickname (personalizes the mascot greeting).
        SectionHeader(title = stringResource(R.string.onboarding_profile_nickname_label))
        NicknameField(
            value = settings.nickname,
            onValueChange = viewModel::onNicknameChange,
            hint = stringResource(R.string.onboarding_profile_nickname_hint),
        )

        // Field 2 Ã¢â‚¬â€ main focus area.
        SectionHeader(title = stringResource(R.string.onboarding_profile_focus_label))
        SegmentedChips(
            options = listOf(
                stringResource(R.string.onboarding_profile_focus_college),
                stringResource(R.string.onboarding_profile_focus_career),
                stringResource(R.string.onboarding_profile_focus_personal),
            ),
            selectedIndex = settings.focusArea.ordinal,
            onSelect = { viewModel.onSelectFocusArea(FocusArea.entries[it]) },
        )

        // Field 3 Ã¢â‚¬â€ default focus-session length (25 / 45 / 60; default 25).
        SectionHeader(title = stringResource(R.string.onboarding_profile_duration_label))
        SegmentedChips(
            options = AppSettings.FOCUS_DURATION_OPTIONS_MIN
                .map { stringResource(R.string.onboarding_profile_duration_value, it) },
            selectedIndex = AppSettings.FOCUS_DURATION_OPTIONS_MIN
                .indexOf(settings.defaultFocusDurationMin)
                .coerceAtLeast(0),
            onSelect = { viewModel.onSelectDuration(AppSettings.FOCUS_DURATION_OPTIONS_MIN[it]) },
        )

        // Field 4 — continuous doomscroll alert (optional).
        SectionHeader(title = stringResource(R.string.onboarding_profile_doomscroll_label))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                .background(Nemo.colors.surface)
                .padding(Spacing.cardHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x1),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_profile_doomscroll_toggle),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Nemo.colors.ink,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_profile_doomscroll_desc),
                        fontFamily = GeistFamily,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Nemo.colors.inkSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x2))
                NemoSwitch(
                    checked = settings.doomscrollAlertEnabled,
                    onCheckedChange = viewModel::onToggleDoomscroll,
                )
            }

            if (settings.doomscrollAlertEnabled) {
                SegmentedChips(
                    options = AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN
                        .map { stringResource(R.string.onboarding_profile_duration_value, it) },
                    selectedIndex = AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN
                        .indexOf(settings.doomscrollThresholdMinutes)
                        .coerceAtLeast(0),
                    onSelect = { index ->
                        viewModel.onSelectDoomscrollThreshold(
                            AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN[index]
                        )
                    },
                )
            }
        }

        // Field 5 — Key Features (Usage Tracking, AI Insights, Focus Blocking).
        SectionHeader(title = stringResource(R.string.onboarding_profile_features_label))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
                .background(Nemo.colors.surface)
                .padding(Spacing.cardHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            FeatureToggleRow(
                title = stringResource(R.string.onboarding_profile_tracking_title),
                subtitle = stringResource(R.string.onboarding_profile_tracking_desc),
                checked = settings.trackingEnabled,
                onCheckedChange = viewModel::onToggleTracking,
            )
            Divider()
            FeatureToggleRow(
                title = stringResource(R.string.onboarding_profile_ai_title),
                subtitle = stringResource(R.string.onboarding_profile_ai_desc),
                checked = settings.aiEnabled,
                onCheckedChange = viewModel::onToggleAi,
            )
            Divider()
            FeatureToggleRow(
                title = stringResource(R.string.onboarding_profile_focus_blocking_title),
                subtitle = stringResource(R.string.onboarding_profile_focus_blocking_desc),
                checked = settings.focusBlockingEnabled,
                onCheckedChange = viewModel::onToggleFocusBlocking,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x4))

        PrimaryButton(
            label = stringResource(R.string.onboarding_profile_continue),
            onClick = {
                if (!settings.trackingEnabled && !settings.aiEnabled && !settings.focusBlockingEnabled) {
                    showWarningDialog = true
                } else {
                    onContinue()
                }
            },
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.onboarding_profile_skip),
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                text = subtitle,
                fontFamily = GeistFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Nemo.colors.inkSecondary,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.x2))
        NemoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Single-line nickname input built on Material3 [OutlinedTextField] (already on the classpath
 * via the Compose BOM Ã¢â‚¬â€ no extra dependency). Styled to match the App's soft inputs: sunken
 * background, thin border, Inter font, and a rounded field shape. `singleLine` + IME "Done"
 * dismisses the keyboard on submit.
 */
@Composable
private fun NicknameField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    // Local editing buffer: binding straight to the persisted (and trimmed) settings value dropped
    // characters and jumped the cursor, because each keystroke round-tripped through the store.
    var text by rememberSaveable { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != text.trim()) text = value
    }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            val capped = new.take(MAX_NICKNAME_LENGTH)
            text = capped
            onValueChange(capped)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = hint,
                fontFamily = GeistFamily,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = GeistFamily,
            fontSize = 14.sp,
            color = Nemo.colors.ink,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        shape = RoundedCornerShape(Radius.field),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Nemo.colors.fog,
            unfocusedContainerColor = Nemo.colors.fog,
            focusedBorderColor = Nemo.colors.ink,
            unfocusedBorderColor = Nemo.colors.line,
            cursorColor = Nemo.colors.ink,
        ),
    )
}

/** Nickname is a greeting, not data Ã¢â‚¬â€ cap it so the header never overflows. */
private const val MAX_NICKNAME_LENGTH = 24

/** Reads the PACKAGE_USAGE_STATS AppOps state (special access, granted in Settings). */
private fun usageAccessGranted(context: Context): Boolean = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.packageName)
    mode == AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)
private fun requestNotifications(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        openNotificationSettings(context)
    }
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    granted: Boolean,
    onEnable: () -> Unit,
    iconRes: Int? = null,
    helperText: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .border(1.dp, Nemo.colors.line, RoundedCornerShape(Radius.card))
            .background(Nemo.colors.surface)
            .padding(Spacing.cardHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Nemo.colors.fog),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Nemo.colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x3))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                Text(text = title, fontFamily = GeistFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nemo.colors.ink)
                Text(text = subtitle, fontFamily = GeistFamily, fontSize = 12.sp, color = Nemo.colors.mute)
            }
            Spacer(modifier = Modifier.width(Spacing.x2))
            if (granted) {
                NemoPill(text = stringResource(R.string.onboarding_perm_enabled), filled = true)
            } else {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Nemo.colors.ink)
                        .clickable(onClick = onEnable)
                        .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_perm_enable),
                        fontFamily = GeistFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Nemo.colors.onInk,
                    )
                }
            }
        }
        if (helperText != null) {
            Text(
                text = helperText,
                fontFamily = GeistFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Nemo.colors.mute,
            )
        }
    }
}
