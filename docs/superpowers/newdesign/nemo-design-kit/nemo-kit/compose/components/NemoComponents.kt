package com.anchor.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.ui.theme.Nemo
import com.anchor.app.ui.theme.NemoMotion
import com.anchor.app.ui.theme.SheetShape

// ───────── Buttons ─────────
enum class NemoButtonStyle { Primary, Secondary, Outline, Danger, Text }

@Composable
fun NemoButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    style: NemoButtonStyle = NemoButtonStyle.Primary, enabled: Boolean = true, small: Boolean = false,
) {
    val c = Nemo.colors
    val container: Color; val content: Color; var border: Color? = null
    when (style) {
        NemoButtonStyle.Primary -> { container = c.ink; content = c.onInk }
        NemoButtonStyle.Secondary -> { container = c.fog; content = c.ink }
        NemoButtonStyle.Outline -> { container = Color.Transparent; content = c.ink; border = c.line }
        NemoButtonStyle.Danger -> { container = Color.Transparent; content = c.danger; border = c.dangerLine }
        NemoButtonStyle.Text -> { container = Color.Transparent; content = c.mute }
    }
    Button(
        onClick = onClick, enabled = enabled, shape = CircleShape,
        modifier = modifier.height(if (small) 34.dp else 54.dp),
        border = border?.let { BorderStroke(1.dp, it) },
        contentPadding = PaddingValues(horizontal = if (small) 16.dp else 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.35f), disabledContentColor = content.copy(alpha = 0.6f),
        ),
        elevation = null,
    ) { Text(text, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge) }
}

// ───────── Chip, Pill ─────────
@Composable
fun NemoChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Nemo.colors
    Surface(
        onClick = onClick, modifier = modifier, shape = CircleShape,
        color = if (selected) c.ink else c.background,
        contentColor = if (selected) c.onInk else c.inkSecondary,
        border = if (selected) null else BorderStroke(1.dp, c.line),
    ) { Text(label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium) }
}

/** LOW / MEDIUM / HIGH / URGENT, IN PROGRESS, On, Enabled. */
@Composable
fun NemoPill(text: String, modifier: Modifier = Modifier, filled: Boolean = false) {
    val c = Nemo.colors
    Surface(modifier = modifier, shape = CircleShape, color = if (filled) c.ink else c.fog,
        contentColor = if (filled) c.onInk else c.inkSecondary) {
        Text(text.uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 0.8.sp))
    }
}

// ───────── Toggle ─────────
@Composable
fun NemoToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Nemo.colors
    Switch(checked, onCheckedChange, modifier, colors = SwitchDefaults.colors(
        checkedThumbColor = c.onInk, checkedTrackColor = c.ink, checkedBorderColor = c.ink,
        uncheckedThumbColor = c.background, uncheckedTrackColor = c.line, uncheckedBorderColor = Color.Transparent))
}

// ───────── Progress ─────────
@Composable
fun NemoProgressTrack(progress: Float, modifier: Modifier = Modifier) {
    val c = Nemo.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(NemoMotion.Progress, easing = NemoMotion.Easing), label = "track")
    Box(modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(c.fog)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(c.ink))
    }
}

@Composable
fun NemoTimerRing(progress: Float, time: String, caption: String, modifier: Modifier = Modifier,
                  diameter: Dp = 220.dp, stroke: Dp = 6.dp) {
    val c = Nemo.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(NemoMotion.Progress, easing = NemoMotion.Easing), label = "ring")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val tl = Offset(s / 2, s / 2); val sz = Size(this.size.width - s, this.size.height - s)
            drawArc(color = c.fog, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = sz, style = Stroke(s))
            drawArc(color = c.ink, startAngle = -90f, sweepAngle = 360f * p, useCenter = false, topLeft = tl, size = sz,
                style = Stroke(s, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, style = MaterialTheme.typography.displayLarge, color = c.ink)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = c.mute)
        }
    }
}

// ───────── Input ─────────
@Composable
fun NemoTextField(value: String, onValueChange: (String) -> Unit, placeholder: String,
                  modifier: Modifier = Modifier, singleLine: Boolean = true) {
    val c = Nemo.colors
    TextField(
        value, onValueChange, modifier.fillMaxWidth().heightIn(min = 54.dp), singleLine = singleLine,
        placeholder = { Text(placeholder, color = c.placeholder) }, shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.fog, unfocusedContainerColor = c.fog, disabledContainerColor = c.fog,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent, cursorColor = c.ink,
            focusedTextColor = c.ink, unfocusedTextColor = c.ink),
    )
}

// ───────── Overline label & list rows ─────────
@Composable
fun NemoSectionLabel(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier.padding(top = 28.dp, bottom = 12.dp),
        style = MaterialTheme.typography.labelSmall, color = Nemo.colors.mute)

@Composable
fun NemoListRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null,
                onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val c = Nemo.colors
    Column(modifier) {
        Row(Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.mute)
            }
            trailing?.invoke()
        }
        HorizontalDivider(color = c.line)
    }
}

// ───────── Task ─────────
@Composable
fun NemoNode(done: Boolean, modifier: Modifier = Modifier) {
    val c = Nemo.colors
    Box(modifier.size(20.dp).clip(CircleShape)
        .then(if (done) Modifier.background(c.ink) else Modifier.border(1.5.dp, c.mute.copy(alpha = 0.5f), CircleShape)),
        contentAlignment = Alignment.Center) {
        if (done) Icon(Icons.Filled.Check, null, Modifier.size(12.dp), tint = c.onInk)
    }
}

@Composable
fun NemoTaskRow(title: String, meta: String, priority: String, done: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Nemo.colors
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
        NemoNode(done, Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (done) c.mute else c.ink,
                textDecoration = if (done) TextDecoration.LineThrough else null)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = c.mute)
        }
        NemoPill(priority)
    }
}

// ───────── Navigation, FAB, Sheet ─────────
data class NemoTab(val label: String, val icon: ImageVector)

@Composable
fun NemoBottomBar(tabs: List<NemoTab>, selected: Int, onSelect: (Int) -> Unit) {
    val c = Nemo.colors
    NavigationBar(containerColor = c.background, tonalElevation = 0.dp) {
        tabs.forEachIndexed { i, t ->
            NavigationBarItem(
                selected = i == selected, onClick = { onSelect(i) },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.ink, selectedTextColor = c.ink, indicatorColor = Color.Transparent,
                    unselectedIconColor = c.mute, unselectedTextColor = c.mute),
            )
        }
    }
}

@Composable
fun NemoFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Nemo.colors
    FloatingActionButton(onClick, modifier, shape = CircleShape, containerColor = c.ink, contentColor = c.onInk,
        elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 2.dp)) {
        Icon(Icons.Filled.Add, contentDescription = "Add task")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NemoBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = Nemo.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = c.surface, contentColor = c.ink, shape = SheetShape,
        scrimColor = c.scrim.copy(alpha = c.scrimAlpha), tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.line) },
    ) { Column(Modifier.padding(horizontal = Nemo.spacing.xl).padding(bottom = Nemo.spacing.xl), content = content) }
}
