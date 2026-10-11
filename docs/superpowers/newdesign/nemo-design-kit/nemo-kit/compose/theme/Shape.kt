package com.anchor.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val NemoShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),   // input
    medium = RoundedCornerShape(24.dp),  // kartu
    large = RoundedCornerShape(30.dp),   // dialog
    extraLarge = RoundedCornerShape(32.dp),
)
val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
