package com.anchor.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anchor.app.R
import com.anchor.app.core.design.theme.GeistFamily
import com.anchor.app.core.design.theme.GeistMonoFamily
import com.anchor.app.core.design.theme.Nemo

private data class MascotItemInfo(
    val expression: MascotExpression,
    val name: String,
    val description: String,
)

private val ALL_MASCOT_ITEMS = listOf(
    MascotItemInfo(MascotExpression.NEUTRAL, "Neutral", "Bernapas pelan, sesekali mengedip."),
    MascotItemInfo(MascotExpression.HAPPY, "Happy", "Melompat kecil squash & stretch."),
    MascotItemInfo(MascotExpression.SAD, "Sad", "Kepala merosot pelan sambil menghela."),
    MascotItemInfo(MascotExpression.ANGRY, "Angry", "Getar kiri-kanan cepat memuncak."),
    MascotItemInfo(MascotExpression.SURPRISED, "Surprised", "Terlonjak, garis kejut meletup."),
    MascotItemInfo(MascotExpression.SLEEPY, "Sleepy", "Mengangguk tidur, zzz melayang."),
    MascotItemInfo(MascotExpression.CONFUSED, "Confused", "Miring pelan, tanda ? bergoyang."),
    MascotItemInfo(MascotExpression.LAUGHING, "Laughing", "Tiga lompatan tawa mendongak."),
    MascotItemInfo(MascotExpression.SHY, "Shy", "Menciut gugup, mengintip kembali."),
    MascotItemInfo(MascotExpression.THINKING, "Thinking", "Miring merenung, 3 titik berurutan."),
    MascotItemInfo(MascotExpression.WINK, "Wink", "Kedipan genit miring melonjak."),
    MascotItemInfo(MascotExpression.TALKING, "Talking", "Buka-tutup ritmis mengangguk."),
)

@Composable
fun NemoMascotShowcaseDialog(
    onDismiss: () -> Unit,
) {
    var selectedItem by remember { mutableStateOf<MascotItemInfo?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Nemo.colors.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "NEMO MASCOT SHOWCASE",
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = Nemo.colors.mute,
                        )
                        Text(
                            text = "12 Dynamic Expressions",
                            fontFamily = GeistFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Nemo.colors.ink,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Nemo.colors.surface)
                            .border(1.dp, Nemo.colors.line, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = Nemo.colors.ink,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail preview banner if an item is selected
                selectedItem?.let { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Nemo.colors.surface)
                            .border(1.dp, Nemo.colors.line, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        NemoMascot(
                            expression = item.expression,
                            size = 80.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontFamily = GeistFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Nemo.colors.ink,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                fontFamily = GeistFamily,
                                fontSize = 12.sp,
                                color = Nemo.colors.inkSecondary,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Grid of 12 expressions
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ALL_MASCOT_ITEMS) { item ->
                        val isSelected = selectedItem?.expression == item.expression
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Nemo.colors.fog else Nemo.colors.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) Nemo.colors.ink else Nemo.colors.line,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    selectedItem = if (isSelected) null else item
                                }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NemoMascot(
                                expression = item.expression,
                                size = 64.dp,
                            )
                            Text(
                                text = item.name,
                                fontFamily = GeistFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Nemo.colors.ink,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
