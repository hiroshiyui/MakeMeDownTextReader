/*
 * MakeMeDown Text Reader
 * Copyright (C) 2026 Hui-Hong You
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ghostsinthelab.app.makedown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.ghostsinthelab.app.makedown.data.MAX_READER_BASE_PT
import org.ghostsinthelab.app.makedown.data.MIN_READER_BASE_PT
import org.ghostsinthelab.app.makedown.data.READER_BASE_PT_STEP
import org.ghostsinthelab.app.makedown.data.ReaderFont
import org.ghostsinthelab.app.makedown.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = SettingsRepository.get(context)
    val settings by repo.state.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            SectionHeader("Base font size")
            Text(
                text = "Controls the body text size for all reading surfaces, in " +
                    "points. Headings and other roles scale proportionally. You can " +
                    "also zoom in and out from the reader's bottom bar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            BaseFontSizeStepper(
                currentPt = settings.baseFontSizePt,
                onChange = { newSize ->
                    scope.launch {
                        repo.update { it.copy(baseFontSizePt = newSize) }
                    }
                },
            )
            Spacer(Modifier.padding(vertical = 8.dp))

            SectionHeader("Reading font")
            Text(
                text = "Applies to plain text, Markdown, and EPUB reading surfaces. " +
                    "Code blocks and inline code always use a monospace font.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Spacer(Modifier.padding(vertical = 4.dp))
            ReaderFont.entries.forEach { font ->
                FontOptionRow(
                    font = font,
                    selected = settings.readerFont == font,
                    onSelected = {
                        scope.launch {
                            repo.update { it.copy(readerFont = font) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BaseFontSizeStepper(
    currentPt: Float,
    onChange: (Float) -> Unit,
) {
    // Live preview renders a sample string at the currently-selected size so
    // the user can judge the change without leaving the settings screen.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            TextButton(
                onClick = { onChange(stepBaseFontSize(currentPt, -READER_BASE_PT_STEP)) },
                enabled = currentPt > MIN_READER_BASE_PT,
            ) { Text("A−", fontSize = 14.sp) }
            Text(
                text = "${currentPt.roundToInt()} pt",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(72.dp),
            )
            TextButton(
                onClick = { onChange(stepBaseFontSize(currentPt, READER_BASE_PT_STEP)) },
                enabled = currentPt < MAX_READER_BASE_PT,
            ) { Text("A+", fontSize = 18.sp) }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                text = "Sample — the quick brown fox jumps over the lazy dog.",
                fontSize = ((currentPt * 4f) / 3f).sp,
                lineHeight = ((currentPt * 4f) / 3f * 1.4f).sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Apply [delta] to [current] and clamp to supported range, rounded to 1 pt. */
private fun stepBaseFontSize(current: Float, delta: Float): Float {
    val raw = current + delta
    val quantized = raw.roundToInt().toFloat()
    return quantized.coerceIn(MIN_READER_BASE_PT, MAX_READER_BASE_PT)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FontOptionRow(
    font: ReaderFont,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // selectable handles the click for the whole row
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = font.displayName,
                style = MaterialTheme.typography.bodyLarge,
            )
            // Live preview rendered in the chosen family so the user can
            // see what they're picking before committing.
            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = font.toFontFamily(),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
