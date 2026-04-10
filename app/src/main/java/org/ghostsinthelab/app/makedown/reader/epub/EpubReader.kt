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

package org.ghostsinthelab.app.makedown.reader.epub

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontFamily
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontScale
import org.ghostsinthelab.app.makedown.ui.scaledBy

@Composable
fun EpubReader(
    book: EpubBook,
    modifier: Modifier = Modifier,
) {
    if (book.chapters.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This EPUB has no readable chapters.")
        }
        return
    }

    var chapterIndex by rememberSaveable(book.title) { mutableIntStateOf(0) }
    val chapter = book.chapters[chapterIndex.coerceIn(0, book.chapters.size - 1)]
    val listState = rememberLazyListState()

    // Reset scroll when chapter changes.
    LaunchedEffect(chapterIndex) {
        listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Chapter header bar.
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                IconButton(
                    onClick = { if (chapterIndex > 0) chapterIndex-- },
                    enabled = chapterIndex > 0,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous chapter")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        text = "Chapter ${chapterIndex + 1} of ${book.chapters.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { if (chapterIndex < book.chapters.size - 1) chapterIndex++ },
                    enabled = chapterIndex < book.chapters.size - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next chapter")
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            blockItems(chapter.blocks, book.resources)
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(
                        onClick = { if (chapterIndex > 0) chapterIndex-- },
                        enabled = chapterIndex > 0,
                    ) { Text("Previous") }
                    Button(
                        onClick = { if (chapterIndex < book.chapters.size - 1) chapterIndex++ },
                        enabled = chapterIndex < book.chapters.size - 1,
                    ) { Text("Next") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun LazyListScope.blockItems(
    blocks: List<EpubBlock>,
    resources: Map<String, ByteArray>,
) {
    items(blocks.size) { index -> EpubBlockRenderer(blocks[index], resources) }
}

@Composable
internal fun EpubBlockRenderer(block: EpubBlock, resources: Map<String, ByteArray>) {
    val readerFont = LocalReaderFontFamily.current
    val scale = LocalReaderFontScale.current
    when (block) {
        is EpubBlock.Heading -> {
            val base = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            Text(
                text = block.text,
                style = base.scaledBy(scale),
                fontFamily = readerFont,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
            )
        }
        is EpubBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge.scaledBy(scale),
                fontFamily = readerFont,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }
        is EpubBlock.BulletList -> {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (item in block.items) {
                    ListItemRow(marker = "•", children = item, resources = resources)
                }
            }
        }
        is EpubBlock.OrderedList -> {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for ((idx, item) in block.items.withIndex()) {
                    ListItemRow(marker = "${block.start + idx}.", children = item, resources = resources)
                }
            }
        }
        is EpubBlock.BlockQuote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (child in block.children) EpubBlockRenderer(child, resources)
                }
            }
        }
        is EpubBlock.CodeBlock -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = block.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp.scaledBy(scale),
                    lineHeight = 18.sp.scaledBy(scale),
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
        }
        EpubBlock.HorizontalRule -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        is EpubBlock.Image -> {
            val bitmap = remember(block.resourcePath) {
                resources[block.resourcePath]?.let { bytes ->
                    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = block.alt,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            } else if (!block.alt.isNullOrBlank()) {
                Text(
                    text = block.alt,
                    style = MaterialTheme.typography.bodySmall
                        .copy(fontStyle = FontStyle.Italic)
                        .scaledBy(scale),
                    fontFamily = readerFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ListItemRow(marker: String, children: List<EpubBlock>, resources: Map<String, ByteArray>) {
    val readerFont = LocalReaderFontFamily.current
    val scale = LocalReaderFontScale.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = "$marker ",
            style = MaterialTheme.typography.bodyLarge.scaledBy(scale),
            fontFamily = readerFont,
            modifier = Modifier.width((28f * scale).dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            for (child in children) EpubBlockRenderer(child, resources)
        }
    }
}
