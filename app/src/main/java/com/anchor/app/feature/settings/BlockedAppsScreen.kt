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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.app.R
import com.anchor.app.core.design.components.AppIcon
import com.anchor.app.core.design.components.NemoScreenHeader
import com.anchor.app.core.design.components.NemoSwitch
import com.anchor.app.core.design.components.pressable
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
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
    // Remembered: sorting the whole installed-app list is O(n log n) and would otherwise re-run on
    // every recomposition (including each keystroke in the search field).
    val ordered = remember(apps, blocked) {
        apps.sortedWith(
            compareByDescending<com.anchor.app.platform.apps.InstalledApp> { it.packageName in blocked }
                .thenBy { it.label.lowercase() },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.background)
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.x6),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        BackHeader(
            eyebrow = "DISTRACTION SHIELD",
            title = stringResource(R.string.blocked_title),
            onBack = onBack,
        )

        // Search
        SearchField(
            query = query,
            hint = stringResource(R.string.settings_blocked_search_hint),
            onQueryChange = viewModel::setAppQuery,
        )

        if (ordered.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_blocked_no_apps),
                fontFamily = ManropeFamily,
                fontSize = 13.sp,
                color = Nemo.colors.mute,
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

/** A row: app icon Â· label + package Â· toggle. Reused by both app-management screens. */
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
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
            )
            Text(
                text = packageName,
                fontFamily = ManropeFamily,
                fontSize = 11.sp,
                color = Nemo.colors.mute,
            )
        }
        NemoSwitch(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
internal fun BackHeader(
    title: String,
    onBack: () -> Unit,
    eyebrow: String? = null,
) {
    NemoScreenHeader(
        eyebrow = eyebrow,
        title = title,
        onBack = onBack,
    )
}

@Composable
internal fun SearchField(query: String, hint: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(Radius.field))
            .background(Nemo.colors.fog)
            .padding(horizontal = Spacing.x3),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text(
                text = hint,
                fontFamily = ManropeFamily,
                fontSize = 13.sp,
                color = Nemo.colors.mute,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = ManropeFamily, fontSize = 13.sp, color = Nemo.colors.ink),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
