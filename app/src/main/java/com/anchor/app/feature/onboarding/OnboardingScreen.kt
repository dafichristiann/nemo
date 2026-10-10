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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.anchor.app.core.design.components.NemoPill
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.SegmentedChips
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.platform.accessibility.AccessibilityStatus
import com.anchor.app.platform.usagestats.UsageAccessSettings

/**
 * 00b Permission Onboarding (docs 07-screens.md Ã‚Â§2.4).
 *
 * Step indicator, progress track, two optional permission cards, a local-only privacy
 * card, and Continue / Skip. Permissions are never forced; both cards are informational
 * in Sprint 1 and get live status wiring alongside Wellbeing (Sprint 2).
 */
@Composable
fun PermissionOnboardingScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
        )
    }
    var usageGranted by remember { mutableStateOf(false) }
    var blockingGranted by remember { mutableStateOf(AccessibilityStatus().isEnabled(context)) }

    // Usage access + App blocking are granted only in OS Settings, so re-check when we return
    // (docs 08 Ã‚Â§2).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = usageAccessGranted(context)
                blockingGranted = AccessibilityStatus().isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask for notifications here (the "enable notifications" step most apps use first); if the
    // user declines, guide them to the notification settings screen.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        if (!granted) openNotificationSettings(context)
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
            text = stringResource(R.string.onboarding_perm_step, 3, 3),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
        ProgressTrack(progress = 1f)

        Text(
            text = stringResource(R.string.onboarding_perm_headline),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = stringResource(R.string.onboarding_perm_sub),
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Nemo.colors.inkSecondary,
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_perm_usage_title),
            subtitle = stringResource(R.string.onboarding_perm_usage_sub),
            granted = usageGranted,
            onEnable = { UsageAccessSettings.open(context) },
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_notif_title),
            subtitle = stringResource(R.string.onboarding_perm_notif_sub),
            granted = notificationsGranted,
            onEnable = { requestNotifications(notifLauncher, context) },
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_blocking_title),
            subtitle = stringResource(R.string.onboarding_perm_blocking_sub),
            granted = blockingGranted,
            onEnable = { AccessibilityStatus().open(context) },
        )

        Banner {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = Nemo.colors.inkSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.x2))
                Text(
                    text = stringResource(R.string.onboarding_privacy_card),
                    fontFamily = ManropeFamily,
                    fontSize = 13.sp,
                    color = Nemo.colors.inkSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(label = stringResource(R.string.onboarding_continue), onClick = onContinue)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.onboarding_skip_for_now),
                fontFamily = ManropeFamily,
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
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Nemo.colors.mute,
        )
        ProgressTrack(progress = 2f / 3f)

        Text(
            text = stringResource(R.string.onboarding_profile_headline),
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Nemo.colors.ink,
        )
        Text(
            text = stringResource(R.string.onboarding_profile_sub),
            fontFamily = ManropeFamily,
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

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            label = stringResource(R.string.onboarding_profile_continue),
            onClick = onContinue,
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.onboarding_profile_skip),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }
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
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                color = Nemo.colors.mute,
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = ManropeFamily,
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
) {
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(text = title, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nemo.colors.ink)
            Text(text = subtitle, fontFamily = ManropeFamily, fontSize = 12.sp, color = Nemo.colors.mute)
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
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Nemo.colors.onInk,
                )
            }
        }
    }
}
