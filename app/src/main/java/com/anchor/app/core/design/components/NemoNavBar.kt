package com.anchor.app.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.navigation.TopLevelDestination
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Spacing

/**
 * 4-tab bottom navigation bar in the NEMO style.
 *
 * Height 76, `surface` background with a thin top divider. Active item `ink` + SemiBold,
 * inactive `mute`. No pill indicator behind the active item. Items come from
 * [TopLevelDestination]; labels are resolved from string resources (English default, PRD §12).
 */
@Composable
fun NemoNavBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(Nemo.colors.surface)) {
        Divider(thickness = 1.dp, color = Nemo.colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = Spacing.x2),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = destination.route == currentRoute
                val tint by animateColorAsState(
                    targetValue = if (selected) Nemo.colors.ink else Nemo.colors.mute,
                    animationSpec = Motion.standardSpec(),
                    label = "nav-tint",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressable(
                            pressedScale = 0.94f,
                            onClick = { onNavigate(destination) },
                        )
                        .padding(vertical = Spacing.x1),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.x1),
                ) {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = stringResource(destination.labelRes),
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(destination.labelRes),
                        fontFamily = ManropeFamily,
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
private fun NemoNavBarPreview() {
    NemoTheme {
        NemoNavBar(
            destinations = TopLevelDestination.entries,
            currentRoute = TopLevelDestination.TASKS.route,
            onNavigate = {},
        )
    }
}
