package com.anchor.app.feature.settings

import android.Manifest
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.components.TextPill
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.platform.usagestats.UsageAccessSettings

/**
 * 09 Settings & Privacy (docs/superpowers/design/07-screens.md §2.13).
 *
 * Deliberately short: toggles (FR-S6), live permission status with shortcuts (FR-S1), and
 * compact summaries that link to the dedicated sub-screens for the long lists — app
 * categories (FR-S3) and blocked apps (FR-S4) — plus data controls (FR-S5).
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
    val context = LocalContext.current

    // Re-check OS permission state whenever we return to this screen (FR-S1, docs 08 §2 flow).
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
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6, bottom = Spacing.x8),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detail_back),
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AnchorColors.textPrimary,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AnchorColors.textPrimary,
        )

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

        // --- Privacy & AI (FR-S2, FR-S6) ---
        SectionHeader(title = stringResource(R.string.settings_section_privacy))
        SettingRow(
            title = stringResource(R.string.settings_ai_title),
            subtitle = stringResource(R.string.settings_ai_subtitle),
            checked = settings.aiEnabled,
            onCheckedChange = viewModel::setAiEnabled,
        )
        SettingRow(
            title = stringResource(R.string.settings_tracking_title),
            subtitle = stringResource(R.string.settings_tracking_subtitle),
            checked = settings.trackingEnabled,
            onCheckedChange = viewModel::setTrackingEnabled,
        )

        Divider()

        // --- App categories summary (FR-S3) → sub-screen ---
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

        // --- Blocked apps summary (FR-S4) → sub-screen ---
        SectionHeader(title = stringResource(R.string.settings_section_blocked))
        ManageRow(
            summary = if (settings.blockedApps.isEmpty()) {
                stringResource(R.string.settings_blocked_summary_none)
            } else {
                stringResource(R.string.settings_blocked_summary, settings.blockedApps.size)
            },
            onManage = onManageBlocked,
        )

        // --- Focus blocking opt-in (FR-F4/F8, PRD v0.4 §10.5) ---
        Divider()
        SectionHeader(title = stringResource(R.string.settings_section_focus_blocking))
        SettingRow(
            title = stringResource(R.string.settings_focus_blocking_title),
            subtitle = stringResource(R.string.settings_focus_blocking_subtitle),
            checked = settings.focusBlockingEnabled,
            onCheckedChange = viewModel::setFocusBlockingEnabled,
        )

        Divider()

        // --- Data controls (FR-S5) ---
        SectionHeader(title = stringResource(R.string.settings_section_data))
        Text(
            text = stringResource(R.string.settings_retention_label, settings.usageRetentionDays),
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.chip))
                .border(1.dp, AnchorColors.accentDanger, RoundedCornerShape(Radius.chip))
                .clickable { viewModel.onDeleteAllData() }
                .padding(horizontal = Spacing.x4, vertical = Spacing.x3),
        ) {
            Text(
                text = stringResource(R.string.settings_delete_all),
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = AnchorColors.accentDanger,
            )
        }

        Divider()
        Text(
            text = stringResource(R.string.settings_assurance),
            fontFamily = InterFamily,
            fontSize = 12.sp,
            color = AnchorColors.textMuted,
        )
    }
}

/** Requests POST_NOTIFICATIONS on 33+; on older versions it is granted by default. */
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(text = title, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AnchorColors.textPrimary)
            Text(text = description, fontFamily = InterFamily, fontSize = 12.sp, color = AnchorColors.textMuted)
        }
        Spacer(modifier = Modifier.width(Spacing.x3))
        if (granted) {
            TextPill(text = stringResource(R.string.settings_perm_granted), highlighted = true)
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(AnchorColors.ink)
                    .clickable(onClick = onEnable)
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.settings_perm_enable),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AnchorColors.surface,
                )
            }
        }
    }
}

/** Compact row: a one-line summary plus a [Manage] affordance that opens a sub-screen. */
@Composable
private fun ManageRow(summary: String, onManage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.field))
            .clickable(onClick = onManage)
            .padding(vertical = Spacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = AnchorColors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(Spacing.x3))
        Text(
            text = stringResource(R.string.settings_manage),
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = AnchorColors.ink,
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
            Text(text = title, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AnchorColors.textPrimary)
            Text(text = subtitle, fontFamily = InterFamily, fontSize = 12.sp, color = AnchorColors.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AnchorColors.surface,
                checkedTrackColor = AnchorColors.ink,
            ),
        )
    }
}
