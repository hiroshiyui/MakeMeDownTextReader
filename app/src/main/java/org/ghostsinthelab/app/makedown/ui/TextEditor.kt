package org.ghostsinthelab.app.makedown.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple full-screen plain-text editor.
 *
 * Uses [BasicTextField] inside a vertical scroll container so the text field
 * grows as content grows and scrolls when it exceeds the viewport. Soft-wrap
 * is on by default, so long lines wrap to the display width — matching the
 * reader's wrapping behavior.
 */
@Composable
fun TextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val baseStyle = LocalTextStyle.current
    val style = baseStyle.merge(
        TextStyle(
            color = colors.onSurface,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = if (monospace) FontFamily.Monospace else baseStyle.fontFamily,
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = style,
            cursorBrush = SolidColor(colors.primary),
        )
    }
}
