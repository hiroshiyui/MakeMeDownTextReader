package org.ghostsinthelab.app.makedown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.ghostsinthelab.app.makedown.data.MAX_READER_BASE_PT
import org.ghostsinthelab.app.makedown.data.MIN_READER_BASE_PT
import org.ghostsinthelab.app.makedown.data.READER_BASE_PT_STEP
import org.ghostsinthelab.app.makedown.data.ReaderFont

/**
 * Bottom app bar shown while reading. Provides:
 *
 * 1. A back-to-main button.
 * 2. Text zoom (adjusts [baseFontSizePt] downward/upward), with the current
 *    point value displayed between the buttons.
 * 3. A font-family switcher that opens a dropdown menu.
 *
 * This composable is intentionally stateless — the parent owns the current
 * [baseFontSizePt] / [font] and persists callbacks into [SettingsRepository].
 */
@Composable
fun ReaderBottomBar(
    baseFontSizePt: Float,
    font: ReaderFont,
    onBack: () -> Unit,
    onBaseFontSizeChange: (Float) -> Unit,
    onFontChange: (ReaderFont) -> Unit,
) {
    var fontMenuOpen by remember { mutableStateOf(false) }

    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to main",
            )
        }

        Spacer(Modifier.weight(1f))

        // Zoom controls.
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    onBaseFontSizeChange(stepBaseFontSize(baseFontSizePt, -READER_BASE_PT_STEP))
                },
                enabled = baseFontSizePt > MIN_READER_BASE_PT,
            ) {
                Text("A−", fontSize = 14.sp)
            }
            Text(
                text = "${baseFontSizePt.roundToInt()} pt",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(44.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            TextButton(
                onClick = {
                    onBaseFontSizeChange(stepBaseFontSize(baseFontSizePt, READER_BASE_PT_STEP))
                },
                enabled = baseFontSizePt < MAX_READER_BASE_PT,
            ) {
                Text("A+", fontSize = 18.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        // Font family switcher.
        Box {
            TextButton(onClick = { fontMenuOpen = true }) {
                Text(
                    text = "Aa",
                    fontFamily = font.toFontFamily(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            DropdownMenu(
                expanded = fontMenuOpen,
                onDismissRequest = { fontMenuOpen = false },
            ) {
                ReaderFont.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.displayName,
                                fontFamily = option.toFontFamily(),
                            )
                        },
                        onClick = {
                            fontMenuOpen = false
                            onFontChange(option)
                        },
                        trailingIcon = if (option == font) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
    }
}

/**
 * Apply [delta] to [current] and clamp to the supported range, rounded to
 * the nearest whole point so repeated stepping doesn't accumulate floating
 * point drift.
 */
private fun stepBaseFontSize(current: Float, delta: Float): Float {
    val raw = current + delta
    val quantized = raw.roundToInt().toFloat()
    return quantized.coerceIn(MIN_READER_BASE_PT, MAX_READER_BASE_PT)
}
