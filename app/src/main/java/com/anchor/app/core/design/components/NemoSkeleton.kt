package com.anchor.app.core.design.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Motion
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import com.anchor.app.core.design.theme.PillShape
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing

/**
 * Shimmer effect modifier with infinite pulsing alpha (0.35f to 0.70f),
 * Motion.standardEase easing, and Motion.progress (600ms) duration using Nemo.colors.fog.
 */
fun Modifier.nemoShimmer(shape: Shape? = null): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "nemo_shimmer_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = Motion.progress,
                easing = Motion.standardEase,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nemo_shimmer_alpha",
    )
    val color = Nemo.colors.fog.copy(alpha = alpha)
    if (shape != null) {
        this.background(color = color, shape = shape)
    } else {
        this.background(color = color)
    }
}

/**
 * Skeleton placeholder replicating the footprint of [NemoTaskRow]
 * (min height 72dp, rounded corner Radius.card, 20dp node circle, title and meta line placeholders).
 */
@Composable
fun NemoSkeletonRow(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.card)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(shape)
            .background(Nemo.colors.surface)
            .padding(
                start = Spacing.cardHorizontal,
                end = Spacing.cardHorizontal,
                top = Spacing.x4,
                bottom = Spacing.x4,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .nemoShimmer(),
        )
        Spacer(modifier = Modifier.width(Spacing.x3))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(PillShape)
                    .nemoShimmer(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
                    .clip(PillShape)
                    .nemoShimmer(),
            )
        }
    }
}

/**
 * Rounded rectangle skeleton card for Wellbeing and Insights sections.
 */
@Composable
fun NemoSkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
) {
    val shape = RoundedCornerShape(Radius.card)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Nemo.colors.surface)
            .padding(Spacing.x4),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.x3),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(16.dp)
                    .clip(PillShape)
                    .nemoShimmer(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(12.dp)
                    .clip(PillShape)
                    .nemoShimmer(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(PillShape)
                    .nemoShimmer(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NemoSkeletonPreview() {
    NemoTheme {
        Box(
            modifier = Modifier
                .background(Nemo.colors.background)
                .padding(Spacing.x4),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                NemoSkeletonRow()
                NemoSkeletonCard()
            }
        }
    }
}
