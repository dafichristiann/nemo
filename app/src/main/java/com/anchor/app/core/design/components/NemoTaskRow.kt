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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.R
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Task item row in the NEMO style: a flat card (no shadow), background `surface`, radius 24.
 *
 * The leading node (design-spec.md §6) is a 20dp circle: empty when not done, filled `ink`
 * with a check when done. A done row fades its title to `mute` and strikes it through.
 * Metadata (subtitle / meta) is secondary text; all copy is supplied by the caller.
 */
@Composable
fun NemoTaskRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    done: Boolean = false,
    highlighted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
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
        NemoNode(done = done, modifier = Modifier.padding(top = 1.dp))
        Spacer(modifier = Modifier.width(Spacing.x3))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (done) Nemo.colors.mute else Nemo.colors.ink,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Nemo.colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
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

/**
 * The task node (design-spec.md §6): a 20dp circle. When [done] it is filled with `ink` and
 * shows a check; otherwise it is a 1.5dp `mute` ring. Readable as "selesai/belum selesai".
 */
@Composable
fun NemoNode(
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentDescription = if (done) "Completed" else "Not completed"
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .then(
                if (done) {
                    Modifier.background(Nemo.colors.ink)
                } else {
                    Modifier.border(1.5.dp, Nemo.colors.mute.copy(alpha = 0.5f), CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(
                painter = painterResource(R.drawable.ic_tasks),
                contentDescription = contentDescription,
                modifier = Modifier.size(12.dp),
                tint = Nemo.colors.onInk,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NemoTaskRowPreview() {
    NemoTheme {
        Box(modifier = Modifier.padding(Spacing.x4)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                NemoTaskRow(
                    title = "Web Assignment",
                    subtitle = "Due today 23:59",
                    meta = "2h est · 3 subtasks · 1/3",
                    trailing = { NemoPill(text = "HIGH", filled = true) },
                )
                NemoTaskRow(
                    title = "Reply to emails",
                    subtitle = "15m",
                    done = true,
                    trailing = { NemoPill(text = "LOW") },
                )
            }
        }
    }
}
