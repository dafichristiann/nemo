package com.anchor.app.feature.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.AnchorSwitch
import com.anchor.app.core.design.components.AppIcon
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Sub-screen: manage blocked apps (FR-S4). Keeps the main Settings screen short by moving the
 * long installed-app list here. Search + icon + toggle per app; blocked apps float to the top.
 */
@Composable
fun BlockedAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val query by viewModel.appQuery.collectAsStateWithLifecycle()
    val blocked = settings.blockedApps

    // Blocked apps first (so they are easy to find/remove), then the rest alphabetically.
    val ordered = apps.sortedWith(compareByDescending<com.anchor.app.platform.apps.InstalledApp> { it.packageName in blocked }.thenBy { it.label.lowercase() })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        BackHeader(title = stringResource(R.string.blocked_title), onBack = onBack)

        // Search
        SearchField(
            query = query,
            hint = stringResource(R.string.settings_blocked_search_hint),
            onQueryChange = viewModel::setAppQuery,
        )

        if (ordered.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_blocked_no_apps),
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = AnchorColors.textFaint,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.x8),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                items(ordered, key = { it.packageName }) { app ->
                    val isBlocked = app.packageName in blocked
                    AppToggleRow(
                        icon = app.icon,
                        label = app.label,
                        packageName = app.packageName,
                        checked = isBlocked,
                        onToggle = {
                            if (isBlocked) viewModel.onRemoveBlockedApp(app.packageName)
                            else viewModel.onAddBlockedApp(app.packageName)
                        },
                    )
                }
            }
        }
    }
}

/** A row: app icon · label + package · toggle. Reused by both app-management screens. */
@Composable
internal fun AppToggleRow(
    icon: Drawable?,
    label: String,
    packageName: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.field))
            .pressable(shape = RoundedCornerShape(Radius.field), onClick = onToggle)
            .padding(horizontal = Spacing.x2, vertical = Spacing.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon = icon, fallbackLabel = label)
        Spacer(modifier = Modifier.width(Spacing.x3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AnchorColors.textPrimary,
            )
            Text(
                text = packageName,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                color = AnchorColors.textFaint,
            )
        }
        AnchorSwitch(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
internal fun BackHeader(title: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.chip))
                .pressable(
                    shape = RoundedCornerShape(Radius.chip),
                    pressedScale = 0.96f,
                    onClick = onBack,
                )
                .padding(vertical = Spacing.x1, horizontal = Spacing.x1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.detail_back),
                tint = AnchorColors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.detail_back),
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AnchorColors.textPrimary,
            )
        }
        Text(
            text = title,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = AnchorColors.textPrimary,
        )
    }
}

@Composable
internal fun SearchField(query: String, hint: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.field))
            .background(AnchorColors.surfaceSunken)
            .padding(horizontal = Spacing.x3),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text(
                text = hint,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = AnchorColors.textFaint,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = InterFamily, fontSize = 13.sp, color = AnchorColors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
