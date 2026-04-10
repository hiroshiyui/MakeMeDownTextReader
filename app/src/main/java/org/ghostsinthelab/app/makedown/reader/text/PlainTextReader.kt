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

package org.ghostsinthelab.app.makedown.reader.text

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontFamily
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontScale
import org.ghostsinthelab.app.makedown.ui.scaledBy

@Composable
fun PlainTextReader(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    // Pre-split into stable line items so LazyColumn can efficiently recycle.
    // Each item carries an index so identical lines remain distinct keys.
    val lines = remember(text) {
        text.split('\n').mapIndexed { index, line -> LineItem(index, line) }
    }
    val fontFamily = LocalReaderFontFamily.current
    val scale = LocalReaderFontScale.current
    val fontSize = 14.sp.scaledBy(scale)
    val lineHeight = 20.sp.scaledBy(scale)
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        items(items = lines, key = { it.index }) { item ->
            // softWrap defaults to true; we never put this in a horizontal scroller,
            // so the line wraps to the available display width.
            Text(
                text = if (item.line.isEmpty()) " " else item.line,
                fontFamily = fontFamily,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}

private data class LineItem(val index: Int, val line: String)
