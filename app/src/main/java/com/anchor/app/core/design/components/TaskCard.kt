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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.AnchorElevation
import com.anchor.app.core.design.theme.AnchorTheme
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.LocalAnchorElevation
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Task item card with the soft-3D "Base/Front" treatment
 * (docs/superpowers/design/06-design-system.md §7.2, §7.3).
 *
 * Priority is conveyed by a thin left bar (soft tone) — never a red background. An
 * optional footnote row renders meta / sub-progress; a subtitle renders secondary text.
 * All textual content is supplied by the caller so copy stays in string resources.
 */
@Composable
fun TaskCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    highlighted: Boolean = false,
    priorityBarColor: Color = AnchorColors.ink,
    trailing: (@Composable () -> Unit)? = null,
) {
    val elevation: AnchorElevation = LocalAnchorElevation.current
    val shape = RoundedCornerShape(Radius.card)

    // Soft-3D via a single soft shadow (06-design-system.md §6: max one shadow/element).
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation.card.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(shape)
            .background(AnchorColors.surfaceCard)
            .then(
                if (highlighted) {
                    Modifier.border(1.dp, AnchorColors.ink, shape)
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
                .background(priorityBarColor),
        )
        Spacer(modifier = Modifier.width(Spacing.x3))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = title,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AnchorColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = AnchorColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta != null) {
                Text(
                    text = meta,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = AnchorColors.textMuted,
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
private fun TaskCardPreview() {
    AnchorTheme {
        Box(modifier = Modifier.padding(Spacing.x4)) {
            TaskCard(
                title = "Web Assignment",
                subtitle = "Due today 23:59",
                meta = "2h est · 3 subtasks · 1/3",
                trailing = { TextPill(text = "HIGH", highlighted = true) },
            )
        }
    }
}
