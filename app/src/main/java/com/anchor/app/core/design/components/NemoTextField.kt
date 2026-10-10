package com.anchor.app.core.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anchor.app.core.design.theme.Nemo
import com.anchor.app.core.design.theme.NemoTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * NEMO text input (design-spec.md §6): `fog` background, radius 18, no underline, ink text,
 * `placeholder` hint. Height min 54. Focus and default states share the same flat look.
 */
@Composable
fun NemoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val c = Nemo.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        singleLine = singleLine,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.placeholder) },
        shape = MaterialTheme.shapes.small,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.fog,
            unfocusedContainerColor = c.fog,
            disabledContainerColor = c.fog,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = c.ink,
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun NemoTextFieldPreview() {
    NemoTheme {
        NemoTextField(value = "", onValueChange = {}, placeholder = "What needs doing?")
    }
}
