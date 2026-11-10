package com.anchor.app.core.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.R
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Spacing

/**
 * Standard Screen Header across all Nemo pages (NEMO Design System).
 *
 * Enforces uniform typography, spacing, and hierarchy:
 * - Eyebrow (optional): Manrope, SemiBold, 11sp, uppercase, letterSpacing 1.sp, color mute
 * - Title: Fraunces, Normal (400), 28sp, lineHeight 34sp, letterSpacing -0.3sp, color ink
 * - Subtitle (optional): Manrope, Normal, 13sp, lineHeight 18sp, color mute
 * - Action & Back slots for complete alignment across every screen.
 */
@Composable
fun NemoScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(PillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    )
                    .padding(vertical = Spacing.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = backContentDescription ?: stringResource(R.string.detail_back),
                    tint = Nemo.colors.ink,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = backContentDescription ?: stringResource(R.string.detail_back),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Nemo.colors.ink,
                )
            }
        }

        if (eyebrow != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    fontFamily = GeistFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Nemo.colors.mute,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.3).sp,
                color = Nemo.colors.ink,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (action != null) {
                action()
            }
        }

        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = GeistFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Nemo.colors.mute,
            )
        }
    }
}

/**
 * Standard Sheet / Dialog Header across all Nemo modal overlays.
 *
 * Headline Medium: Geist, Normal (400), 22sp, lineHeight 28sp.
 */
@Composable
fun NemoSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isClosingEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = Nemo.colors.ink,
        )
        Box(
            modifier = Modifier
                .minTouchTarget()
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = isClosingEnabled,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cd_close_sheet),
                tint = Nemo.colors.mute,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
