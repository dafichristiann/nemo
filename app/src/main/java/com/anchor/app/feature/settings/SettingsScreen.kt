package com.anchor.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.Divider
import com.anchor.app.core.design.components.SectionHeader
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * 09 Settings & Privacy (docs/superpowers/design/07-screens.md §2.13), Sprint-1 scope:
 * permission placeholders, PRIVACY &amp; AI toggle (default OFF, FR-S6), DATA CONTROLS with
 * a working "delete all data" (FR-S5), and the local-only assurance line.
 *
 * Permission status wiring (Usage Access, Notifications) arrives with Wellbeing in Sprint 2.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
