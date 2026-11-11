package com.anchor.app.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.R
import com.anchor.app.core.design.theme.Motion
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
    showNode: Boolean = done,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onToggleDone: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Radius.card)
    val titleColor by animateColorAsState(
        targetValue = if (done) Nemo.colors.mute else Nemo.colors.ink,
        animationSpec = Motion.standardSpec(),
        label = "task_title_color",
    )

    val rowModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(Nemo.colors.surface)
        .then(
            if (highlighted) {
                Modifier.border(1.dp, Nemo.colors.ink, shape)
            } else {
                Modifier.border(1.dp, Nemo.colors.line, shape)
            },
        )
        .then(
            if (onClick != null) {
                Modifier.pressable(
                    shape = shape,
                    pressedScale = 0.99f,
                    onClick = onClick,
                )
            } else {
                Modifier
            },
        )
        .padding(
            start = Spacing.cardHorizontal,
            end = Spacing.cardHorizontal,
            top = Spacing.x4,
            bottom = Spacing.x4,
        )

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top,
    ) {
        if (showNode) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(24.dp)
                    .semantics {
                        role = Role.Checkbox
                        this.contentDescription = title
                        stateDescription = if (done) "Selesai" else "Belum selesai"
                    }
                    .then(
                        if (onToggleDone != null) {
                            Modifier.pressable(
                                shape = CircleShape,
                                pressedScale = 0.88f,
                                onClick = onToggleDone,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                NemoNode(done = done)
            }
            Spacer(modifier = Modifier.width(Spacing.x3))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2,
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
            content?.invoke()
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
    val haptic = LocalHapticFeedback.current
    var prevDone by remember { mutableStateOf(done) }

    LaunchedEffect(done) {
        if (done && !prevDone) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevDone = done
    }

    val contentDescription = if (done) "Completed" else "Not completed"
    val backgroundColor by animateColorAsState(
        targetValue = if (done) Nemo.colors.ink else Color.Transparent,
        animationSpec = Motion.standardSpec(),
        label = "nemo_node_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (done) Color.Transparent else Nemo.colors.mute.copy(alpha = 0.5f),
        animationSpec = Motion.standardSpec(),
        label = "nemo_node_border",
    )
    val scale by animateFloatAsState(
        targetValue = if (done) 1f else 0.94f,
        animationSpec = Motion.calmSpringSpec(),
        label = "nemo_node_scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(20.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = done,
            enter = scaleIn(
                initialScale = 0.4f,
                animationSpec = Motion.calmSpringSpec(),
            ) + fadeIn(animationSpec = Motion.standardSpec()),
            exit = scaleOut(animationSpec = Motion.instantSpec()) +
                fadeOut(animationSpec = Motion.instantSpec()),
        ) {
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
