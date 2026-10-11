package com.anchor.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anchor.app.ui.theme.Nemo
import com.anchor.app.ui.theme.NemoTheme

/** Contoh layar Tasks ringkas. Dipakai untuk mengecek tema setelah dipasang. */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TasksPreview() = NemoTheme {
    Column(Modifier.fillMaxSize().padding(horizontal = Nemo.spacing.screenH, vertical = 32.dp)) {
        Text("Good morning,\nRina", style = MaterialTheme.typography.headlineLarge, color = Nemo.colors.ink)
        Spacer(Modifier.height(24.dp))
        NemoProgressTrack(0.33f)
        NemoTaskRow("Finish thesis outline", "2h · Due 23:59", "High", done = false, onClick = {})
        NemoTaskRow("Reply to emails", "15m", "Low", done = true, onClick = {})
        Spacer(Modifier.height(24.dp))
        NemoButton("Start Focus to Complete", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}
