package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.ManropeFamily
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Task item row in the NEMO style: a flat card (no shadow), background `surface`, radius 24.
 *
 * Priority is conveyed by a thin left bar (soft tone) — never a red background. An
 * optional footnote row renders meta / sub-progress; a subtitle renders secondary text.
 * All textual content is supplied by the caller so copy stays in string resources.
 */
@Composable
fun NemoTaskRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    highlighted: Boolean = false,
    priorityBarColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
) {
    val resolvedBarColor = if (priorityBarColor == Color.Unspecified) Nemo.colors.ink else priorityBarColor
    val shape = RoundedCornerShape(Radius.card)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nemo.colors.surface)
            .then(
                if (highlighted) {
                    Modifier.border(1.dp, Nemo.colors.ink, shape)
                } else {
                    Modifier
                },
            )
            .padding(
                start = Spacing.cardHorizontal,
                end = Spacing.cardHorizontal,
                top = Spacing.x4,
                bottom = Spacing.x4,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        // Priority bar (thin, left edge) — soft neutral tone.
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(Radius.track))
                .background(resolvedBarColor),
        )
        Spacer(modifier = Modifier.width(Spacing.x3))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = title,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Nemo.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Nemo.colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta != null) {
                Text(
                    text = meta,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Nemo.colors.mute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let {
            Spacer(modifier = Modifier.width(Spacing.x2))
            it()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NemoTaskRowPreview() {
    NemoTheme {
        Box(modifier = Modifier.padding(Spacing.x4)) {
            NemoTaskRow(
                title = "Web Assignment",
                subtitle = "Due today 23:59",
                meta = "2h est · 3 subtasks · 1/3",
                trailing = { NemoPill(text = "HIGH", highlighted = true) },
            )
        }
    }
}
