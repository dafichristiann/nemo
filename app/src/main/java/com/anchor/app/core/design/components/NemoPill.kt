package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Spacing

/**
 * Neutral badge / tag in the NEMO style. Fully-rounded pill, height ~22, font 11.
 *
 * Neutral = `fog`, highlighted (selected/emphasis) = `ink` with `onInk` text.
 */
@Composable
fun NemoPill(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(PillShape)
            .background(
                if (highlighted) Nemo.colors.ink else Nemo.colors.fog,
            )
            .padding(horizontal = Spacing.x2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = if (highlighted) Nemo.colors.onInk else Nemo.colors.mute,
        )
    }
}
