package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.navigation.TopLevelDestination
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Spacing

/**
 * 4-tab bottom navigation bar (docs/superpowers/design/06-design-system.md §7.10).
 *
 * Height 56, white background with a thin top divider. Active item `#0A0A0A`,
 * inactive `#888`. Items come from [TopLevelDestination]; labels are resolved from
 * string resources (English default, PRD §12).
 */
@Composable
fun AnchorNavBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(AnchorColors.surface)) {
        Divider(thickness = 1.dp, color = AnchorColors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = Spacing.x2),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = destination.route == currentRoute
                val tint = if (selected) AnchorColors.ink else AnchorColors.textMutedAlt
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(destination) }
                        .padding(vertical = Spacing.x1),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.x1),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(destination.labelRes),
                        fontFamily = InterFamily,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = tint,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnchorNavBarPreview() {
    AnchorTheme {
        AnchorNavBar(
            destinations = TopLevelDestination.entries,
            currentRoute = TopLevelDestination.TASKS.route,
            onNavigate = {},
        )
    }
}
